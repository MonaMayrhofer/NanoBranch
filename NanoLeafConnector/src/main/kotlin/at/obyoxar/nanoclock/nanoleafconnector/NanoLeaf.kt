package at.obyoxar.nanoclock.nanoleafconnector

import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.io.File
import java.net.InetAddress
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

private val logger = KotlinLogging.logger {  }

class NanoLeaf private constructor(ip: InetAddress, port: Short) {

    val api = NanoLeafStreamApi(ip, port)

    lateinit var panels: List<Panel>
        private set
    val size = Rectangle2D.Double()
    val animator = Animator()
    val browser: NanoLeafBrowser by lazy { NanoLeafBrowser(this) }

    suspend fun start(){
        api.loadToken(File("token.txt").takeIf { it.exists() }?.readLines()?.get(0))
        api.startStream()
        panels = api.panelLayout.layout().positionData.map {
            size.add(it.x.toDouble(), it.y.toDouble())
            Panel(it.x, it.y, it.o, it.panelId, nanoLeaf = this@NanoLeaf)
        }
    }

    companion object {
        suspend fun connectTo(connectionString: String): NanoLeaf {
            val parts = connectionString.split(Regex(":(?=\\d+\$)"), 2)
            val addr = InetAddress.getByName(parts[0])
            val port = parts.getOrNull(1)?.toShort() ?: 16021
            return NanoLeaf(addr, port).also { it.start() }
        }
    }


    fun identify(){
        pauseAnimation()
        println("Y to store in list, any other key to skip")
        val store = panels.filter {
            it.applyColor(Color.BLUE)
            println("Panel: ${it.uid}")
            var line = readLine()
            while(line?.toUpperCase() != "Y" && line?.toUpperCase() != "N")
                line = readLine()
            (line.toUpperCase() == "Y").also { keep ->
                if(keep){
                    it.applyColor(Color.GREEN)
                }else{
                    it.applyColor(Color.RED)
                }
                it.applyColorDelayed(Color.BLACK, 1000)
            }
        }
        println("Specify desired index in list")
        val sortStore = store.map {
            println("Panel: ${it.uid}")
            it.applyColor(Color.GREEN)

            var line = readLine()
            while(line?.toIntOrNull() == null) line = readLine()
            it.applyColor(Color.BLUE)
            it.applyColorDelayed(Color.BLACK, 300)
            (line!!.toInt() to it)
        }.sortedBy {
            it.first
        }.map { it.second.uid }.joinToString()
        println(sortStore)
        resumeAnimation()
    }

    private val animationTimer = Timer()
    private var animationTask: RestartableTimerTask? = null
    private var hertz: Double? = null

    fun animation(hertz: Double = 10.0, animation: Animator.() -> Unit){
        animation(object: Animation{
            override fun animate(animator: Animator) {
                animator.animation()
            }
        }, hertz)
    }

    fun animation(animation: Animation, hertz: Double = 10.0){
        logger.info("Changing animation to ${animation::class}")
        assert(hertz < 10)
        this.hertz = hertz

        pauseAnimation()
        logger.info("Changing animation to ${animation::class}")
        launch {
            animation.onStart(animator)

            animation.animate(animator)
            animator.applyPanels(10)
            api.flush()
            animationTask = restartableTimerTask {
                animator.updateTime()
                animation.animate(animator)
                animator.applyPanels(1)
                api.flush()
            }
            resumeAnimation(immediateStart = false)
        }
    }

    fun shutdownAnimation(){
        animationTimer.cancel()
    }

    fun pauseAnimation(keepImage: Boolean = false){
        animationTask?.cancel()
        if(!keepImage){
            logger.trace("Paused Animation with black!")
            animator.fill(Color.BLACK)
            animator.applyPanels(3)
            api.flush()
        }
    }

    fun resumeAnimation(hertz: Double = this.hertz?:10.0, immediateStart: Boolean = false){
        assert(hertz < 10)
        val delay = (1000.0/hertz).toLong()
        animationTimer.schedule(animationTask ?: return, if(immediateStart) 0 else delay, delay)
    }

