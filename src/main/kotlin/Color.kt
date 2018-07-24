data class Color(val r: Byte = 0, val g: Byte = 0, val b: Byte = 0){
    constructor(r: Int, g: Int = 0, b: Int = 0) : this(r.toByte(), g.toByte(), b.toByte())
    val red get() = r
    val blue get() = b
    val green get() = g
}

fun Byte.toPositiveInt() = toInt() and 0xFF