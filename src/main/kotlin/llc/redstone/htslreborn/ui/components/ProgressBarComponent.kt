package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.BoxComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import kotlin.math.roundToInt

class ProgressBarComponent(
    horizontalSizing: Sizing,
    verticalSizing: Sizing,
    progress: Float,
    private val backgroundColor: Int = 0xFF4A4A4A.toInt(),
    private val fillColor: Int = 0xFF5CB85C.toInt()
) : FlowLayout(horizontalSizing, verticalSizing, Algorithm.VERTICAL) {

    private val fillBox: BoxComponent
    private val barContainer: FlowLayout
    private var currentProgress = 0f

    init {
        barContainer = UIContainers.verticalFlow(Sizing.fill(), Sizing.fill()).apply {
            child(
                UIComponents.box(Sizing.fill(), Sizing.fill()).apply {
                    color(Color.ofArgb(backgroundColor))
                    fill(true)
                }
            )
            fillBox = UIComponents.box(Sizing.fixed(0), Sizing.fill()).apply {
                color(Color.ofArgb(fillColor))
                fill(true)
                positioning(Positioning.absolute(0, 0))
            }
            child(fillBox)
        }
        child(barContainer)
        setProgress(progress)
    }

    fun setProgress(value: Float) {
        currentProgress = value.coerceIn(0f, 1f)
        updateFillWidth()
    }

    override fun draw(graphics: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        updateFillWidth()
        super.draw(graphics, mouseX, mouseY, partialTicks, delta)
    }

    private fun updateFillWidth() {
        val containerWidth = barContainer.width()
        if (containerWidth <= 0) {
            fillBox.horizontalSizing(Sizing.fixed(0))
            return
        }

        val fillWidth = when {
            currentProgress <= 0f -> 0
            currentProgress >= 1f -> containerWidth
            else -> (containerWidth * currentProgress).roundToInt().coerceIn(1, containerWidth)
        }
        fillBox.horizontalSizing(Sizing.fixed(fillWidth))
    }
}