    private val dischargeTimer = Timer("NanoLeafDischargeTimer")
    private val scheduledChanges = HashMap<Int, List<ChangeInfo>>()
    fun scheduleChange(changeInfo: ChangeInfo){
        val changes = scheduledChanges.getOrElse(changeInfo.panel.uid) { ArrayList() }
        val newChanges = changes.filter {
            (it.date < changeInfo.date).also { if(it) changeInfo.task.cancel()}
        } + listOf(changeInfo)
        scheduledChanges[changeInfo.panel.uid] = newChanges
        dischargeTimer.schedule(changeInfo.task, changeInfo.date)
    }

    fun cancelChangesFor(panel: Panel){
        scheduledChanges[panel.uid]?.forEach {
            it.task.cancel()
        }
        scheduledChanges.remove(panel.uid)
    }

    fun shutdown(){
        shutdownAnimation()
        dischargeTimer.cancel()
    }

    inner class Animator{
        fun randomBlink(){
            panels.random().color = Color((0..255).random(), (0..255).random(), (0..255).random())
        }

        fun dot(x: Double, y: Double, color: Color, radius: Double){
            val radSqr = Math.pow(radius, 2.0)
            val (h, s, b) = Color.RGBtoHSB(color.red, color.blue, color.green, floatArrayOf(0f, 0f, 0f))
            panels.forEach {
                val dist = Math.pow((x-it.x), 2.0)+Math.pow((y-it.y), 2.0)
                val bright = Math.max(1.0-(dist/radSqr),0.0)
                if(bright > 0)
                    it.color = it.color.blendWith(Color.getHSBColor(h, s, (b*bright).toFloat()))
            }
        }

        fun fill(color: Color){
            panels.forEach { it.color = color }
        }

        var delta: Long = 0
            private set
        var time: Long = 0
            private set

        fun updateTime(){
            val now = System.currentTimeMillis()
            delta = now-time
            time = now
        }

        fun panel(uid: Int) = panels.first{it.uid == uid}

        fun applyPanels(time: Byte){
            panels.forEach {
                it.apply(time)
            }
        }
    }
}



data class ChangeInfo(val panel: Panel, val date: Date, val duration: Byte, val task: RestartableTimerTask){
    constructor(panel: Panel, delay: Long, duration: Byte, task: RestartableTimerTask)
    :this(panel, Date(System.currentTimeMillis()+delay), duration, task)
}

private fun Color.blendWith(color: Color): Color {
    val c0 = this
    val totalAlpha = c0.alpha.toDouble() + color.alpha.toDouble()
    val weight0 = c0.alpha / totalAlpha
    val weight1 = color.alpha / totalAlpha

    val r = weight0 * c0.red + weight1 * color.red
    val g = weight0 * c0.green + weight1 * color.green
    val b = weight0 * c0.blue + weight1 * color.blue
    val a = Math.max(c0.alpha.toDouble(), color.alpha.toDouble())

    return Color(r.toInt(), g.toInt(), b.toInt(), a.toInt())
}

private fun <T> Iterable<T>.random(): T {
    return shuffled().take(1)[0]
}

private fun ClosedRange<Int>.random(): Int = Random().nextInt((endInclusive + 1) - start) +  start

class Panel(
        val x: Int,
        val y: Int,
        val o: Int,
        private val tempId: Int,
        val nanoLeaf: NanoLeaf,
        val uid: Int = "$y$x$o".hashCode()
){
    var color: Color = Color.BLACK

    fun apply(time: Byte = 1){
        nanoLeaf.cancelChangesFor(this)
        nanoLeaf.api.write(PanelFrame(tempId, color, time))
    }

    fun applyColor(color: Color, time: Byte = 1, flush: Boolean = true){
        this.color = color
        apply(time)
        if(flush)
            nanoLeaf.api.flush()
    }

    fun applyColorDelayed(color: Color, milliseconds: Long = 1000, duration: Byte = 1){
        nanoLeaf.scheduleChange(ChangeInfo(this, milliseconds, duration, restartableTimerTask {
            applyColor(color)
        }))
    }

    fun <T> whileColor(color: Color, dischargeTime: Long = 250, dischargeTransitionTime: Byte = 2, codeBlock: () -> T): T{
        val prevColor = this.color
        applyColor(color)
        return codeBlock().also {
            discharge(dischargeTime, dischargeTransitionTime, prevColor)
        }
    }

    fun discharge(milliseconds: Long, duration: Byte = 1, color: Color = Color.BLACK){
        nanoLeaf.scheduleChange(ChangeInfo(this, milliseconds, duration, restartableTimerTask {
            applyColor(color)
        }))
    }
}