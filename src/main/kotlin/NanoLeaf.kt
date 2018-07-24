import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.time.delay
import mu.KotlinLogging
import java.io.File
import java.net.InetAddress

private val logger = KotlinLogging.logger {  }
class NanoLeaf private constructor(ip: InetAddress, port: Short){

    val connector: NanoLeafConnector = NanoLeafConnector(ip, port).also { it.start() }

    init {

    }

    companion object {
        fun connectTo(connectionString: String): NanoLeaf {
            val parts = connectionString.split(Regex(":(?=\\d+\$)"), 2)
            val addr = InetAddress.getByName(parts[0])
            val port = parts.getOrNull(1)?.toShort() ?: 16021
            return NanoLeaf(addr, port)
        }
    }

    class NanoLeafConnector(val ip: InetAddress, val port: Short){

        private val api = NanoLeafApi(ip, port)

        fun start(){
            logger.info("NanoLeafConnector was started to connect to ${ip.hostAddress}:$port")

            api.loadToken(File("token.txt").takeIf { it.exists() }?.readLines()?.get(0))
        }
    }
}