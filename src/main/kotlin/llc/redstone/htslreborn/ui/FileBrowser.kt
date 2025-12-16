package llc.redstone.htslreborn.ui

import com.github.shynixn.mccoroutine.fabric.launch
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.MOD_ID
import llc.redstone.htslreborn.HTSLReborn.importing
import llc.redstone.htslreborn.accessors.HandledScreenAccessor
import llc.redstone.htslreborn.htslio.HTSLExporter
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.utils.ItemConvertUtils
import llc.redstone.htslreborn.utils.RenderUtils
import llc.redstone.htslreborn.utils.RenderUtils.drawText
import llc.redstone.htslreborn.utils.RenderUtils.drawTexture
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsapi.util.ItemUtils
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.StringNbtReader
import net.minecraft.text.Text.literal
import net.minecraft.util.Identifier
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.math.floor

object FileBrowser {
    private var files = mutableListOf<String>()
    private var filteredFiles = mutableListOf<String>()
    private var page = 0
    private var linesPerPage = 0
    private val cachedItems = mutableMapOf<String, ItemStack?>()
    private var hoveringIndex = -1
    private var subDir = ""

    // Buttons
    private val import = ButtonWidget.builder(literal("Import HTSL")) {}
        .dimensions(0, 0, 0, 20).build()
    private val export = ButtonWidget.builder(literal("Export HTSL")) {}
        .dimensions(0, 0, 0, 20).build()
    private val refreshFiles = ButtonWidget.builder(literal("⟳")) {}
        .dimensions(0, 0, 0, 20).build()
    private val backDir = ButtonWidget.builder(literal("⇪")) {}
        .dimensions(0, 0, 0, 20).build()
    private val forwardPage = ButtonWidget.builder(literal("⇨")) {}
        .dimensions(0, 0, 15, 20).build()
    private val backwardPage = ButtonWidget.builder(literal("⇦")) {}
        .dimensions(0, 0, 15, 20).build()

    // Assets
    private val htslIcon = Identifier.of(MOD_ID, "sprites/icon/htsl.png")
    private val itemIcon = Identifier.of(MOD_ID, "sprites/icon/item.png")
    private val folderIcon = Identifier.of(MOD_ID, "sprites/icon/folder.png")
    private val trashBin = Identifier.of(MOD_ID, "sprites/icon/bin_closed.png")
    private val openTrashBin = Identifier.of(MOD_ID, "sprites/icon/bin.png")

    val input = TextFieldWidget(MC.textRenderer, 0, 18, literal("Enter File Name")).also {
        it.setMaxLength(2000)
        it.text = "Enter File Name"
    }

    private fun getFileIcon(fileName: String, file: File): Identifier? = when {
        file.isDirectory -> folderIcon
        fileName.endsWith(".htsl", true) -> htslIcon
        fileName.endsWith(".nbt", true) -> itemIcon
        else -> null
    }

    private fun getItemForFile(fileDir: String, file: File): ItemStack? {
        return cachedItems.getOrPut(fileDir) {
            try {
                ItemConvertUtils.fileToItemStack(file)
            } catch (e: Exception) {
                return null
            }
        }
    }

    private fun renderFileIcon(ctx: DrawContext, fileName: String, fileDir: String, file: File, x: Int, y: Int, size: Int) {
        if (fileName.endsWith(".nbt", true)) {
            val item = getItemForFile(fileDir, file)
            if (item != null) {
                ctx.drawItem(item, x + 2, y + 1)
            } else {
                ctx.drawTexture(itemIcon, x, y, size, size)
            }
        } else {
            getFileIcon(fileName, file)?.let { icon ->
                ctx.drawTexture(icon, x, y, size, size)
            }
        }
    }

