package llc.redstone.htslreborn.ui

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.exporting
import llc.redstone.htslreborn.HTSLReborn.exportingFile
import llc.redstone.htslreborn.HTSLReborn.importing
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.accessors.HandledScreenAccessor
import llc.redstone.htslreborn.ui.FileExplorerHandler.onSearchChanged
import llc.redstone.htslreborn.ui.FileHandler.baseDir
import llc.redstone.htslreborn.ui.FileHandler.filteredFiles
import llc.redstone.htslreborn.ui.FileHandler.htslExtensions
import llc.redstone.htslreborn.ui.FileHandler.itemExtensions
import llc.redstone.htslreborn.ui.FileHandler.refreshFiles
import llc.redstone.htslreborn.ui.FileHandler.search
import llc.redstone.htslreborn.ui.components.*
import llc.redstone.systemsapi.SystemsAPI
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

//? if >= 26.2 {
 /*import llc.redstone.systemsapi.screen
*///? }

class FileExplorer : BaseOwoScreen<FlowLayout>() {
    companion object {
        @JvmStatic
        var INSTANCE = FileExplorer()
        var isMinimized = false

        @JvmStatic
        fun inActionGui(): Boolean {
            if (importing) return true
            if (exporting) return true
            val screen = MC.screen as? ContainerScreen ?: return false
            val title = screen.title.string
            return title.contains(Regex(I18n.get("htslreborn.action.container.title")))
        }
    }

    init {
        if (HTSLReborn.CONFIG.isMinimizedByDefault) {
            isMinimized = true
        }
    }

    fun resetCursor() {
        uiAdapter?.cursorAdapter?.applyStyle(CursorStyle.NONE)
    }

    var focus: ExplorerEntryComponent? = null

    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    override fun onClose() {
        this.resetCursor()
        if (SystemsAPI.getHousingImporter().isImporting()) {
            SystemsAPI.getHousingImporter().cancelImport()
        }
        super.onClose()
    }

    private fun buildTitle(): UIComponent {
        return UIContainers.horizontalFlow(Sizing.fill(), Sizing.content()).apply {
            alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
            margins(Insets.of(2))

            child(
                UIComponents.button(Component.literal("-")) {
                    toggleMinimize()
                }.apply {
                    sizing(Sizing.fixed(16), Sizing.fixed(16))
                    setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.minimize")))
                }
            )

            child(
                UIComponents.label(Component.translatable("htslreborn.explorer.title")).apply {
                    sizing(Sizing.expand(), Sizing.content())
                    horizontalTextAlignment(HorizontalAlignment.CENTER)
                }
            )
        }
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        this.uiAdapter.rootComponent.clearChildren()

        this.build(this.uiAdapter.rootComponent)
    }

    private fun buildMinimizePopout(): UIComponent {
        return UIComponents.button(Component.literal("□")) {
            toggleMinimize()
        }.apply {
            sizing(Sizing.fixed(16), Sizing.fixed(16))
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.expand")))
            positioning(Positioning.absolute(7, 7));
        }
    }

    private val searchBox = UIComponents.textBox(Sizing.expand()).apply {
        verticalSizing(Sizing.fill())
        setHint(Component.translatable("htslreborn.explorer.search"))
    }

    override fun charTyped(input: CharacterEvent): Boolean {
        if (searchBox.isFocused) {
            return searchBox.charTyped(input).also {
                onSearchChanged(searchBox.value)
            }
        }
        return super.charTyped(input)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (input.key() == /* ESCAPE */ 256) {
            MC.screen?.onClose()
        }
        if (searchBox.isFocused) {
            if (input.key() == /* E */ 69) {
                return true
            }
            return searchBox.keyPressed(input).also {
                onSearchChanged(searchBox.value)
            }
        }
        return super.keyPressed(input)
    }

    private fun buildHeader(): FlowLayout {
        val openFolderButton = UIComponents.button(Component.literal("\uD83D\uDDC0")) {
            val dir = FileHandler.currentDir
            Util.getPlatform().openPath(dir)
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.create(Component.translatable("htslreborn.explorer.openfolder.description")))
        }

