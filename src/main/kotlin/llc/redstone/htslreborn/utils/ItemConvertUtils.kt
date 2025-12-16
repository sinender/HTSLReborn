package llc.redstone.htslreborn.utils

import llc.redstone.systemsapi.util.ItemUtils
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.StringNbtReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream

object ItemConvertUtils {
    fun fileToNbtCompound(file: File): NbtCompound {
        val name = file.name
        val content = file.readText()
        if (name.endsWith(".json")) {
            return ItemUtils.toNBT(ItemUtils.createFromJsonString(content))
        } else if (name.endsWith(".nbt")) {
            val dataInputStream = DataInputStream(FileInputStream(file))
            return NbtIo.readCompound(dataInputStream).also {
                dataInputStream.close()
            }
        } else {
            return StringNbtReader.readCompound(file.readText(Charsets.UTF_8))
        }
    }

    fun stringToNbtCompound(nbtString: String): NbtCompound {
        return StringNbtReader.readCompound(nbtString)
    }

    fun fileToItemStack(file: File) =
        ItemUtils.createFromNBT(fileToNbtCompound(file))
}