package llc.redstone.htslreborn

import llc.redstone.htslreborn.commands.HTSLCommand
import llc.redstone.htslreborn.config.HtslConfig
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileExplorerHandler
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.utils.RenderUtils.isInitialized
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path

object HTSLReborn : ClientModInitializer {
    const val MOD_ID = "htslreborn"
    val LOGGER: Logger = LoggerFactory.getLogger("HTSL Reborn")
    const val VERSION = /*$ mod_version*/ "0.2.1";
    const val MINECRAFT = /*$ minecraft*/ "1.21.11";
    val CONFIG: HtslConfig = HtslConfig.createAndLoad();
    val MC: Minecraft
        get() = Minecraft.getInstance()

    var importing = false
    var importingFile: Path? = null
    var exporting = false
    var exportingFile: Path? = null

    fun Player.sendSystemMessage(comp: Component) {
        //? if <26.1 {
        this.displayClientMessage(comp, false)
        //?} else {
         /*this.sendSystemMessage(comp)
        *///?}
    }

    override fun onInitializeClient() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Loaded HTSL Reborn v$VERSION for Minecraft $MINECRAFT.")

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            HTSLCommand.register(dispatcher)
        }

        CONFIG.subscribeToImportsDirectory {
            FileHandler.baseDir = Path(it)
            FileHandler.currentDir = FileHandler.baseDir

            FileHandler.refreshFiles(live = true)
            FileExplorerHandler.setWatchedDir(FileHandler.currentDir)
            LOGGER.info(FileHandler.filteredFiles.toString())
            if (FileExplorer.INSTANCE.isInitialized()) {
                FileExplorer.INSTANCE.refreshExplorer(true)
                FileExplorer.INSTANCE.refreshBreadcrumbs()
            }
        }

        FileExplorerHandler.init()
    }
}