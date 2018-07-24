/*import kotlinx.coroutines.experimental.runBlocking
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timer
import kotlin.properties.Delegates


class ClockController(val nanoleaf: Nanoleaf){

    private var timer: Timer? = null
    private val timePanels: ByteArray by lazy {
        runBlocking {
            println("Fetching Panel-Infos")
            val positionalPanels = nanoleaf.getPanelInfo().positionData
            arrayOf(
                    74 to 822, 0 to 433, //Hours indicator, Minutes indicator
                    -74 to 649, 0 to 692, 0 to 779, -74 to 822, //Hours
                    -149 to 259, -74 to 303, -74 to 389, -149 to 433 //Minutes
            ).map {
                pos -> positionalPanels.firstOrNull {
                    it.x == pos.first && it.y == pos.second
                }?.panelId?.toByte() ?: throw Exception("No panel found @ $pos")
            }.toByteArray()
        }
    }
    val running
        get() = (timer!=null)


    var onColor: Color by Delegates.observable(Color(255)){ _, _, _ ->
        updateLeaves(true)
    }
    var timeMultiplier = 1f
    var transitionTime = 100

    private fun render(hours: Int, minutes: Int): Array<PanelFrame>{
        val fourMinutes = minutes/4
        return arrayOf(
                PanelFrame(timePanels[0], onColor), PanelFrame(timePanels[1], onColor)
        ) + arrayOf(
                hours, hours / 2, hours / 4, hours / 8,
                fourMinutes, fourMinutes / 2, fourMinutes / 4, fourMinutes / 8
        ).mapIndexed { index, on -> PanelFrame(timePanels[index+2], if(on % 2 == 1)onColor else Color(), transitionTime.toByte()) }
    }

    fun tryStart() = (!running).also { if(it) start() }
    fun tryStop() = (running).also { if(it) stop() }

    fun start() {
        assert(!running)
        timer = timer("UpdateClock", period = (1_000).toLong(), action = {
            updateLeaves()
        })
    }

    private var lastHour = 0
    private var lastMinute = 0
    private fun updateLeaves(force: Boolean = false){
        val calendar = Calendar.getInstance()
        calendar.time = Date((System.currentTimeMillis()*timeMultiplier).toLong())
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        if(force || (lastHour != hour && lastMinute != minute)){
            println("Update!")
            nanoleaf.push(render(hour, minute))
            lastHour = hour
            lastMinute = minute
        }
    }

    fun stop(){
        assert(running)
        timer!!.cancel()
    }

    fun manual(){
        val later = Timer()
        do {
            val str = readLine()!!
            val id = str.toInt().toByte()
            nanoleaf.push(arrayOf(PanelFrame(id, Color(255))))
            later.schedule(delay = 1000) {
                nanoleaf.push(arrayOf(PanelFrame(id, Color())))
            }
        }while(str != ".")
    }
}*/