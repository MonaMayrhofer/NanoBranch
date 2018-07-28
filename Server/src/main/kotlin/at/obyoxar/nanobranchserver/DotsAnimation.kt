package at.obyoxar.nanobranchserver

import at.obyoxar.nanobranch.nanoleafconnector.Animation
import at.obyoxar.nanobranch.nanoleafconnector.NanoLeaf
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.util.*

private val random = Random()

class Dot(val bounds: Rectangle2D, val color: Color){
    var offsetX = random.nextDouble()*bounds.width+bounds.minX
    var offsetY = random.nextDouble()*bounds.height+bounds.minY

    var velX = random.nextDouble()*40-20
    var velY = random.nextDouble()*40-20

    fun tick(){
        if(offsetX > bounds.maxX-20) velX *= -1
        if(offsetY > bounds.maxY-20) velY *= -1
        if(offsetX < bounds.minX+20) velX *= -1
        if(offsetY < bounds.minY+20) velY *= -1

        offsetX += velX
        offsetY += velY
    }

}
class DotsAnimation(boundingBox: Rectangle2D) : Animation {
    val dots = arrayOf(
            Dot(boundingBox, Color.RED),
            Dot(boundingBox, Color.GREEN),
            Dot(boundingBox, Color.CYAN)
    )
    override fun animate(animator: NanoLeaf.Animator) {
        with(animator){
            fill(Color.BLACK)
            dots.forEach { it.tick() }
            dots.forEach {
                dot(it.offsetX, it.offsetY, it.color, 150.0)
            }
        }
    }
}