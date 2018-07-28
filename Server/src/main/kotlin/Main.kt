import at.obyoxar.nanoclock.nanoleafconnector.NanoLeaf
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import sun.misc.Signal
import java.util.concurrent.Semaphore

private val logger = KotlinLogging.logger {  }

fun main(args: Array<String>){
    val nanoLeaf = runBlocking {
        NanoLeaf.connectTo("192.168.170.121")
    }
    nanoLeaf.api.switchOn()

    launch {
        nanoLeaf.animation(DotsAnimation(nanoLeaf.size))

        delay(1000)

        nanoLeaf.animation(ClockAnimation(nanoLeaf), 0.1)
    }

    val s = Semaphore(0)
    Signal.handle(Signal("INT")) {
        logger.info("Shutting down...")
        launch {
            nanoLeaf.shutdown()
            nanoLeaf.api.switchOff()
            delay(100)
            s.release()
        }
    }
    s.acquire()
    logger.info("Shut down!")
}