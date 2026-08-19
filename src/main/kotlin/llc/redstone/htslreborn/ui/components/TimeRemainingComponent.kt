package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.systemsapi.api.ImportProgress
import net.minecraft.text.Text

class TimeRemainingComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing
) : FlowLayout(
    horizontalSizing, verticalSizing, Algorithm.VERTICAL
) {
    init {
        init()
    }

    fun init() {
        val progress = ImportProgress.current() ?: return

        val remaining = progress.remainingSeconds
            ?.let { if (progress.indeterminate) "~%.1fs".format(it) else "%.1fs".format(it) }
            ?: "--"

        val heading = "${progress.phase} ${(progress.fraction * 100f).toInt()}%  $remaining left"
        val detail = buildString {
            append(progress.completedSteps).append('/').append(progress.totalSteps)
            append("  elapsed ").append("%.1fs".format(progress.elapsedSeconds))
        }

        this.horizontalAlignment(HorizontalAlignment.CENTER)
        this.child(UIComponents.label(Text.of(heading)))
        this.child(ProgressBarComponent(Sizing.fixed(180), Sizing.fixed(8), progress.fraction))
        this.child(UIComponents.label(Text.of(detail)).apply {
            this.color(Color.ofArgb(0xFFAAAAAA.toInt()))
        })
    }
}