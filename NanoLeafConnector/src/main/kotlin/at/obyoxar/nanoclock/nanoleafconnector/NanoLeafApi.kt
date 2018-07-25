import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.Gson
import com.sun.org.apache.xpath.internal.operations.Bool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.setState
import java.io.OutputStream
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import javax.accessibility.AccessibleStateSet

private val logger = KotlinLogging.logger {}

suspend inline fun <reified T : Any> Request.awaitObject(): T{
    val (_, response, result) = awaitObjectResponse(gsonDeserializerOf<T>())
    val (objekt, _) = result
    if(response.statusCode != 200){
        throw Exception("Get of Property returned ${response.statusCode}") //TODO Better error handling
    }
    return objekt!!
}

suspend inline fun Request.awaitNothing() {
    val (_, response, result) = awaitByteArrayResponse()
    val (_, _) = result
    if(response.statusCode != 200){
        throw Exception("Get of Property returned ${response.statusCode}") //TODO Better error handling
    }
}

suspend inline fun Request.sendString(string: String) {
    val (_, response, _) = body(string).awaitByteArrayResponse()
    if(response.statusCode != 204){
        throw Exception("Set of Property returned ${response.statusCode}") //TODO Better error handling
    }
}


suspend inline fun <reified T : Any> Request.sendStringWithResponse(string: String): T {
    val (_, response, result) = body(string).awaitObjectResponse(gsonDeserializerOf<T>())
    //val (_, response, result) = body(string).awaitStringResponse()
    val (objekt, _) = result
    if(response.statusCode != 200){
        throw Exception("Set of Property returned ${response.statusCode}") //TODO Better error handling
    }
    return objekt!!
    //println(objekt!!)
    //return Gson().fromJson(objekt, T::class.java)
}

object StatelessNanoLeafApi {
    suspend fun newUser(address: String): AuthTokenResponse = Fuel.post("$address/api/v1/new").awaitObject()

    suspend fun completeInfo(address: String, authToken: String): CompleteInfo = Fuel.get("$address/api/v1/$authToken").awaitObject()


    private suspend inline fun <reified T : Any> getProp(address: String, authToken: String, group: String, state: String): T {
        return Fuel.get("$address/api/v1/$authToken/$group/$state").awaitObject()
    }

    private suspend inline fun <reified T : Any> setProp(address: String, authToken: String, group: String, state: String, value: T) {
        val body = """{"$state": ${Gson().toJson(value)}}"""
        val addr = "$address/api/v1/$authToken/$group"
        logger.trace("PUT '$addr' with '$body'")
        Fuel.put(addr).sendString(body)
    }

    private suspend inline fun <reified T : Any, reified U : Any> setPropReturn(address: String, authToken: String, group: String, state: String, value: T): U {
        val body = """{"$state": ${Gson().toJson(value)}}"""
        val addr = "$address/api/v1/$authToken/$group"
        logger.trace("PUT '$addr' with '$body'")
        return Fuel.put(addr).sendStringWithResponse(body)
    }

    private suspend inline fun sendIdentify(address: String, authToken: String) {
        val addr = "$address/api/v1/$authToken/identify"
        logger.trace("PUT '$addr'")
        Fuel.put(addr).awaitNothing()
    }

    private suspend inline fun <reified T : Any> getState(address: String, authToken: String, state: String): T = getProp(address, authToken, "state", state)
    private suspend inline fun <reified T : Any> setState(address: String, authToken: String, state: String, value: T) = setProp(address, authToken, "state", state, value)

    private suspend inline fun <reified T : Any> getEffects(address: String, authToken: String, state: String): T = getProp(address, authToken, "effects", state)
    private suspend inline fun <reified T : Any> setEffects(address: String, authToken: String, state: String, value: T) = setProp(address, authToken, "effects", state, value)
    private suspend inline fun <reified T : Any, reified U : Any> setEffectsReturn(address: String, authToken: String, state: String, value: T): U = setPropReturn(address, authToken, "effects", state, value)

    private suspend inline fun <reified T : Any> getPanelLayout(address: String, authToken: String, state: String): T = getProp(address, authToken, "panelLayout", state)
    private suspend inline fun <reified T : Any> setPanelLayout(address: String, authToken: String, state: String, value: T) = setProp(address, authToken, "panelLayout", state, value)

    object state {
        suspend fun on(address: String, authToken: String): BoolValue                           = getState(address, authToken, "on")
        suspend fun on(address: String, authToken: String, value: Boolean)                      = setState(address, authToken, "on", BoolValue(value))

