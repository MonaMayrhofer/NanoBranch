import mu.KotlinLogging
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.io.File
import java.net.InetAddress
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.properties.Delegates

private val logger = KotlinLogging.logger {  }

class NanoLeaf private constructor(ip: InetAddress, port: Short){

    val api = NanoLeafStreamApi(ip, port)

    lateinit var panels: List<Panel>
        private set
    val size = Rectangle2D.Double()
    val animator = Animator()

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

    val animationTimer = Timer()
    var animationTask: TimerTask? = null

    fun animation(hertz: Double = 10.0, animation: Animator.() -> Unit){
        assert(hertz < 10)
        pauseAnimation()
        animationTask = timerTask {
            animator.animation()
            panels.forEach(Panel::apply)
            api.flush()
        }
        playAnimation(hertz)
    }

    fun stopAnimation(){
        animationTimer.cancel()
    }

    fun pauseAnimation(){
        animationTask?.cancel()
    }

    fun playAnimation(hertz: Double){
        animationTimer.schedule(animationTask, 0, (1000.0/hertz).toLong())
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
    }
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

    fun apply(){
        nanoLeaf.api.write(PanelFrame(tempId, color, 1))
    }
}