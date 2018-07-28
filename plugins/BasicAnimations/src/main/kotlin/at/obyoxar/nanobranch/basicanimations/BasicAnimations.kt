package at.obyoxar.nanobranch.basicanimations

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