        suspend fun brightness(address: String, authToken: String): RangeValue                  = getState(address, authToken, "brightness")
        suspend fun brightness(address: String, authToken: String, value: NumericTransition)    = setState(address, authToken, "brightness", value)
        suspend fun brightness(address: String, authToken: String, value: Increment)            = setState(address, authToken, "brightness", value)

        suspend fun hue(address: String, authToken: String): RangeValue                         = getState(address, authToken, "hue")
        suspend fun hue(address: String, authToken: String, value: NumericValue)                = setState(address, authToken, "hue", value)
        suspend fun hue(address: String, authToken: String, value: Increment)                   = setState(address, authToken, "hue", value)

        suspend fun saturation(address: String, authToken: String): RangeValue                  = getState(address, authToken, "sat")
        suspend fun saturation(address: String, authToken: String, value: NumericValue)         = setState(address, authToken, "sat", value)
        suspend fun saturation(address: String, authToken: String, value: Increment)            = setState(address, authToken, "sat", value)

        suspend fun temperature(address: String, authToken: String): RangeValue                 = getState(address, authToken, "ct")
        suspend fun temperature(address: String, authToken: String, value: NumericValue)        = setState(address, authToken, "ct", value)
        suspend fun temperature(address: String, authToken: String, value: Increment)           = setState(address, authToken, "ct", value)

        suspend fun colorMode(address: String, authToken: String): String                       = getState(address, authToken, "colorMode")
    }

    object effects {
        suspend fun select(address: String, authToken: String): String                          = getEffects(address, authToken, "select")
        suspend fun select(address: String, authToken: String, value: String)                   = setEffects(address, authToken, "select", value)

        suspend fun list(address: String, authToken: String): Array<String>                     = getEffects(address, authToken, "effectsList")

        suspend fun write(address: String, authToken: String, value: WriteCommand): WriteResponse = setEffectsReturn(address, authToken, "write", value)
        suspend fun write(address: String, authToken: String, value: WriteRequestAllCommand): WriteRequestAllResponse = setEffectsReturn(address, authToken, "write", value)
        suspend fun write(address: String, authToken: String, value: WriteDisplayExernalCommand): WriteDisplayExternalCommandResponse = setEffectsReturn(address, authToken, "write", value)
    }

    object panelLayout {
        suspend fun globalOrientation(address: String, authToken: String): RangeValue            = getPanelLayout(address, authToken, "globalOrientation")
        suspend fun globalOrientation(address: String, authToken: String, value: NumericValue)   = setPanelLayout(address, authToken, "globalOrientation", value)

        suspend fun layout(address: String, authToken: String): Layout                           = getPanelLayout(address, authToken, "layout")

        suspend fun identify(address: String, authToken: String)                                 = sendIdentify(address, authToken)
    }
}


open class NanoLeafApi(val ip: InetAddress, val port: Short) {

    val address get() = "http://${ip.hostAddress}:$port"
    lateinit var token: String
    private set

    lateinit var state: State
    lateinit var panelLayout: PanelLayout
    lateinit var effects: Effects

    suspend fun loadToken(token: String?){
        this@NanoLeafApi.token = token ?: StatelessNanoLeafApi.newUser(address).authToken
        state = State()
        panelLayout = PanelLayout()
        effects = Effects()
    }


