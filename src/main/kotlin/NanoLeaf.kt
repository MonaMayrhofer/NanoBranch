import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.time.delay
import mu.KotlinLogging
import java.io.File
import java.net.InetAddress

private val logger = KotlinLogging.logger {  }

class NanoLeaf private constructor(ip: InetAddress, port: Short){

    val api = NanoLeafStreamApi(ip, port)

    suspend fun start(){
        api.loadToken(File("token.txt").takeIf { it.exists() }?.readLines()?.get(0))
        api.startStream()
    }

    fun test(){
        launch {
            api.write(PanelFrame(27, Color(255, 0, 1), 1))
            api.flush()
            delay(1000)
            api.switchOff()
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
}