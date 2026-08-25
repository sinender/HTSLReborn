package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.systemsapi.util.CommandUtils
import llc.redstone.systemsapi.util.ItemStackUtils.giveItem
import llc.redstone.systemsapi.util.NbtHelper
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.TagParser
import net.minecraft.world.level.GameType
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrNull

object ItemUtils {

    fun LocalPlayer.giveItem(path: Path): ItemStack {
        if (this.gameMode() != GameType.CREATIVE) CommandUtils.runCommand("gmc")
        val item = FileHandler.getItemForFile(path) ?: throw IllegalStateException("Could not find item at $path.")
        val slot = convertSlot(this.inventory.freeSlot) ?: throw IllegalStateException("No empty inventory slot!")
        item.giveItem(slot)
        return item
    }

    fun LocalPlayer.saveItem(path: Path): ItemStack {
        val item = this.inventory.selectedItem ?: throw IllegalStateException("Could not find held item.")
        itemStackToFile(item, path.toFile())
        return item
    }

    private fun convertSlot(slot: Int): Int? {
//        if (MC.currentScreen !is ContainerScreen) return slot
        return when (slot) {
            in 0..8 -> slot + 36
            in 9..35 -> slot
            else -> null
        }
    }

    fun fileToNbtCompound(path: Path): CompoundTag {
        val name = path.name
        if (name.endsWith(".nbt")) {
            val dataInputStream = DataInputStream(path.inputStream())
            return NbtIo.read(dataInputStream).also {
                dataInputStream.close()
            }
        }
        error("Unsupported file extension for NBT conversion: $name")
    }

    fun itemStackToFile(itemStack: ItemStack, file: File) {
        val nbtCompound = NbtHelper.serializeItemStack(itemStack).getOrNull()
        val dataOut = DataOutputStream(FileOutputStream(file))
        NbtIo.write(nbtCompound ?: CompoundTag(), dataOut)
        dataOut.close()
    }
    fun stringToNbtCompound(nbtString: String): CompoundTag {
        return TagParser.parseCompoundFully(nbtString)
    }

    fun fileToItemStack(path: Path) =
        NbtHelper.deserializeItemStack(fileToNbtCompound(path)).getOrNull()

}