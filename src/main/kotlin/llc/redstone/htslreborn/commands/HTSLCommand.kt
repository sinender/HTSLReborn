package llc.redstone.htslreborn.commands

import com.github.shynixn.mccoroutine.fabric.launch
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.systemsapi.SystemsAPI
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Colors
import java.io.File
import kotlin.reflect.full.primaryConstructor

object HTSLCommand {
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
        )
    }

    fun import(context: CommandContext<FabricClientCommandSource>): Int {
        val fileArg = StringArgumentType.getString(context, "file") ?: return -1

        val file = File(fileArg)

        val tokens = Tokenizer.tokenize(file)
        val compiledCode = Parser.parse(tokens, file)
        if (compiledCode.contains("base") && compiledCode["base"]?.isNotEmpty() == true) {
            MinecraftClient.getInstance().player?.sendMessage(
                Text.of("Couldn't use actions before a goto call.").copy().withColor(Colors.RED), false
            )
        }

        for ((goto, actions) in compiledCode) {
            val split = goto.split(" ")
            when (split.first()) {
                "function" -> {
//                    HTSLReborn.launch {
//                        SystemsAPI.getHousingImporter().getFunctionOrNull(split[1])?.getActionContainer()?.addActions(actions)
//                    }
                    for (action in actions) {
                        println(
                            action::class.simpleName + "(" + action::class.primaryConstructor?.parameters?.joinToString(
                                ", "
                            ) {
                                val value =
                                    action::class.primaryConstructor?.parameters?.find { p -> p.name == it.name }
                                        ?.let { prop ->
                                            action::class.members.find { m -> m.name == prop.name }?.call(action)
                                        }
                                "${it.name}=$value"
                            })
                    }
                }
            }
        }

        return 1
    }
}