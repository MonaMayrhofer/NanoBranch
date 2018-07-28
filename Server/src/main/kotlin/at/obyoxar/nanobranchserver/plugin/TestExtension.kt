package at.obyoxar.nanobranchserver.plugin

import org.pf4j.ExtensionPoint

interface TestExtension: ExtensionPoint{
    fun helloWorld()
}