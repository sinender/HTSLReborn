package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.MOD_ID
import llc.redstone.htslreborn.accessors.HandledScreenAccessor
import llc.redstone.systemsapi.util.ItemUtils
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.text.Text.literal
import net.minecraft.util.Identifier
import java.io.File
import kotlin.math.floor

object FileBrowser {
    private var show = false
    private var files = mutableListOf<String>()
    private var filteredFiles = mutableListOf<String>()
    private var page = 0
    private var linesPerPage = 0
    private val cachedItems = mutableMapOf<String, ItemStack?>()
    private var hoveringIndex = -1
    private var subDir = ""

    // Buttons
    private val refreshFiles = ButtonWidget.builder(literal("⟳")) {}
        .dimensions(0, 0, 20, 20).build()
    private val backDir = ButtonWidget.builder(literal("⇪")) {}
        .dimensions(0, 0, 20, 20).build()
    private val forwardPage = ButtonWidget.builder(literal("⇨")) {}
        .dimensions(0, 0, 20, 20).build()
    private val backwardPage = ButtonWidget.builder(literal("⇦")) {}
        .dimensions(0, 0, 20, 20).build()
    private val toggleShow = ButtonWidget.builder(literal("⇩")) {}
        .dimensions(0, 0, 20, 20).build()

    // Assets
    private val htslIcon = Identifier.of(MOD_ID, "textures/icon/htsl.png")
    private val itemIcon = Identifier.of(MOD_ID, "textures/icon/item.png")
    private val folderIcon = Identifier.of(MOD_ID, "textures/icon/folder.png")
    private val nhItemIcon = Identifier.of(MOD_ID, "textures/icon/nh_item.png")
    private val trashBin = Identifier.of(MOD_ID, "textures/icon/bin_closed.png")
    private val openTrashBin = Identifier.of(MOD_ID, "textures/icon/bin_open.png")

    val input = TextFieldWidget(MC.textRenderer, 0, 0, literal("Enter File Name")).apply {
        setMaxLength(2000)
        setEditable(false)
        text = "Enter File Name"
    }

    private fun getFileIcon(fileName: String): Identifier? = when {
        !fileName.contains(".") -> folderIcon
        fileName.endsWith(".htsl", true) -> htslIcon
        fileName.endsWith(".nbt", true) -> itemIcon
        else -> null
    }

    private fun getItemForFile(fileDir: String, file: File): ItemStack? {
        return cachedItems.getOrPut(fileDir) {
            try {
                ItemUtils.createFromNBT(StringNbtReader.readCompound(file.readText(Charsets.UTF_8)))
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun renderFileIcon(ctx: DrawContext, fileName: String, fileDir: String, file: File, x: Int, y: Int, size: Int) {
        if (fileName.endsWith(".nbt", true)) {
            val item = getItemForFile(fileDir, file)
            if (item != null) {
                ctx.drawItem(item, x + 2, y + 1)
            } else {
                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, nhItemIcon, x, y, size, size)
            }
        } else {
            getFileIcon(fileName)?.let { icon ->
                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, size, size)
            }
        }
    }

    fun renderFileBrowser(ctx: DrawContext, x: Int, y: Int, delta: Float) {
        val gui = MC.currentScreen as? HandledScreenAccessor ?: return
        val screenWidth = MC.window.scaledWidth
        val screenHeight = MC.window.scaledHeight
        val chestWidth = gui.getXSize()
        val chestX = (screenWidth - chestWidth) / 2
        val topBound = 30
        val xBound = 18

        updateWidgetPositions(chestX, screenWidth, screenHeight, xBound)
        ctx.drawStrokedRectangle(input.x - 5, topBound, input.width + 10, screenHeight / 7 * 6 - topBound, 0x1E1E1EC8)

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
                ctx.drawText(MC.textRenderer, fileName, input.x + 21, yPos + 9, 0xFFFFFF, true)
            }
        }

        if (!hovered) hoveringIndex = -1
        renderNavigationControls(ctx, x, y, delta, chestX, topBound)
    }

    private fun updateWidgetPositions(chestX: Int, screenWidth: Int, screenHeight: Int, xBound: Int) {
        input.y = screenHeight / 7 - 20
        input.setWidth(chestX * 6 / 7)
        input.x = chestX / 2 - input.width / 2

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
        ctx.drawStrokedRectangle(input.x - 3, yPos + 2, input.width + 6, 21, 0x3C3C3CC8)
        if (fileName.contains(".")) {
            val trashIcon = if (x in (xBound - 24) until (xBound - 8)) openTrashBin else trashBin
            ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, trashIcon, xBound - 24, yPos + 3, 20, 20)
        }
        renderFileIcon(ctx, fileName, fileDir, file, input.x - 2, yPos + 3, 20)
        ctx.drawText(MC.textRenderer, fileName, input.x + 22, yPos + 8, 0xFFFFFF, true)
    }

    private fun renderNavigationControls(ctx: DrawContext, x: Int, y: Int, delta: Float, chestX: Int, topBound: Int) {
        if (filteredFiles.isEmpty()) {
            ctx.drawText(MC.textRenderer, "Nothing is here...", input.x + 10, topBound + 9, 0xFFFFFF, true)
        }
        if (subDir.isNotEmpty()) {
            backDir.render(ctx, x, y, delta)
            ctx.drawText(MC.textRenderer, "/$subDir", chestX / 2 - MC.textRenderer.getWidth("/$subDir") / 2, topBound - 10, 0xFFFFFF, true)
        }
        if ((page + 1) * linesPerPage < filteredFiles.size) forwardPage.render(ctx, x, y, delta)
        if (page > 0) backwardPage.render(ctx, x, y, delta)
        refreshFiles.render(ctx, x, y, delta)
    }
}
