import com.sun.javafx.geom.Vec2d
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import sun.misc.Signal
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.math.sign

private val logger = KotlinLogging.logger {  }
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

fun main(args: Array<String>){
    val nanoLeaf = runBlocking {
        NanoLeaf.connectTo("192.168.170.121")
    }

    nanoLeaf.api.switchOn()

    val dots = arrayOf(
            Dot(nanoLeaf.size, Color.RED),
            Dot(nanoLeaf.size, Color.GREEN),
            Dot(nanoLeaf.size, Color.CYAN)
    )

    nanoLeaf.animation{
        fill(Color.BLACK)
        dots.forEach { it.tick() }
        dots.forEach {
            dot(it.offsetX, it.offsetY, it.color, 150.0)
        }

    }


    val s = Semaphore(0)
    Signal.handle(Signal("INT")) {
        logger.info("Shutting down...")
        launch {
            nanoLeaf.stopAnimation()
            nanoLeaf.api.switchOff()
            delay(100)
            s.release()
        }
    }
    s.acquire()
    logger.info("Shut down!")
}