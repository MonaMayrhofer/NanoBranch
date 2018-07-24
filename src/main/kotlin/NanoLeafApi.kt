import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.Gson
import com.sun.org.apache.xpath.internal.operations.Bool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.setState
import java.net.InetAddress
import java.util.*
import javax.accessibility.AccessibleStateSet

private val logger = KotlinLogging.logger {}

suspend inline fun <reified T : Any> Request.awaitObject(): T{
    return awaitObject(gsonDeserializerOf())
}



suspend inline fun Request.sendString(string: String) {
    val (_, response, _) = body(string).awaitByteArrayResponse()
    if(response.statusCode != 204){
        throw Exception("Set of Property returned ${response.statusCode}") //TODO Better error handling
    }
}

object StatelessNanoLeafApi {
    suspend fun newUser(address: String): AuthTokenResponse = Fuel.post("$address/api/v1/new").awaitObject()

    suspend fun completeInfo(address: String, authToken: String): CompleteInfo = Fuel.get("$address/api/v1/$authToken").awaitObject()

    private suspend inline fun <reified T : Any> getState(address: String, authToken: String, state: String): T {
        return Fuel.get("$address/api/v1/$authToken/state/$state").awaitObject()
    }

    private suspend inline fun <reified T : Any> setState(address: String, authToken: String, state: String, value: T) {
        val body = """{"$state": ${Gson().toJson(value)}}"""
        val addr = "$address/api/v1/$authToken/state"
        logger.trace("PUT '$addr' with '$body'")
        Fuel.put(addr).sendString(body)
    }

    object state {
        suspend fun on(address: String, authToken: String): BoolValue                           = getState(address, authToken, "on")
        suspend fun on(address: String, authToken: String, value: Boolean)                      = setState(address, authToken, "on", BoolValue(value))

        suspend fun brightness(address: String, authToken: String): RangeValue                  = getState(address, authToken, "brightness")
        suspend fun brightness(address: String, authToken: String, value: NumericTransition)    = setState(address, authToken, "brightness", value)
        suspend fun brightness(address: String, authToken: String, value: Increment)            = setState(address, authToken, "brightness", value)

        suspend fun hue(address: String, authToken: String): RangeValue                         = getState(address, authToken, "hue")
        suspend fun hue(address: String, authToken: String, value: NumericValue)           = setState(address, authToken, "hue", value)
        suspend fun hue(address: String, authToken: String, value: Increment)                   = setState(address, authToken, "hue", value)

        suspend fun saturation(address: String, authToken: String): RangeValue                  = getState(address, authToken, "sat")
        suspend fun saturation(address: String, authToken: String, value: NumericValue)    = setState(address, authToken, "sat", value)
        suspend fun saturation(address: String, authToken: String, value: Increment)            = setState(address, authToken, "sat", value)

        suspend fun temperature(address: String, authToken: String): RangeValue                 = getState(address, authToken, "ct")
        suspend fun temperature(address: String, authToken: String, value: NumericValue)   = setState(address, authToken, "ct", value)
        suspend fun temperature(address: String, authToken: String, value: Increment)           = setState(address, authToken, "ct", value)

        suspend fun colorMode(address: String, authToken: String): String                       = getState(address, authToken, "colorMode")
    }
}


class NanoLeafApi(val ip: InetAddress, val port: Short) {

    val address get() = "http://${ip.hostAddress}:$port"
    lateinit var token: String
    private set

    fun loadToken(token: String?){
        launch {
            this@NanoLeafApi.token = token ?: StatelessNanoLeafApi.newUser(address).authToken
        }
    }

    val state = State()

    inner class State{
        suspend fun on(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): BoolValue                           = StatelessNanoLeafApi.state.on(address, authToken)
        suspend fun on(value: Boolean, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)                      = StatelessNanoLeafApi.state.on(address, authToken, value)

        suspend fun brightness(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue                  = StatelessNanoLeafApi.state.brightness(address, authToken)
        suspend fun brightness(value: NumericTransition, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)    = StatelessNanoLeafApi.state.brightness(address, authToken, value)
        suspend fun brightness(value: Increment, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)            = StatelessNanoLeafApi.state.brightness(address, authToken, value)

        suspend fun hue(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue                         = StatelessNanoLeafApi.state.hue(address, authToken)
        suspend fun hue(value: NumericValue, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)           = StatelessNanoLeafApi.state.hue(address, authToken, value)
        suspend fun hue(value: Increment, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)                   = StatelessNanoLeafApi.state.hue(address, authToken,value)

        suspend fun saturation(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue                  = StatelessNanoLeafApi.state.saturation(address, authToken)
        suspend fun saturation(value: NumericValue, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)    = StatelessNanoLeafApi.state.saturation(address, authToken, value)
        suspend fun saturation(value: Increment, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)            = StatelessNanoLeafApi.state.saturation(address, authToken,value)

        suspend fun temperature(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue                 = StatelessNanoLeafApi.state.temperature(address, authToken)
        suspend fun temperature(value: NumericValue, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)   = StatelessNanoLeafApi.state.temperature(address, authToken, value)
        suspend fun temperature(value: Increment, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)           = StatelessNanoLeafApi.state.temperature(address, authToken,value)

        suspend fun colorMode(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): String                                                   = StatelessNanoLeafApi.state.colorMode(address, authToken)
    }
}

data class NumericValue(val value: Int)
data class NumericTransition(val value: Int, val duration: Int)
data class Increment(val increment: Int, val duration: Int? = null)

data class AuthTokenResponse(val authToken: String)
data class CompleteInfo(
        val name: String,
        val serialNo: String,
        val manufacturer: String,
        val firmwareVersion: String,
        val model: String,
        val state: State
)

data class State(
        val on: BoolValue,
        val brightness: RangeValue,
        val hue: RangeValue,
        val sat: RangeValue,
        val ct: RangeValue,
        val colorMode: String,
        val effects: Effects,
        val panelLayout: PanelLayout,
        val rhythm: Rhythm
)

data class BoolValue(val value: Boolean)
data class RangeValue(val value: Int, val max: Int, val min: Int)
data class Effects(
        val select: String,
        val effectsList: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Effects

        if (select != other.select) return false
        if (!Arrays.equals(effectsList, other.effectsList)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = select.hashCode()
        result = 31 * result + Arrays.hashCode(effectsList)
        return result
    }
}

data class PanelLayout(
        val layout: Layout,
        val globalOrientation: RangeValue
)

data class Layout(
        val numPanels: Int,
        val sideLength: Int,
        val positionData: Array<PositionalData>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Layout

        if (numPanels != other.numPanels) return false
        if (sideLength != other.sideLength) return false
        if (!Arrays.equals(positionData, other.positionData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = numPanels
        result = 31 * result + sideLength
        result = 31 * result + Arrays.hashCode(positionData)
        return result
    }
}

data class PositionalData(
        val panelId: Int,
        val x: Int,
        val y: Int,
        val o: Int
)

data class Rhythm(
        val rhythmConnected: Boolean,
        val rhythmActive: Boolean,
        val rhythmId: Int,
        val hardwareVersion: String,
        val firmwareVersion: String,
        val auxAvailable: Boolean,
        val rhythmMode: Int,
        val rhythmPos: Position
)

data class Position(
        val x: Int,
        val y: Int,
        val o: Int
)