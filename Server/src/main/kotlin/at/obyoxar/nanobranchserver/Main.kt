package at.obyoxar.nanobranchserver

import at.obyoxar.nanobranch.nanoleafconnector.NanoLeaf
import at.obyoxar.nanobranchserver.plugin.App
import at.obyoxar.nanobranchserver.plugin.TestExtension
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import org.pf4j.DefaultPluginManager
import sun.misc.Signal
import java.util.concurrent.Semaphore

private val logger = KotlinLogging.logger {  }

fun main(args: Array<String>){
    logger.info("Starting up")

    val pluginManager = DefaultPluginManager()
    logger.info("Loading plugins from ${pluginManager.pluginsRoot.toAbsolutePath()}")
    pluginManager.loadPlugins()
    pluginManager.startPlugins()

    val nanoLeaf = runBlocking {
        NanoLeaf.connectTo("192.168.170.121")
    }
    nanoLeaf.api.switchOn()

    val apps = pluginManager.getExtensions(App::class.java)
    logger.info("Loaded Apps:")
    apps.forEach {
        logger.info(it::class.simpleName)
    }

    /*
    launch {
        nanoLeaf.animation(DotsAnimation(nanoLeaf.size))

        delay(1000)

        nanoLeaf.animation(ClockAnimation(nanoLeaf), 0.1)
    }
*/

    val s = Semaphore(0)
    Signal.handle(Signal("INT")) {
        logger.info("Shutting down...")
        launch {
            nanoLeaf.shutdown()
            delay(100)
            s.release()
        }
    }
    s.acquire()
    logger.info("Shut down!")
}