package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.HTSLReborn
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier

object RenderUtils {
    fun DrawContext.drawTexture(sprite: Identifier, x: Int, y: Int, width: Int, height: Int) {
        this.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            x,
            y,
            0f,
            0f,
            width,
            height,
            width,
            height
        )
    }

    fun DrawContext.drawText(text: String, x: Int, y: Int) {
        this.drawTextWithShadow(HTSLReborn.MC.textRenderer, text, x, y, getColor(255, 255, 255))
    }

    fun getColor(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
        return ((alpha.coerceIn(0, 255) shl 24) or
                (red.coerceIn(0, 255) shl 16) or
                (green.coerceIn(0, 255) shl 8) or
                blue.coerceIn(0, 255))
    }


}