        return UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(20)).apply {
            gap(2)
            children(
                listOf(
                    searchBox,
                    openFolderButton
                )
            )
        }
    }

    fun explorerEntry(index: Int): FlowLayout {
        val file = filteredFiles.getOrNull(index)

        val entry = when {
            (file?.isDirectory() == true) ->
                FolderEntryComponent.create(Sizing.fill(), Sizing.fixed(25), index, file)

            (itemExtensions.contains(file?.extension)) ->
                ItemEntryComponent.create(Sizing.fill(), Sizing.fixed(25), index, file!!)

            (htslExtensions.contains(file?.extension)) ->
                ScriptEntryComponent.create(Sizing.fill(), Sizing.fixed(25), index, file!!)

            (index == -1) ->
                CreateEntryComponent.create(Sizing.fill(), Sizing.fixed(25))

            else -> throw IllegalStateException("Unknown file type: ${file?.toAbsolutePath()}")
        }

        return entry.apply {
            val icon = UIComponents.texture(this.icon, 0, 0, 16, 16, 16, 16)
            val label = UIComponents.label(
                if (file == null) {
                    Component.translatable("htslreborn.explorer.newfile").withStyle { it.withBold(true) }
                        .append(Component.literal("\"$search\"").withStyle { it.withBold(false) })
                } else {
                    Component.literal(file.nameWithoutExtension).apply {
                        if (file.isDirectory()) return@apply
                        append(Component.literal(".${file.extension}").withColor(0x808080))
                    }
                }
            )

            surface(Surface.DARK_PANEL)
            gap(4)
            padding(Insets.of(4).withLeft(5).withRight(5))
            alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER)

            children(
                listOf(
                    icon,
                    label
                )
            )
        }
    }

    val content: FlowLayout = UIContainers.verticalFlow(Sizing.fill(), Sizing.content()).apply {
        gap(1)
        margins(Insets.right(6))

        children(
            MutableList(filteredFiles.size) { index ->
                explorerEntry(index)
            }.also {
                if (search.isNotEmpty() && filteredFiles.find { it.nameWithoutExtension == search } == null) {
                    it.add(0, explorerEntry(-1))
                }
            }
        )
    }

    fun updateButtons() {
        val buttons = this.uiAdapter.rootComponent
            .childById(FlowLayout::class.java, "base")
            .childById(FlowLayout::class.java, "buttons")
        buttons.queue {
            buttons.clearChildren()
            if (focus == null) {
                buttons.verticalSizing(Sizing.fixed(0))
                return@queue
            }
            buttons.verticalSizing(Sizing.fixed(20))
            buttons.children(focus!!.buildContextButtons())
        }
    }

    fun refreshExplorer(queue: Boolean = false) {
        if (queue) {
            uiAdapter.rootComponent.queue { refreshExplorer(false) }
            return
        }
        content.clearChildren()
        content.children(
            MutableList(filteredFiles.size) { index ->
                explorerEntry(index)
            }.also {
                if (search.isNotEmpty() && filteredFiles.find { it.nameWithoutExtension == search } == null) {
                    it.add(0, explorerEntry(-1))
                }
            }
        )
    }

    private fun buildExplorer(): ScrollContainer<FlowLayout> {
        refreshExplorer()
        return UIContainers.verticalScroll(Sizing.expand(), Sizing.expand(), content).apply {
            surface(Surface.VANILLA_TRANSLUCENT)
            margins(Insets.vertical(5))
            padding(Insets.of(2))
            scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            scrollbarThiccness(4)
        }
    }

    private fun buildContextButtons(): FlowLayout {
        return UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(0)).apply {
            id("buttons")
            gap(2)
        }
    }

    fun breadcrumb(name: String, index: Int): LabelComponent {
        return UIComponents.label(Component.literal(name)).apply {
            cursorStyle(CursorStyle.HAND)
            mouseEnter().subscribe {
                text(Component.literal(name).withColor(0x808080));
            }
            mouseLeave().subscribe {
                text(Component.literal(name))
            }
            mouseDown().subscribe { _, _ ->
                FileExplorerHandler.onBreadcrumbClicked(index)
                false
            }
            focusLost().subscribe {
                text(Component.literal(name))
            }
        }
    }

    val breadcrumbs: FlowLayout = UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(20)).apply {
        gap(4)
        alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
    }

    fun refreshBreadcrumbs() {
        val subDir = FileHandler.currentDir

        val update = {
            breadcrumbs.clearChildren()
            val names = (baseDir.nameCount - 1 until subDir.nameCount).map { subDir.getName(it).toString() }
            names.forEachIndexed { index, name ->
                if (index > 0) breadcrumbs.child(UIComponents.label(Component.literal(">").withColor(0x505050)))
                breadcrumbs.child(breadcrumb(name, index))
            }
        }

        if (breadcrumbs.hasParent()) uiAdapter.rootComponent.queue { update() } else update()
    }

    fun buildWorkingScreen(display: Component, type: WorkingScreenType): FlowLayout {
        val accessor =
            (MC.screen as? HandledScreenAccessor) ?: throw IllegalStateException("Could not get accessor")
        val label = UIComponents.label(display)
        val timeRemaining = TimeRemainingComponent(Sizing.expand(), Sizing.content())
        val cancelButton = UIComponents.button(Component.translatable("htslreborn.importing.working.cancel")) {
            SystemsAPI.getHousingImporter().cancelImport()
            hideWorkingScreen()
        }
        return UIContainers.verticalFlow(Sizing.fill(), Sizing.fill()).apply {
            id("importScreen")
            surface(Surface.flat(0xcc000000.toInt()).and(Surface.blur(6f, 6f)))
            horizontalAlignment(HorizontalAlignment.CENTER)
            verticalAlignment(VerticalAlignment.CENTER)
            gap(5)
            sizing(
                Sizing.fixed((accessor.getGuiLeft() * HTSLReborn.CONFIG.widthScale).toInt() - 10),
                Sizing.fixed((MC.window.guiScaledHeight * HTSLReborn.CONFIG.heightScale).toInt() - 10)
            )

            mouseDown().subscribe { click, bool ->
                true
            }

            children(
                listOf(
                    label,
                    timeRemaining,
                    cancelButton
                )
            )
        }
    }

    enum class WorkingScreenType {
        IMPORT, EXPORT
    }

    fun showWorkingScreen(type: WorkingScreenType, fileName: String) {
        val importScreen = this.uiAdapter.rootComponent.childById(FlowLayout::class.java, "importScreen")
        if (importScreen != null) return

        val display = Component.translatable(
            when (type) {
                WorkingScreenType.IMPORT -> "htslreborn.importing.working.type.import"
                WorkingScreenType.EXPORT -> "htslreborn.importing.working.type.export"
            }, fileName
        )
        val workingScreen = buildWorkingScreen(display, type)
        this.uiAdapter.rootComponent.child(workingScreen)
    }

    fun hideWorkingScreen() {
        val importScreen = this.uiAdapter.rootComponent.childById(FlowLayout::class.java, "importScreen")
        if (importScreen != null) this.uiAdapter.rootComponent.removeChild(importScreen)
    }

    var base: FlowLayout = UIContainers.verticalFlow(Sizing.fill(), Sizing.fill())

    public override fun build(root: FlowLayout) {
        val accessor =
            (MC.screen as? HandledScreenAccessor) ?: throw IllegalStateException("Could not get accessor")

        //Needs to be recreated for minimize toggle to work
        base = UIContainers.verticalFlow(Sizing.fill(), Sizing.fill())

        root.apply {
            sizing(Sizing.fixed(accessor.getGuiLeft()), Sizing.fixed(MC.window.guiScaledHeight))
            padding(Insets.of(5))
            verticalAlignment(VerticalAlignment.CENTER)
            horizontalAlignment(HorizontalAlignment.RIGHT)
            gap(((MC.window.guiScaledHeight * HTSLReborn.CONFIG.heightScale).toInt() - 10) * -1)

            if (isMinimized) {
                child(buildMinimizePopout())
            } else {
                refreshFiles(true)
                refreshBreadcrumbs()

                child(
                    base.apply {
                        sizing(
                            Sizing.fixed((accessor.getGuiLeft() * HTSLReborn.CONFIG.widthScale).toInt() - 10),
                            Sizing.fixed((MC.window.guiScaledHeight * HTSLReborn.CONFIG.heightScale).toInt() - 10)
                        )

                        id("base")
                        surface(Surface.DARK_PANEL)
                        padding(Insets.of(5))

                        clearChildren()
                        children(
                            listOf(
                                buildTitle(),
                                buildHeader(),
                                buildExplorer(),
                                buildContextButtons(),
                                breadcrumbs
                            )
                        )
                    }
                )
            }

            if (importing) {
                showWorkingScreen(WorkingScreenType.IMPORT, importingFile!!.name)
            } else if (exporting) {
                showWorkingScreen(WorkingScreenType.EXPORT, exportingFile!!.name)
            }

        }
    }
}