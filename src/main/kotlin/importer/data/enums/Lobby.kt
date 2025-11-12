package dev.wekend.housingtoolbox.feature.importer.data.enums

import dev.wekend.housingtoolbox.feature.importer.data.Keyed
import dev.wekend.housingtoolbox.feature.importer.data.KeyedSerializer
import kotlinx.serialization.Serializable

enum class Lobby(override val key: String) : Keyed {
    MainLobby("Main Lobby"),
    TournamentHall("Tournament Hall"),
    BlitzSG("Blitz SG"),
    TNTGames("The TNT Games"),
    MegaWalls("Mega Walls"),
    ArcadeGames("Arcade Games"),
    CopsAndCrims("Cops and Crims"),
    UHCChampions("UHC Champions"),
    Warlords("Warlords"),
    SmashHeroes("Smash Heroes"),
    Housing("Housing"),
    SkyWars("SkyWars"),
    SpeedUHC("Speed UHC"),
    ClassicGames("Classic Games"),
    Prototype("Prototype"),
    BedWars("Bed Wars"),
    MurderMystery("Murder Mystery"),
    BuildBattle("Build Battle"),
    Duels("Duels"),
    WoolWars("Wool Wars");

    companion object {
        fun fromKey(key: String): Lobby? = Lobby.entries.find { it.key == key }
    }
}