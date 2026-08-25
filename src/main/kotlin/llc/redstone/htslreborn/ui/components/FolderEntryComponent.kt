package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileExplorerHandler.setWatchedDir
import llc.redstone.htslreborn.ui.FileHandler
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class FolderEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int, val path: Path
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/file_explorer/folder_icon.png")

    @OptIn(ExperimentalPathApi::class)
    override fun buildContextButtons(): List<UIComponent> {
        val open = UIComponents.button(Component.translatable("htslreborn.explorer.button.folder.open")) {
            handleFolderClick()
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.folder.open.description")))
        }

        val spacer = UIComponents.spacer()

        val openExternal = UIComponents.button(Component.literal("✎")) {
            Util.getPlatform().openPath(path)
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.folder.openext.description")))
        }

        val delete = UIComponents.button(Component.literal("\uD83D\uDDD1")) {
            DeleteConfirmationComponent.handleDeleteClick()
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.folder.delete.description")))
        }

        return listOf(
            open,
            spacer,
            openExternal,
            delete
        )
    }

    fun handleFolderClick() {
        FileHandler.currentDir = path
        FileHandler.refreshFiles()
        FileExplorer.INSTANCE.refreshExplorer()
        FileExplorer.INSTANCE.refreshBreadcrumbs()
        setWatchedDir(FileHandler.currentDir)
    }

    override fun onMouseDown(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (doubled) {
            handleFolderClick()
            return true
        }
        return super.onMouseDown(click, false)
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int, path: Path): ExplorerEntryComponent {
            return FolderEntryComponent(horizontalSizing, verticalSizing, index, path)
        }
    }

}