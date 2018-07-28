package at.obyoxar.nanobranch.nanoleafconnector

import mu.KotlinLogging
import java.awt.Color
import java.io.File

private val logger = KotlinLogging.logger {  }

class NanoLeafBrowser(val nanoLeaf: NanoLeaf){
    fun browse(position: Collection<String>, informant: (String) -> Unit = {println(it)}): Map<String, Panel> {
        logger.info("Started NanoLeafBrowser to browse for missing positions: ")
        position.forEach {
            logger.info("  - $it")
        }

        nanoLeaf.pauseAnimation()

        informant("Assigning Positions: ${position.joinToString()}")
        return position.map {

            var panel: Panel? = null

            while(panel == null){
                forPanels@for(curr in nanoLeaf.panels){
                    val keep = curr.whileColor(Color.BLUE, dischargeTransitionTime = 5, dischargeTime = 50){
                        informant("Position $it: [Enter to move] [yY to accept]")
                        var line = readLine()
                        while(line == null)
                            line = readLine()
                        line.toUpperCase() == "Y"
                    }
                    if(keep) {
                        panel = curr
                        curr.applyColor(Color.GREEN)
                        break
                    }
                }
            }
            informant("Position $it was assigned to: ${panel.uid}")
            logger.info("Position $it was assigned to: ${panel.uid}")
            it to panel
        }.toMap()
                .also { logger.info("Browse finished!") }
                .also { nanoLeaf.resumeAnimation() }
    }

    fun loadMap(path: String, positions: Collection<String>, saveAfterLoad: Boolean = true): Map<String, Panel> = loadMap(File(path), positions)
    fun loadMap(file: File, positions: Collection<String>, saveAfterLoad: Boolean = true): Map<String, Panel> {
        logger.info("Loading Positionsmap from ${file.absolutePath}")
        val filePositions = if(!file.exists()) mapOf() else file.readLines().map {
            val (position, panelId) = it.split(Regex(":"), 2)
            val panelUid = panelId.trim().toInt()
            position to nanoLeaf.panels.first{
                it.uid == panelUid
            }
        }.filter { positions.contains(it.first) }.toMap()

        val missing = positions.minus(filePositions.keys)

        return (filePositions + browse(missing)).also { if(saveAfterLoad && missing.isNotEmpty()) saveMap(file, it) }
    }

    fun saveMap(filePath: String, positionMap: Map<String, Panel>) = saveMap(File(filePath), positionMap)
    fun saveMap(file: File, positionMap: Map<String, Panel>){
        logger.info("Saving Positionsmap to ${file.absolutePath}")
        val writer = file.printWriter().use { out ->
            positionMap.forEach { position, panel ->
                out.write("$position: ${panel.uid}\n")
            }
        }
    }
}
fun Map<String, Panel>.setColors(colors: List<Pair<String, Color>>) {
    colors.forEach {
        this[it.first]!!.color = it.second
    }
}

fun Map<String, Panel>.applyColors(colors: List<Pair<String, Color>>, transition: Byte = 1) {
    colors.forEach {
        this[it.first]!!.applyColor(it.second, transition, true)
    }

}
