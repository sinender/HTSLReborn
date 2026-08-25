package llc.redstone.htslreborn.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.utils.ItemUtils.giveItem
import llc.redstone.htslreborn.utils.ItemUtils.saveItem
//? if <26.1 {
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument

//?} else {
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
*///?}
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component
import java.nio.file.Path
import kotlin.io.path.*

object HTSLCommand {
    private fun resolveBaseFile(fileArg: String, extension: String): Path {
        val trimmed = fileArg.trim()
        val pathWithExtension = if (trimmed.endsWith(".$extension", ignoreCase = true)) trimmed else "$trimmed.$extension"
        val path = Path(pathWithExtension)
        return if (path.isAbsolute) path else FileHandler.baseDir.resolve(path)
    }

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            literal("htsl")
                .then(
                    literal("import")
                        .then(
                            argument("file", StringArgumentType.greedyString())
                                .executes(::import)
                        )
                )
                .then(literal("item")
                    .then(literal("give").then(
                        argument("file", StringArgumentType.greedyString())
                            .executes(::giveItem)
                    ))
                    .then(literal("save").then(
                        argument("file", StringArgumentType.greedyString())
                            .executes(::saveItem)
                    ))
                    .then(literal("delete").then(
                        argument("file", StringArgumentType.greedyString())
                            .executes(::deleteItem)
                    ))
                )
        )
    }

    fun import(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file") ?: return -1
        val file = resolveBaseFile(fileArg, "htsl")

        HTSLImporter.importFile(file, supportsBase = false)

        return 1
    }

    fun giveItem(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file")
        val file = resolveBaseFile(fileArg, "nbt")

        try {
            val item = context.source.player.giveItem(file)
            context.source.sendFeedback(Component.translatable(
                "htslreborn.command.item.give.success",
                item.hoverName
            ))
            return 1
        } catch (e: IllegalStateException) {
            context.source.sendError(Component.translatable(
                "htslreborn.command.item.give.fail",
                file.pathString
            ))
            e.printStackTrace()
            return -1
        }
    }

    fun saveItem(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file")
        val file = resolveBaseFile(fileArg, "nbt")

        try {
            val item = context.source.player.saveItem(file)
            context.source.sendFeedback(Component.translatable(
                "htslreborn.command.item.save.success",
                item.hoverName, file.pathString
            ))
            return 1
        } catch (e: IllegalStateException) {
            context.source.sendError(Component.translatable(
                "htslreborn.command.item.save.fail",
                file.pathString
            ))
            e.printStackTrace()
            return -1
        }
    }

    fun deleteItem(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file")
        val file = resolveBaseFile(fileArg, "nbt")

        try {
            file.deleteExisting()
            context.source.sendFeedback(Component.translatable(
                "htslreborn.command.item.delete.success",
                file.pathString
            ))
            return 1
        } catch (e: Exception) {
            context.source.sendError(Component.translatable(
                "htslreborn.command.item.delete.fail",
                file.pathString
            ))
            e.printStackTrace()
            return -1
        }
    }
}