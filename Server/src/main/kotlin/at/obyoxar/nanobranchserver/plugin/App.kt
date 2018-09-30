package at.obyoxar.nanobranchserver.plugin

import at.obyoxar.nanobranch.nanoleafconnector.Animation
import at.obyoxar.nanobranch.nanoleafconnector.NanoLeaf
import org.pf4j.ExtensionPoint

abstract class App : ExtensionPoint {
    abstract fun getNewView(nanoLeaf: NanoLeaf): Animation
}