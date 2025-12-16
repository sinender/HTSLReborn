package llc.redstone.htslreborn

import com.github.shynixn.mccoroutine.fabric.mcCoroutineConfiguration
import llc.redstone.htslreborn.commands.HTSLCommand
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.MinecraftClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HTSLReborn : ClientModInitializer {
    val MOD_ID = "htslreborn"
    val LOGGER: Logger = LoggerFactory.getLogger("HTSL Reborn")
    val VERSION = /*$ mod_version*/ "0.0.1"
    val MINECRAFT = /*$ minecraft*/ "1.21.9"
    val MC: MinecraftClient
        get() = MinecraftClient.getInstance()

    var importing = false

    override fun onInitializeClient() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Loaded HTSL Reborn v$VERSION for Minecraft $MINECRAFT.");

        mcCoroutineConfiguration.minecraftExecutor = MinecraftClient.getInstance()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            HTSLCommand.register(dispatcher)
        }
    }
}