    fun renderFileBrowser(ctx: DrawContext, x: Int, y: Int, delta: Float) {
        if (!inActionGui()) return
        val gui = MC.currentScreen as? HandledScreenAccessor ?: return
        val screenWidth = MC.window.scaledWidth
        val screenHeight = MC.window.scaledHeight
        val chestWidth = gui.getXSize()
        val chestX = (screenWidth - chestWidth) / 2
        val topBound = input.y + 30
        val xBound = input.x + input.getWidth()

        updateWidgetPositions(chestX, screenWidth, screenHeight, xBound)
        ctx.fill(input.x - 5, topBound, input.x - 5 + input.width + 10, topBound + screenHeight / 7 * 6 - topBound,
            RenderUtils.getColor(30, 30, 30, 200))

        linesPerPage = floor((screenHeight / 7 * 6 - topBound - 9) / 20.0).toInt()
        var hovered = false
        for (i in (page * linesPerPage) until filteredFiles.size.coerceAtMost((page + 1) * linesPerPage)) {
            val fileName = filteredFiles[i]
            val fileDir = subDir.replace("\\", "/") + fileName
            val file = File("HTSL/imports", fileDir)
            val yPos = topBound + 20 * (i - page * linesPerPage)
            val isHovering = y in yPos until (yPos + 20) && x in input.x until xBound

            if (isHovering) {
                hovered = true
                hoveringIndex = i
                renderHoveredFile(ctx, fileName, fileDir, file, x, xBound, yPos)
            } else {
                renderFileIcon(ctx, fileName, fileDir, file, input.x - 2, yPos + 3, 16)
                ctx.drawText(fileName, input.x + 21, yPos + 9)
            }
        }

        if (!hovered) hoveringIndex = -1
        renderNavigationControls(ctx, x, y, delta, chestX, topBound)
        input.render(ctx, x, y, delta)
        import.render(ctx, x, y, delta)
        export.render(ctx, x, y, delta)
    }

    fun input(keyInput: KeyInput): Boolean {
        if (!inActionGui()) return false
        if (input.isFocused) {
            if (keyInput.isEscape) {
                return false
            }

            if (!input.keyPressed(keyInput)) {
                input.charTyped(CharInput(keyInput.key, keyInput.modifiers))
            }

            if (input.text != "Enter File Name") {
                val query = input.text.lowercase()
                filteredFiles = files.filter { it.lowercase().contains(query) }.toMutableList()
                page = 0
            } else {
                filteredFiles = files.toMutableList()
                page = 0
            }
            return true
        }

        return false
    }

    fun inActionGui(): Boolean {
        val screen = MC.currentScreen as? GenericContainerScreen ?: return false
        if (!screen.title.string.contains("Actions", true)) return false
        if (importing) return false
        return true
    }

    fun onOpen() {
        if (!inActionGui()) return
        subDir = ""
        refreshFiles()
    }

    fun click(click: Click, doubled: Boolean): Boolean {
        if (!inActionGui()) return false
        val x = click.x
        val y = click.y
        input.mouseClicked(click, doubled)
        if (x > input.x && x < input.x + input.getWidth() && y > input.y && y < input.y + input.getHeight()) {
            if (input.text == "Enter File Name") {
                input.text = ""
            }
            input.isFocused = true
        } else {
            input.isFocused = false
        }

        if (refreshFiles.isMouseOver(x, y)) {
            refreshFiles()
        }

        if (subDir.isNotEmpty() && backDir.isMouseOver(x, y)) {
            val lastSeparator = subDir.dropLast(1).lastIndexOf('/')
            subDir = if (lastSeparator != -1) {
                subDir.substring(0, lastSeparator + 1)
            } else {
                ""
            }
            refreshFiles()
        }

        if (forwardPage.isMouseOver(x, y) && (page + 1) * linesPerPage < filteredFiles.size) {
            page++
        }

        if (backwardPage.isMouseOver(x, y) && page > 0) {
            page--
        }

        if (import.isMouseOver(x, y)) {
            val fileName = input.text
            var fileDir = subDir.replace("\\", "/") + fileName
            if (!fileDir.endsWith(".htsl", true)) {
                fileDir += ".htsl"
            }
            val file = File("HTSL/imports", fileDir)
            if (!file.exists()) {
                HTSLReborn.LOGGER.error("File $fileDir does not exist.")
                return false
            }

            HTSLImporter.importFile(file)
        }

        if (export.isMouseOver(x, y)) {
            val fileName = input.text
            var fileDir = subDir.replace("\\", "/") + fileName
            if (!fileDir.endsWith(".htsl", true)) {
                fileDir += ".htsl"
            }
            val file = File("HTSL/imports", fileDir)
            HTSLReborn.launch {
                val actions = SystemsAPI.getHousingImporter().getOpenActionContainer()?.getActions() ?: return@launch
                val htslCode = HTSLExporter.export(actions)
                file.parentFile?.let {
                    if (!it.exists()) {
                        it.mkdirs()
                    }
                }
                file.writeText(htslCode.joinToString("\n"))
            }
        }

        clickHoveredFile(click)
        return false
    }

