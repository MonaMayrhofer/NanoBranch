import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

private val logger = KotlinLogging.logger {  }

class NanoLeafStreamApi(ip: InetAddress, port: Short) : NanoLeafApi(ip, port) {
    lateinit var streamControlIpAddr: InetAddress
    var streamControlPort: Int = 0
    val socket = DatagramSocket()

    val buffer = HashMap<Int, ArrayList<PanelFrame>>()

    suspend fun startStream(){
        val streamData = this@NanoLeafStreamApi.effects.write(WriteDisplayExernalCommand())
        if(streamData.streamControlProtocol != "udp"){
            throw Exception("StreamControlProtocol '${streamData.streamControlProtocol}' is not supported!")
        }
        streamControlIpAddr = InetAddress.getByName(streamData.streamControlIpAddr)
        streamControlPort = streamData.streamControlPort
    }

    fun switchOn(){
        launch {
            this@NanoLeafStreamApi.state.on(true)
        }
    }

    fun switchOff(){
        launch {
            this@NanoLeafStreamApi.state.on(false)
        }
    }


    fun write(frame: PanelFrame){
        if(!buffer.containsKey(frame.panelId)){
            buffer[frame.panelId] = ArrayList()
        }
        buffer[frame.panelId]!!.add(frame)
    }

    fun flush(){
        val bytes = arrayOf(
                buffer.size.toByte()
        ) + buffer.flatMap {
            (arrayOf(it.key.toByte(), it.value.size.toByte()) + it.value.flatMap { frame ->
                arrayOf(frame.color.r, frame.color.g, frame.color.b, 0.toByte(), frame.time).asIterable()
            }).asIterable()
        }
        buffer.clear()

        logger.debug { bytes.map { it.toPositiveInt() }.joinToString() }

        val packet = DatagramPacket(bytes.toByteArray(), bytes.size, streamControlIpAddr, streamControlPort)
        socket.send(packet)
    }
}

data class PanelFrame(
        val panelId: Int,
        val color: Color,
        val time: Byte
)