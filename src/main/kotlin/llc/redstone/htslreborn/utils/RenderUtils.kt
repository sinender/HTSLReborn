package llc.redstone.htslreborn.utils

import net.minecraft.client.gui.screens.Screen

object RenderUtils {

    fun Screen.isInitialized(): Boolean {
        return this.width > 0 && this.height > 0
    }


}