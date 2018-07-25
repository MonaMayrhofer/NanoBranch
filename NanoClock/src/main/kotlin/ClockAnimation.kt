import at.obyoxar.nanoclock.nanoleafconnector.*
import kotlinx.coroutines.experimental.delay
import java.awt.Color
import java.util.*

class ClockAnimation(nanoLeaf: NanoLeaf): Animation{

    private val positions = nanoLeaf.browser.loadMap("ClockAnimation.map", listOf(
            "hAnchor", "mAnchor", "imAnchor",
            "h8", "h4", "h2", "h1",
            "m8", "m4", "m2", "m1",
            "im4", "im2", "im1"
    ))

    val onColor = Color.RED

    override fun animate(animator: NanoLeaf.Animator) {
        with(animator) {
            val calendar = Calendar.getInstance()
            calendar.time = Date((System.currentTimeMillis() * 1))
            val hours = calendar.get(Calendar.HOUR)
            val minutes = calendar.get(Calendar.MINUTE) / 5
            val innerMinutes = calendar.get(Calendar.MINUTE) % 5

            val colors = listOf(
                    "hAnchor" to onColor,
                    "mAnchor" to onColor,
                    "imAnchor" to onColor
            ) + listOf(
                    "h8" to hours / 8,
                    "h4" to hours / 4,
                    "h2" to hours / 2,
                    "h1" to hours,
                    "m8" to minutes / 8,
                    "m4" to minutes / 4,
                    "m2" to minutes / 2,
                    "m1" to minutes,
                    "im4" to innerMinutes / 4,
                    "im2" to innerMinutes / 2,
                    "im1" to innerMinutes / 1
            ).map { v -> v.first to if (v.second % 2 == 0) Color.BLACK else onColor }
            positions.setColors(colors)
        }
    }

    override suspend fun onStart(animator: NanoLeaf.Animator) {
        animator.fill(Color.BLACK)
        //animator.applyPanels(1)
        val colors = listOf(
                "hAnchor"       to Color.getHSBColor(0.25f, 1f, 1f),
                "mAnchor"       to Color.getHSBColor(0.25f, 1f, 1f),
                "imAnchor"      to Color.getHSBColor(0.25f, 1f, 1f),

                "h8"            to Color.getHSBColor(0.5f, 1f, 1f),
                "h4"            to Color.getHSBColor(0.5f, 1f, 0.5f),
                "h2"            to Color.getHSBColor(0.5f, 1f, 0.25f),
                "h1"            to Color.getHSBColor(0.5f, 1f, 0.05f),

                "m8"            to Color.getHSBColor(0.75f, 1f, 1f),
                "m4"            to Color.getHSBColor(0.75f, 1f, 0.5f),
                "m2"            to Color.getHSBColor(0.75f, 1f, 0.25f),
                "m1"            to Color.getHSBColor(0.75f, 1f, 0.05f),

                "im4"           to Color.getHSBColor(0.85f, 1f, 1f),
                "im2"           to Color.getHSBColor(0.85f, 1f, 0.6f),
                "im1"           to Color.getHSBColor(0.85f, 1f, 0.33f)
        )
        colors.forEachIndexed { index, pair ->
            positions[pair.first]!!.applyColor(pair.second, 30)
            delay(150)
            if((index-2) % 4 == 0)
                delay(500)
        }

        val colorMap = colors.toMap()
        delay(1000)
        positions.applyColors(positions.map { it.key to Color.getHSBColor(colorMap[it.key]!!.hue, 1f, 0.5f) }, 10)
        delay(1000)
        //positions.applyColors(positions.map { it.key to onColor }, 10)
        //delay(1000)
    }
}

