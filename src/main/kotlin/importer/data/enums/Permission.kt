package dev.wekend.housingtoolbox.feature.importer.data.enums

import dev.wekend.housingtoolbox.feature.importer.data.Keyed

enum class Permission(override val key: String) : Keyed {
    Fly("Fly"),
    WoodDoor("Wood Door"),
    IronDoor("Iron Door"),
    WoodTrapDoor("Wood Trap Door"),
    IronTrapDoor("Iron Trap Door"),
    FenceGate("Fence Gate"),
    Button("Button"),
    Lever("Lever"),
    UseLaunchPads("Use Launch Pads"),
    Tp("/tp"),
    TpOtherPlayers("/tp Other Players"),
    Jukebox("Jukebox"),
    Kick("Kick"),
    Ban("Ban"),
    Mute("Mute"),
    PetSpawning("Pet Spawning"),
    Build("Build"),
    OfflineBuild("Offline Build"),
    Fluid("Fluid"),
    ProTools("Pro Tools"),
    UseChests("Use Chests"),
    UseEnderChests("Use Ender Chests"),
    ItemEditor("Item Editor"),
    SwitchGameMode("Switch Game Mode"),
    EditStats("Edit Stats"),
    ChangePlayerGroup("Change Player Group"),
    ChangeGameRules("Change Gamerules"),
    HousingMenu("Housing Menu"),
    TeamChatSpy("Team Chat Spy"),
    EditActions("Edit Actions"),
    EditRegions("Edit Regions"),
    EditScoreboard("Edit Scoreboard"),
    EditEventActions("Edit Event Actions"),
    EditCommands("Edit Commands"),
    EditFunctions("Edit Functions"),
    EditInventoryLayouts("Edit Inventory Layouts"),
    EditTeams("Edit Teams"),
    EditCustomMenus("Edit Custom Menus"),
    ItemMailbox("Item: Mailbox"),
    ItemEggHunt("Item: Egg Hunt"),
    ItemTeleportPad("Item: Teleport Pad"),
    ItemLaunchPad("Item: Launch Pad"),
    ItemActionPad("Item: Action Pad"),
    ItemHologram("Item: Hologram"),
    ItemNPC("Item: NPCs"),
    ItemActionButton("Item: Action Button"),
    ItemLeaderboard("Item: Leaderboard"),
    ItemTrashCan("Item: Trash Can"),
    ItemBiomeStick("Item: Biome Stick");

    companion object {
        fun fromKey(key: String): Permission? = entries.find { it.key == key }
    }
}