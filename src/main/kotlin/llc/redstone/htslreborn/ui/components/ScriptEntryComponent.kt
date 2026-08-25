package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import llc.redstone.htslreborn.HTSLReborn.CONFIG
import llc.redstone.htslreborn.HTSLReborn.exportingFile
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.config.HtslConfigModel
import llc.redstone.htslreborn.htslio.HTSLExporter
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.systemsapi.importer.ActionContainer
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.name

class ScriptEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int, val path: Path
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/file_explorer/script_icon.png")
    override fun buildContextButtons(): List<UIComponent> {
        val import = UIContainers.horizontalFlow(Sizing.content(), Sizing.fill()).apply {
            children(
                listOf(
                    UIComponents.button(
                        Component.translatable("htslreborn.explorer.button.script.import")
                    ) {
                        handleScriptClick()
                    }.apply {
                        val tooltipKey = when (CONFIG.defaultImportStrategy) {
                            HtslConfigModel.ImportStrategy.APPEND -> "htslreborn.explorer.button.script.import.append.description"
                            HtslConfigModel.ImportStrategy.REPLACE -> "htslreborn.explorer.button.script.import.replace.description"
//                            HtslConfigModel.ImportStrategy.UPDATE -> "htslreborn.explorer.button.script.import.update.description"
                        }
                        setTooltip(Tooltip.create(Component.translatable(tooltipKey)))
                    },
                    UIComponents.button(Component.literal("↓")) {
                        val base = FileExplorer.INSTANCE.base
                        val dropdown = base.childById(DropdownComponent::class.java, "importDropdown")
                        if (dropdown == null)  {
                            base.queue { base.child(DropdownComponent(Sizing.fixed(50), Sizing.content())) }
                        } else {
                            base.queue { base.removeChild(dropdown) }
                        }
                    }
                ))
        }

        val export = UIComponents.button(Component.translatable("htslreborn.explorer.button.script.export")) {
            FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.EXPORT, path.name)
            exportingFile = path
            HTSLExporter.exportFile(path) {
                FileExplorer.INSTANCE.hideWorkingScreen()
            }
        }.apply {
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.script.export.description")))
        }

        val spacer = UIComponents.spacer()

        val open = UIComponents.button(Component.literal("✎")) {
            Util.getPlatform().openPath(path)
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.script.open.description")))
        }

        val delete = UIComponents.button(Component.literal("\uD83D\uDDD1")) {
            DeleteConfirmationComponent.handleDeleteClick()
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.button.script.delete.description")))
        }

        return listOf(
            import,
            export,
            spacer,
            open,
            delete
        )
    }

    fun handleScriptClick() {
        val method = when (CONFIG.defaultImportStrategy) {
            HtslConfigModel.ImportStrategy.APPEND -> ActionContainer::addActions
            HtslConfigModel.ImportStrategy.REPLACE -> ActionContainer::setActions
//            HtslConfigModel.ImportStrategy.UPDATE -> ActionContainer::updateActions
        }

        importingFile = path
        FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.IMPORT, path.name)
        HTSLImporter.importFile(path, method) {
            FileExplorer.INSTANCE.hideWorkingScreen()
        }
    }

    override fun onMouseDown(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (doubled) {
            handleScriptClick()
            return true
        }
        return super.onMouseDown(click, false)
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int, path: Path): ExplorerEntryComponent {
            return ScriptEntryComponent(horizontalSizing, verticalSizing, index, path)
        }
    }

}