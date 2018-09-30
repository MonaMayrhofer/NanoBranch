package at.obyoxar.nanobranch.basicanimations

import at.obyoxar.nanobranch.nanoleafconnector.Animation
import at.obyoxar.nanobranch.nanoleafconnector.NanoLeaf
import at.obyoxar.nanobranchserver.plugin.App
import at.obyoxar.nanobranchserver.plugin.TestExtension
import mu.KotlinLogging
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper

private val logger = KotlinLogging.logger {}

class BasicAnimations(wrapper: PluginWrapper): Plugin(wrapper) {
    override fun start() {
        logger.info("ClockAnimationPlugin was started")
    }

    override fun stop() {
        logger.info("ClockAnimationPlugin was stopped")
    }
}
@Extension
class ClockApp: App() {
    override fun getNewView(nanoLeaf: NanoLeaf): Animation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}