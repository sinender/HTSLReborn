package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import kotlinx.io.files.Path
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.exportingFile
import llc.redstone.htslreborn.htslio.HTSLExporter
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.ui.FileHandler.search
import llc.redstone.htslreborn.utils.ItemUtils.saveItem
import llc.redstone.htslreborn.utils.UIErrorToast
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name

class CreateEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/file_explorer/create_icon.png")

    @OptIn(ExperimentalPathApi::class)
    override fun buildContextButtons(): List<UIComponent> {
        val exportScript = UIComponents.button(Component.translatable("htslreborn.explorer.button.create.script")) {
            handleScriptExport()
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.create.script.description")))
        }

        val exportItem = UIComponents.button(Component.translatable("htslreborn.explorer.button.create.item")) {
            handleItemExport()
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.create.item.description")))
        }

        val createFolder = UIComponents.button(Component.translatable("htslreborn.explorer.button.create.folder")) {
            handleFolderCreate()
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.create.folder.description")))
        }

        return listOf(
            exportScript,
            exportItem,
            UIComponents.spacer(),
            createFolder
        )
    }

    fun handleScriptExport() {
        val path = FileHandler.currentDir.resolve("$search.htsl")
        FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.EXPORT, path.name)
        exportingFile = path
        HTSLExporter.exportFile(path) {
            FileExplorer.INSTANCE.hideWorkingScreen()
            search = ""
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        }
    }

    fun handleItemExport() {
        val path = FileHandler.currentDir.resolve("$search.nbt")
        MC.player?.saveItem(path)
        FileHandler.refreshFiles()
        FileExplorer.INSTANCE.refreshExplorer()
    }

    fun handleFolderCreate() {
        val path = FileHandler.currentDir.resolve(search)
        if (!path.exists()) {
            path.createDirectories()
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        } else {
            UIErrorToast.report("${path.name} already exists")
        }
    }

    override fun onMouseDown(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (doubled) {
            handleScriptExport()
            return true
        }
        return super.onMouseDown(click, false)
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing): ExplorerEntryComponent {
            return CreateEntryComponent(horizontalSizing, verticalSizing, -1)
        }
    }

}