    inner class State{
        suspend fun on(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): BoolValue                           = StatelessNanoLeafApi.state.on(address, authToken)
        suspend fun on(value: Boolean, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)                      = StatelessNanoLeafApi.state.on(address, authToken, value)

        suspend fun brightness(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue                  = StatelessNanoLeafApi.state.brightness(address, authToken)
        suspend fun brightness(value: NumericTransition, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)    = StatelessNanoLeafApi.state.brightness(address, authToken, value)
        suspend fun brightness(value: Increment, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)            = StatelessNanoLeafApi.state.brightness(address, authToken, value)

        suspend fun hue(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue                         = StatelessNanoLeafApi.state.hue(address, authToken)
        suspend fun hue(value: NumericValue, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)                = StatelessNanoLeafApi.state.hue(address, authToken, value)
        suspend fun hue(value: Increment, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)                   = StatelessNanoLeafApi.state.hue(address, authToken,value)

        suspend fun saturation(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue                  = StatelessNanoLeafApi.state.saturation(address, authToken)
        suspend fun saturation(value: NumericValue, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)         = StatelessNanoLeafApi.state.saturation(address, authToken, value)
        suspend fun saturation(value: Increment, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)            = StatelessNanoLeafApi.state.saturation(address, authToken,value)

        suspend fun temperature(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue                 = StatelessNanoLeafApi.state.temperature(address, authToken)
        suspend fun temperature(value: NumericValue, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)        = StatelessNanoLeafApi.state.temperature(address, authToken, value)
        suspend fun temperature(value: Increment, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)           = StatelessNanoLeafApi.state.temperature(address, authToken,value)

        suspend fun colorMode(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): String                       = StatelessNanoLeafApi.state.colorMode(address, authToken)
    }

    inner class Effects {
        suspend fun select(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): String                          = StatelessNanoLeafApi.effects.select(address, authToken)
        suspend fun select(value: String, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)                   = StatelessNanoLeafApi.effects.select(address, authToken, value)

        suspend fun list(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): Array<String>                     = StatelessNanoLeafApi.effects.list(address, authToken)

        suspend fun write(value: WriteCommand, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): WriteResponse = StatelessNanoLeafApi.effects.write(address, authToken, value)
        suspend fun write(value: WriteRequestAllCommand, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): WriteRequestAllResponse = StatelessNanoLeafApi.effects.write(address, authToken, value)
        suspend fun write(value: WriteDisplayExernalCommand, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): WriteDisplayExternalCommandResponse = StatelessNanoLeafApi.effects.write(address, authToken, value)
    }

    inner class PanelLayout {
        suspend fun globalOrientation(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): RangeValue            = StatelessNanoLeafApi.panelLayout.globalOrientation(address, authToken)
        suspend fun globalOrientation(value: NumericValue, address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)   = StatelessNanoLeafApi.panelLayout.globalOrientation(address, authToken, value)

        suspend fun layout(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token): Layout                           = StatelessNanoLeafApi.panelLayout.layout(address, authToken)

        suspend fun identify(address: String = this@NanoLeafApi.address, authToken: String = this@NanoLeafApi.token)                                 = StatelessNanoLeafApi.panelLayout.identify(address, authToken)
    }
}

abstract class WriteCommand
data class WriteDisplayExernalCommand(val animType: String = "extControl", val command: String = "display")
data class WriteRequestCommand(val animName: String, val command: String = "request") : WriteCommand()
data class WriteRequestAllCommand(val command: String = "requestAll")

data class WriteDisplayExternalCommandResponse(
        val streamControlIpAddr: String,
        val streamControlPort: Int,
        val streamControlProtocol: String
)

data class WriteRequestAllResponse(
        val animations: Array<WriteResponse>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WriteRequestAllResponse

        if (!Arrays.equals(animations, other.animations)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(animations)
    }
}

data class WriteResponse(
        val animName: String,
        val loop: Boolean,
        val palette: Array<HSBValue>,
        val transTime: MinMaxValue,
        val windowSize: Int,
        val flowFactor: Float,
        val delayTime: MinMaxValue,
        val colorType: String,
        val animType: String,
        val explodeFactor: Float,
        val brightnessRange: MinMaxValue,
        val direction: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WriteResponse

        if (animName != other.animName) return false
        if (loop != other.loop) return false
        if (!Arrays.equals(palette, other.palette)) return false
        if (transTime != other.transTime) return false
        if (windowSize != other.windowSize) return false
        if (flowFactor != other.flowFactor) return false
        if (delayTime != other.delayTime) return false
        if (colorType != other.colorType) return false
        if (animType != other.animType) return false
        if (explodeFactor != other.explodeFactor) return false
        if (brightnessRange != other.brightnessRange) return false
        if (direction != other.direction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = animName.hashCode()
        result = 31 * result + loop.hashCode()
        result = 31 * result + Arrays.hashCode(palette)
        result = 31 * result + transTime.hashCode()
        result = 31 * result + windowSize
        result = 31 * result + flowFactor.hashCode()
        result = 31 * result + delayTime.hashCode()
        result = 31 * result + colorType.hashCode()
        result = 31 * result + animType.hashCode()
        result = 31 * result + explodeFactor.hashCode()
        result = 31 * result + brightnessRange.hashCode()
        result = 31 * result + direction.hashCode()
        return result
    }
}

data class MinMaxValue(
        val maxValue: Int,
        val minValue: Int
)
data class HSBValue(
        val hue: Int,
        val saturation: Int,
        val brightness: Int
)

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