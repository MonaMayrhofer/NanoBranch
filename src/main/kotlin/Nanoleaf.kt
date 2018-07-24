import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
import java.io.File
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.Charset
import java.util.*


class Nanoleaf(ip: String, port: Int){

    private val nanoLeafInfo = NanoLeafInfo(ip, port)
    private val address: String get() = "http://${nanoLeafInfo.ip}:${nanoLeafInfo.port}"

    private lateinit var authToken: String
    private lateinit var connectionInfo: StreamResponse
    private lateinit var destinationIp: InetAddress

    private val datagramSocket = DatagramSocket()
    private val streamCommand = """{"write":{"command":"display","animType":"extControl"}}"""

    suspend fun setup(){
        FuelManager.instance.basePath = address
        val file = File("token.txt")
        if(file.exists()){
            authToken = file.readLines(Charset.defaultCharset())[0]
        }else {
            println("Requesting new user!")
            try {
                val (_, response, result) = Fuel.post("/api/v1/new").awaitObjectResponse(NewUserResponse.Deserializer())
                val (obj, _) = result
                when (response.statusCode) {
                    403 -> throw NotInPairingModeException(nanoLeafInfo)
                }
                val authToken = obj?.auth_token ?: throw Exception("No Auth Token!")
                File("token.txt").writeText(authToken, Charset.defaultCharset())
                this.authToken = authToken
            } catch (ex: HttpException) {
                print(ex.message)
            }
        }
        FuelManager.instance.basePath = "$address/api/v1/$authToken"

        val streamData = Fuel.put("/effects").body(streamCommand).awaitObject(StreamResponse.Deserializer())

        if(streamData.streamControlProtocol != "udp") throw Exception("StreamControlProtocol '${streamData.streamControlProtocol}' is not supported yet!")

        connectionInfo = streamData
        destinationIp = InetAddress.getByName(connectionInfo.streamControlIpAddr)
    }

    suspend fun turn(on: Boolean) {
        Fuel.put("/state").body("{\"on\":{\"value\":$on}}").awaitStringResponse()
    }

    suspend fun getPanelInfo(): PanelInfos{
        return Fuel.get("/panelLayout/layout").awaitObject(PanelInfos.Deserializer())
    }

    private fun send(bytes: ByteArray){
        val packet = DatagramPacket(bytes, bytes.size, destinationIp, connectionInfo.streamControlPort)
        synchronized(datagramSocket){
            datagramSocket.send(packet)
        }
    }

    private fun arrayStreamFor(frames: Array<PanelFrame>): ByteArray{
        return byteArrayOf(frames.size.toByte()) + frames.flatMap { it.getStreamBytes().asIterable() }
    }

    fun push(frames: Array<PanelFrame>){
        send(arrayStreamFor(frames))
    }
}


data class PanelFrame(val panelId: Byte, val frames: Array<Frame>) {
    constructor(panelId: Byte, r: Byte, g: Byte, b: Byte, time: Byte = 1) : this(panelId, arrayOf(Frame(r, g, b, time)))
    constructor(panelId: Byte, color: Color, time: Byte = 1) : this(panelId, arrayOf(Frame(color, time)))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PanelFrame

        if (panelId != other.panelId) return false
        if (!Arrays.equals(frames, other.frames)) return false

        return true
    }

    fun getStreamBytes(): ByteArray{
        return byteArrayOf(panelId, frames.size.toByte()) + frames.flatMap { it.getStreamBytes().asIterable() }
    }

    override fun hashCode(): Int {
        var result = panelId.toInt()
        result = 31 * result + Arrays.hashCode(frames)
        return result
    }
}

data class Frame(val r: Byte, val g: Byte, val b: Byte, val time: Byte = 1) {
    constructor(color: Color, time: Byte) : this(color.red, color.green, color.blue, time)

    fun getStreamBytes(): ByteArray{
        return byteArrayOf(r, g, b, 0, time)
    }
}