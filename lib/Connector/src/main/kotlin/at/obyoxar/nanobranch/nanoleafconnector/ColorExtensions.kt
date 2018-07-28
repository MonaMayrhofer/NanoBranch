package at.obyoxar.nanobranch.nanoleafconnector

import java.awt.Color


val Color.hsb: Triple<Float, Float, Float>
    get() {
        val vals = Color.RGBtoHSB(red, green, blue, null)
        return Triple(vals[0], vals[1], vals[2])
    }

val Color.hue: Float
    get() = this.hsb.first

val Color.saturation: Float
    get() = this.hsb.second

val Color.brightness: Float
    get() = this.hsb.third

