import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import java.util.*

data class NewUserResponse(val auth_token: String){
    class Deserializer: ResponseDeserializable<NewUserResponse> {
        override fun deserialize(content: String) = Gson().fromJson(content, NewUserResponse::class.java)
    }
}

// StreamData {"streamControlIpAddr":"192.168.170.121", "streamControlPort":60221, "streamControlProtocol":"udp"}
data class StreamResponse(val streamControlIpAddr: String, val streamControlPort: Int, val streamControlProtocol: String){
    class Deserializer: ResponseDeserializable<StreamResponse> {
        override fun deserialize(content: String): StreamResponse? {
            return Gson().fromJson(content, StreamResponse::class.java)
        }
    }
}

data class PanelInfos(val numPanels: Int, val sideLength: Int, val positionData: Array<PanelInfo>) {
    class Deserializer: ResponseDeserializable<PanelInfos> {
        override fun deserialize(content: String): PanelInfos? {
            return Gson().fromJson(content, PanelInfos::class.java)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PanelInfos

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

data class PanelInfo(val panelId: Int, val x: Int, val y: Int, val o: Int)