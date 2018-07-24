open class NanoException : Exception()

class NotInPairingModeException(val info: NanoLeafInfo) : NanoException()
