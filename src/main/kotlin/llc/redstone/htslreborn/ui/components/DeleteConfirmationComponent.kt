package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.OverlayContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import llc.redstone.htslreborn.HTSLReborn.CONFIG
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileHandler
import net.minecraft.network.chat.Component
import kotlin.io.path.*

class DeleteConfirmationComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing,
) : OverlayContainer<FlowLayout>(UIContainers.verticalFlow(horizontalSizing, verticalSizing)) {

    init {
        id("deleteConfirmation")
        child().apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            surface(Surface.DARK_PANEL)
            padding(Insets.of(10))
            children(
                listOf(
                    UIComponents.label(Component.translatable("htslreborn.explorer.confirm.delete.title")),
                    UIComponents.spacer(2),
                    UIComponents.label(Component.translatable("htslreborn.explorer.confirm.delete.message").withColor(0x7E7E7E)),
                    UIComponents.label(Component.literal(fileName() + "?").withColor(0x7E7E7E)),
                    UIComponents.spacer(2),
                    UIContainers.horizontalFlow(Sizing.fill(), Sizing.content()).apply {
                        horizontalAlignment(HorizontalAlignment.CENTER)
                        gap(4)
                        children(
                            listOf(
                                UIComponents.button(Component.translatable("htslreborn.explorer.confirm.delete.button.cancel")) {
                                    click(it)
                                }.apply {
                                    id("cancel")
                                    horizontalSizing(Sizing.expand(40))
                                    horizontalAlignment(HorizontalAlignment.CENTER)
                                },
                                UIComponents.button(Component.translatable("htslreborn.explorer.confirm.delete.button.confirm")) {
                                    click(it)
                                }.apply {
                                    id("confirm")
                                    horizontalSizing(Sizing.expand(40))
                                    horizontalAlignment(HorizontalAlignment.CENTER)
                                }
                            )
                        )
                    }
                )
            )
        }
    }

    @OptIn(ExperimentalPathApi::class)
    companion object {
        private fun fileName(): String? {
            val file = FileExplorer.INSTANCE.focus ?: return null
            return (file as? ScriptEntryComponent)?.path?.name
                ?: (file as? FolderEntryComponent)?.path?.name
                ?: (file as? ItemEntryComponent)?.path?.name
        }

        fun handleDeleteClick() {
            if (CONFIG.fileDeleteConfirmation) {
                toggleDeleteConfirmation()
            } else {
                deleteFocusedFile()
            }
        }

        private fun toggleDeleteConfirmation() {
            val base = FileExplorer.INSTANCE.base
            val dropdown = base.childById(DeleteConfirmationComponent::class.java, "deleteConfirmation")
            base.queue {
                if (dropdown == null) {
                    base.child(DeleteConfirmationComponent(Sizing.fixed(200), Sizing.content()))
                } else {
                    base.removeChild(dropdown)
                }
            }
        }

        private fun deleteFocusedFile() {
            val file = FileExplorer.INSTANCE.focus
            val path = (file as? ScriptEntryComponent)?.path
                ?: (file as? FolderEntryComponent)?.path
                ?: (file as? ItemEntryComponent)?.path

            path?.let {
                if (it.isDirectory()) {
                    it.deleteRecursively()
                } else {
                    it.deleteExisting()
                }
                FileHandler.refreshFiles()
                FileExplorer.INSTANCE.refreshExplorer()
            }
        }

        fun click(button: ButtonComponent) {
            if (button.id() == "cancel") {
                val base = FileExplorer.INSTANCE.base
                val dropdown = base.childById(DeleteConfirmationComponent::class.java, "deleteConfirmation")
                base.queue { base.removeChild(dropdown) }
                return
            }
            if (button.id() == "confirm") {
                deleteFocusedFile()
                val base = FileExplorer.INSTANCE.base
                val dropdown = base.childById(DeleteConfirmationComponent::class.java, "deleteConfirmation")
                base.queue {
                    base.removeChild(dropdown)
                }
            }
        }
    }
}