package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.utils.ItemUtils.giveItem
import llc.redstone.htslreborn.utils.ItemUtils.saveItem
import llc.redstone.htslreborn.utils.UIErrorToast
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import java.nio.file.Path
import kotlin.io.path.deleteExisting

class ItemEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int, val path: Path
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/file_explorer/item_icon.png")

    override fun buildContextButtons(): List<UIComponent> {
        val give = UIComponents.button(Component.translatable("htslreborn.explorer.button.item.give")) {
            handleItemClick()
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.item.give.description")))
        }

        val save = UIComponents.button(Component.translatable("htslreborn.explorer.button.item.save")) {
            try {
                MC.player?.saveItem(path)
            } catch (e: IllegalStateException) {
                UIErrorToast.report(e.message)
            }
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.item.save.description")))
        }

        val spacer = UIComponents.spacer()

        val open = UIComponents.button(Component.literal("✎")) {
            Util.getPlatform().openPath(path)
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.item.open.description")))
        }

        val delete = UIComponents.button(Component.literal("\uD83D\uDDD1")) {
            DeleteConfirmationComponent.handleDeleteClick()
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.item.delete.description")))
        }

        return listOf(
            give,
            save,
            spacer,
            open,
            delete
        )
    }

    fun handleItemClick() = try {
        MC.player?.giveItem(path)
    } catch (e: IllegalStateException) {
        UIErrorToast.report(e.message)
    }

    override fun onMouseDown(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (doubled) {
            handleItemClick()
            return true
        }
        return super.onMouseDown(click, false)
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int, path: Path): ExplorerEntryComponent {
            return ItemEntryComponent(horizontalSizing, verticalSizing, index, path)
        }
    }

}