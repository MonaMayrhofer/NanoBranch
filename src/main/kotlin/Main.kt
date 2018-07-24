import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import sun.misc.Signal
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

fun main(args: Array<String>){
    val nanoLeaf = runBlocking {
        NanoLeaf.connectTo("192.168.170.121")
    }
    nanoLeaf.test()


    val s = Semaphore(0)
    Signal.handle(Signal("INT")) {
        println("Shutting down...")
        s.release()
    }
    s.acquire()
    print("Shutdown...")
}