package at.obyoxar.nanobranch.nanoleafconnector

import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import java.awt.Color
import java.io.Flushable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

private val logger = KotlinLogging.logger {  }

class NanoLeafStreamApi(ip: InetAddress, port: Short) : NanoLeafApi(ip, port), Flushable {
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
        synchronized(buffer){
            if(!buffer.containsKey(frame.panelId)){
                buffer[frame.panelId] = ArrayList()
            }
            buffer[frame.panelId]!!.add(frame)
        }
    }

    override fun flush(){
        synchronized(buffer){
            if(buffer.isEmpty())
                return
            val bytes = arrayOf(
                    buffer.size.toByte()
            ) + buffer.flatMap {
                (arrayOf(it.key.toByte(), it.value.size.toByte()) + it.value.flatMap { frame ->
                    arrayOf(frame.color.red.toByte(), frame.color.green.toByte(), frame.color.blue.toByte(), 0.toByte(), frame.time).asIterable()
                }).asIterable()
            }
            buffer.clear()



            logger.trace { bytes.map { it.toPositiveInt() }.joinToString() }

            val packet = DatagramPacket(bytes.toByteArray(), bytes.size, streamControlIpAddr, streamControlPort)
            socket.send(packet)
        }
    }
}

fun Byte.toPositiveInt() = toInt() and 0xFF

data class PanelFrame(
        val panelId: Int,
        val color: Color,
        val time: Byte
)