    fun clickHoveredFile(click: Click): Boolean {
        if (!inActionGui()) return false
        val index = hoveringIndex
        if (index == -1) return false
        val fileName = filteredFiles[index]
        val fileDir = subDir.replace("\\", "/") + fileName
        val file = File("HTSL/imports", fileDir)

        if (click.x.toInt() in (input.x + input.width - 24) until (input.x + input.width - 8) && click.y.toInt() in (input.y + 30 + 20 * (index - page * linesPerPage)) until (input.y + 30 + 20 * (index - page * linesPerPage) + 20) && fileName.contains(".")) {
            // Delete file
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            refreshFiles()
            return true
        }

        if (file.isDirectory) {
            subDir += "$fileName/"
            refreshFiles()
        } else {
            if (fileName.endsWith(".nbt", true)) {
                val item = getItemForFile(fileDir, file) ?: return true
                ItemUtils.placeInventory(item, MC.player?.inventory?.indexOfFirst { it.isEmpty } ?: 0)
            } else if (fileName.endsWith(".htsl", true)) {
                input.text = fileName.dropLast(5)
                filteredFiles = files.filter { it.lowercase().contains(input.text.lowercase()) }.toMutableList()
                page = 0

                HTSLImporter.importFile(file)
            }
        }
        return true
    }

    private fun refreshFiles() {
        val baseDir = File("HTSL/imports", subDir)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        files = baseDir.listFiles().filter {
            if (it.isDirectory) {
                true
            } else {
                it.name.endsWith(".htsl", true) || it.name.endsWith(".nbt", true)
            }
        }.map { it.name }.toMutableList()
        files.sortWith(compareBy({ !File(baseDir, it).isDirectory }, { it.lowercase() }))
        filteredFiles = files.toMutableList()
        page = 0
        cachedItems.clear()

        if (input.text != "Enter File Name") {
            val query = input.text.lowercase()
            filteredFiles = files.filter { it.lowercase().contains(query) }.toMutableList()
        }
    }

    private fun updateWidgetPositions(chestX: Int, screenWidth: Int, screenHeight: Int, xBound: Int) {
        input.y = screenHeight / 7 - 20
        input.setWidth(chestX * 6 / 7)
        input.x = chestX / 2 - input.width / 2

        import.y = input.y - 25;
        export.y = input.y - 25;
        import.x = input.x;
        import.setWidth(input.getWidth() / 2);
        export.x = input.x + input.getWidth() / 2;
        export.setWidth(import.getWidth());

        val btnWidth = (chestX - xBound - 10).coerceAtLeast(10)
        refreshFiles.setWidth(btnWidth)
        refreshFiles.x = (chestX - xBound) / 2 + xBound - refreshFiles.width / 2
        refreshFiles.y = input.y
        backDir.setWidth(btnWidth)
        backDir.x = refreshFiles.x
        backDir.y = input.y - 25
        forwardPage.y = screenHeight / 7 * 6 + 2
        forwardPage.x = input.width + input.x - 5
        backwardPage.y = screenHeight / 7 * 6 + 2
        backwardPage.x = input.x - 5
    }

    private fun renderHoveredFile(ctx: DrawContext, fileName: String, fileDir: String, file: File, x: Int, xBound: Int, yPos: Int) {
        ctx.fill(input.x - 3, yPos + 2, input.x - 3 + input.width + 6, yPos + 2 + 21, RenderUtils.getColor(60, 60, 60, 200))
        if (fileName.contains(".")) {
            val trashIcon = if (x in (xBound - 24) until (xBound - 8)) openTrashBin else trashBin
            ctx.drawTexture(trashIcon, xBound - 24, yPos + 3, 20, 20)
        }
        renderFileIcon(ctx, fileName, fileDir, file, input.x - 2, yPos + 3, 20)
        ctx.drawText(subDir + fileName, input.x + 22, yPos + 8)
    }

    private fun renderNavigationControls(ctx: DrawContext, x: Int, y: Int, delta: Float, chestX: Int, topBound: Int) {
        if (filteredFiles.isEmpty()) {
            ctx.drawText( "Nothing is here...", input.x + 10, topBound + 9)
        }
        if (subDir.isNotEmpty()) {
            backDir.render(ctx, x, y, delta)
            ctx.drawText("/$subDir", chestX / 2 - MC.textRenderer.getWidth("/$subDir") / 2, topBound - 10)
        }
        if ((page + 1) * linesPerPage < filteredFiles.size) forwardPage.render(ctx, x, y, delta)
        if (page > 0) backwardPage.render(ctx, x, y, delta)
        refreshFiles.render(ctx, x, y, delta)
    }
}
