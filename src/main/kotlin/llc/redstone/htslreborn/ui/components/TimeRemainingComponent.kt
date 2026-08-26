package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.systemsapi.api.ImportProgress
import net.minecraft.network.chat.Component

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
            ?.let {
                val clamped = maxOf(it.toDouble(), 0.0)
                if (progress.indeterminate) "~%.1fs".format(clamped) else "%.1fs".format(clamped)
            }
            ?: "--"

        val detail = Component.translatable(
            "htslreborn.importing.working.detail",
            Component.translatable("htslreborn.importing.phase.${progress.phase.name.lowercase()}"),
            remaining
        )

        this.horizontalAlignment(HorizontalAlignment.CENTER)
        this.gap(6)
        this.child(
            UIContainers.horizontalFlow(Sizing.fixed(210), Sizing.fixed(8)).apply {
                this.child(ProgressBarComponent(Sizing.fixed(180), Sizing.fixed(8), progress.fraction))
                this.child(UIComponents.label(Component.literal(" ${(progress.fraction * 100f).toInt()}%")).apply {
                    this.color(Color.ofArgb(0xFFAAAAAA.toInt()))
                })
            }
        )

        this.child(UIComponents.label(detail).apply {
            this.color(Color.ofArgb(0xFFAAAAAA.toInt()))
        })
    }
}