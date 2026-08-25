package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.BoxComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.CursorStyle
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileExplorerHandler
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.resources.Identifier

abstract class ExplorerEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, open val index: Int
): FlowLayout(horizontalSizing, verticalSizing, Algorithm.HORIZONTAL) {

    init {
        mouseEnter().subscribe {
            cursorStyle(CursorStyle.HAND)
        }
    }

    abstract val icon: Identifier
    abstract fun buildContextButtons(): List<UIComponent>

    override fun child(child: UIComponent?): FlowLayout? {
        val value = super.child(child)

        // Render this on top of everything
        val clickmask = this.childById(BoxComponent::class.java, "clickmask")
        if (clickmask != null) removeChild(clickmask)
        super.child(
            UIComponents.box(Sizing.fill(), Sizing.fill()).apply {
                id("clickmask")
                positioning(Positioning.absolute(0, 0))
                color(Color.ofArgb(0))
                fill(true)
                mouseDown().subscribe { click, bool ->
                    // why twice? idfk but IT WORKS
                    this@ExplorerEntryComponent.onMouseDown(click, bool)
                    this@ExplorerEntryComponent.onMouseDown(click, bool)
                }
                mouseEnter()?.subscribe {
                    cursorStyle(CursorStyle.HAND)
                }
            }
        )

        return value
    }

    override fun children(children: Collection<UIComponent?>?): FlowLayout? {
        val value = super.children(children)

        // Render this on top of everything
        val clickmask = childById(BoxComponent::class.java, "clickmask")
        if (clickmask != null) removeChild(clickmask)
        super.child(
            UIComponents.box(Sizing.fill(), Sizing.fill()).apply {
                id("clickmask")
                positioning(Positioning.absolute(0, 0))
                color(Color.ofArgb(0))
                fill(true)
                mouseDown().subscribe { click, bool ->
                    // why twice? idfk but IT WORKS
                    this@ExplorerEntryComponent.onMouseDown(click, bool)
                    this@ExplorerEntryComponent.onMouseDown(click, bool)
                }
                mouseEnter()?.subscribe {
                    cursorStyle(CursorStyle.HAND)
                }
            }
        )

        return value
    }

    override fun draw(graphics: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        super.draw(graphics, mouseX, mouseY, partialTicks, delta)
        if (FileExplorer.INSTANCE.focus == this) this.drawFocusHighlight(graphics, mouseX, mouseY, partialTicks, delta)
    }

    override fun onMouseDown(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (FileExplorer.INSTANCE.focus == this) {
            FileExplorer.INSTANCE.focus = null
        } else {
            FileExplorer.INSTANCE.focus = this
        }

        FileExplorer.INSTANCE.updateButtons()

        return super.onMouseDown(click, doubled)
    }
}