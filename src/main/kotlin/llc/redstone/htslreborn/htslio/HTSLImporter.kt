package llc.redstone.htslreborn.htslio

import com.github.shynixn.mccoroutine.fabric.launch
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.importing
import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsapi.api.Event
import llc.redstone.systemsapi.data.Action
import llc.redstone.systemsapi.util.CommandUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.world.GameMode
import java.io.File
import kotlin.collections.contains
import kotlin.collections.isNotEmpty

object HTSLImporter {
    fun importFile(file: File, supportsBase: Boolean = true) {
        val compiledCode: MutableMap<String, List<Action>>
        try {
            val tokens = Tokenizer.tokenize(file)
            compiledCode = Parser.parse(tokens, file)
        }  catch (e: Exception) {
            MinecraftClient.getInstance().player?.sendMessage(
                Text.of("An error occurred while importing HTSL code: ${e.message}").copy().withColor(Colors.RED), false
            )
            e.printStackTrace()
            return
        }

        import(compiledCode, supportsBase)
    }

    fun import(compiledCode: MutableMap<String, List<Action>>, supportsBase: Boolean = true) {
        if (compiledCode.contains("base") && compiledCode["base"]?.isNotEmpty() == true && !supportsBase) {
            MinecraftClient.getInstance().player?.sendMessage(
                Text.of("Couldn't use actions before a goto call.").copy().withColor(Colors.RED), false
            )
        }

        if (MC.player?.gameMode != GameMode.CREATIVE) CommandUtils.runCommand("gmc")

        //TODO: go through the compiled code and look for anything that doesnt exist yet and prompt the user to create it first

        HTSLReborn.launch {
            try {
                importing = true
                for ((goto, actions) in compiledCode) {
                    val split = goto.split(" ")
                    when (split.first()) {
                        "base" -> {
                            SystemsAPI.getHousingImporter().getOpenActionContainer()
                                ?.addActions(actions)
                        }

                        "function" -> {
                            val name = split.getOrNull(1) ?: continue
                            SystemsAPI.getHousingImporter().getFunction(name)?.getActionContainer()
                                ?.addActions(actions)
                        }

                        "command" -> {
                            val name = split.getOrNull(1) ?: continue
                            SystemsAPI.getHousingImporter().getCommand(name)?.getActionContainer()
                                ?.addActions(actions)
                        }

                        "event" -> {
                            val name = split.getOrNull(1) ?: continue
                            SystemsAPI.getHousingImporter().getEvent(Event.Events.valueOf(name))
                                .addActions(actions)
                        }

                        "gui" -> {
                            val name = split.getOrNull(1) ?: continue
                            val slot = split.getOrNull(2)?.toIntOrNull() ?: continue
                            SystemsAPI.getHousingImporter().getMenu(name)?.getMenuElement(slot)?.getActionContainer()
                                ?.addActions(actions)
                        }
                    }
                }
            } catch (e: Exception) {
                MinecraftClient.getInstance().player?.sendMessage(
                    Text.of("An error occurred while importing HTSL code: ${e.message}").copy().withColor(Colors.RED), false
                )
                e.printStackTrace()
            }
            importing = false
        }
    }
}