import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import sun.misc.Signal
import java.time.Clock
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.concurrent.thread
import kotlin.concurrent.timer



fun main(args: Array<String>){
    val s = Semaphore(0)
    val nanoleaf = Nanoleaf("192.168.170.121", 16021)
    runBlocking {
        nanoleaf.setup()
        nanoleaf.turn(true)
    }
    val controller = ClockController(nanoleaf)

    val server = embeddedServer(Netty, 9966){
        routing {
            get("/"){
                call.respondText("Started: ${controller.running}")
            }
            get("/start"){
                controller.tryStart()
                call.respondRedirect("/")
            }
            get("/stop"){
                controller.tryStop()
                launch {
                    nanoleaf.turn(false)
                }
                call.respondRedirect("/")
            }
            route("/config"){
                route("/color"){
                    get{
                        val c = controller.onColor
                        call.respondText("${c.red.toPositiveInt()}, ${c.green.toPositiveInt()}, ${c.blue.toPositiveInt()}")
                    }
                    post{
                        val str = call.receiveText()
                        println(str)
                        controller.onColor = Gson().fromJson(str, Color::class.java)
                        println(controller.onColor)
                    }
                }
                route("/timemultiplier"){
                    get{
                        call.respondText("${controller.timeMultiplier}")
                    }
                    post{
                        val str = call.receiveText()
                        println(str)
                        controller.timeMultiplier = str.toFloat()
                        println(controller.onColor)
                    }
                }
                route("/transition"){
                    get{
                        call.respondText("${controller.transitionTime}")
                    }
                    post{
                        val str = call.receiveText()
                        println(str)
                        controller.transitionTime = str.toInt()
                        println(controller.onColor)
                    }
                }
            }
        }
    }.start()


    Signal.handle(Signal("INT")) {
        println("Shutting down...")
        runBlocking {
            server.stop(500, 500, TimeUnit.MILLISECONDS)
            controller.tryStop()
            nanoleaf.turn(false)
            delay(1000)
        }
        s.release()
    }
    s.acquire()
    print("Shutdown...")
}