@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package dev.wekend.housingtoolbox.feature.importer.data

import dev.wekend.housingtoolbox.feature.importer.data.enums.Permission
import dev.wekend.housingtoolbox.feature.importer.data.enums.PotionEffect
import dev.wekend.housingtoolbox.feature.importer.lexar.Comparators
import dev.wekend.housingtoolbox.feature.importer.lexar.Operators
import dev.wekend.housingtoolbox.feature.importer.lexar.Tokens
import guru.zoroark.tegral.niwen.lexer.Token
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/*
Borrowed from https://github.com/sndyx/hsl, licensed under the MIT License
 */

@Serializable
sealed class Condition(
    @Transient val conditionName: String = ""
) {
    
    companion object {
        fun createCondition(keyword: String, iterator: Iterator<Token>, inverted: Boolean = false): Condition? {
            val clazz = Condition::class.sealedSubclasses.find {
                it.annotations
                    .filterIsInstance<Keyword>()
                    .any { it.keyword == keyword }
            } ?: return null

            return clazz.primaryConstructor?.let { constructor ->
                val args = constructor.parameters.associateWith args@{ param ->
                    val prop = clazz.memberProperties.find { it.name == param.name }!!
                    val re = when (prop.returnType.jvmErasure.simpleName) {
                        "String" -> iterator.next().string
                        "Int" -> iterator.next().string.toInt()
                        "Long" -> iterator.next().string.removeSuffix("L").toLong()
                        "Double" -> iterator.next().string.toDouble()
                        "Boolean" -> iterator.next().string.toBoolean()
                        "StatValue" -> {
                            val token = iterator.next()
                            when (token.tokenType) {
                                Tokens.STRING -> StatValue.Str(token.string)
                                Tokens.INT -> StatValue.I64(token.string.toLong())
                                Tokens.LONG -> StatValue.I64(token.string.removeSuffix("L").toLong())
                                Tokens.DOUBLE -> StatValue.Dbl(token.string.toDouble())
                                else -> StatValue.Str(token.string)
                            }
                        }

                        "Comparison" -> {
                            val token = iterator.next()
                            Comparison.entries.find {
                                when (token.tokenType) {
                                    Comparators.EQUALS -> it == Comparison.Eq
                                    Comparators.GREATER_THAN -> it == Comparison.Gt
                                    Comparators.GREATER_THAN_OR_EQUAL -> it == Comparison.Ge
                                    Comparators.LESS_THAN -> it == Comparison.Lt
                                    Comparators.LESS_THAN_OR_EQUAL -> it == Comparison.Le
                                    else -> false
                                }
                            } ?: error("Unknown Comparison: ${token.string}")
                        }

                        else -> error("Unsupported parameter type: ${prop.returnType.jvmErasure.simpleName} in Condition ${clazz.simpleName}")
                    }

                    return@args re
                }

                val con = constructor.callBy(args)
                con.inverted = inverted
                return@let con
            }
        }
    }

    var inverted = false

    @Keyword("hasGroup")
    data class RequiredGroup(
        @SerialName("required_group")
        val group: String,
        @SerialName("include_higher_groups")
        val includeHigherGroups: Boolean,
    ) : Condition("IN_GROUP")

    @Keyword("var")
    @Keyword("stat")
    data class PlayerVariableRequirement(
        val variable: String,
        val op: Comparison,
        val value: StatValue,
    ) : Condition("VARIABLE_REQUIREMENT") {
        val holder = VariableHolder.Player
    }

    @Keyword("teamvar")
    @Keyword("teamstat")
    data class TeamVariableRequirement(
        val team: String?,
        val variable: String,
        val op: Comparison,
        val value: StatValue,
    ) : Condition("VARIABLE_REQUIREMENT") {
        val holder = VariableHolder.Team
    }

    @Keyword("globalvar")
    @Keyword("globalstat")
    data class GlobalVariableRequirement(
        val variable: String,
        val op: Comparison,
        val value: StatValue,
    ) : Condition("VARIABLE_REQUIREMENT") {
        val holder = VariableHolder.Global
    }

    @Keyword("hasPermission")
    data class HasPermission(
        @SerialName("required_permission")
        val permission: Permission,
    ) : Condition("HAS_PERMISSION")
    
    @Keyword("inRegion")
    data class InRegion(
        val region: String,
    ) : Condition("IN_REGION")
    
    @Keyword("hasItem")
    data class HasItem(
        val item: ItemStack,
        @SerialName("what_to_check") val whatToCheck: ItemCheck,
        @SerialName("where_to_check") val whereToCheck: InventoryLocation,
        @SerialName("required_amount") val amount: ItemAmount,
    ) : Condition("HAS_ITEM")
    
    @Keyword("doingParkour")
    data object InParkour : Condition("IN_PARKOUR")
    
    @Keyword("hasPotion")
    data class RequiredEffect(
        val effect: PotionEffect,
    ) : Condition("POTION_EFFECT")
    
    @Keyword("isSneaking")
    data object PlayerSneaking : Condition("SNEAKING")
    
    @Keyword("isFlying")
    data object PlayerFlying : Condition("FLYING")
    
    @Keyword("health")
    data class RequiredHealth(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("HEALTH")
    
    @Keyword("maxHealth")
    data class RequiredMaxHealth(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("MAX_HEALTH")
    
    @Keyword("hunger")
    data class RequiredHungerLevel(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("HUNGER_LEVEL")
    
    @Keyword("gamemode")
    data class RequiredGameMode(
        @SerialName("required_gamemode")
        val gameMode: GameMode
    ) : Condition("GAMEMODE")
    
    @Keyword("placeholder")
    data class RequiredPlaceholderNumber(
        val placeholder: String,
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("PLACEHOLDER_NUMBER")
    
    @Keyword("inTeam")
    data class RequiredTeam(
        @SerialName("required_team")
        val team: String,
    ) : Condition("IN_TEAM")
    
    @Keyword("canPvp")
    data object PvpEnabled : Condition("PVP_ENABLED")
    
    @Keyword("fishingEnv")
    data class FishingEnvironment(
        val environment: dev.wekend.housingtoolbox.feature.importer.data.FishingEnvironment
    ) : Condition("FISHING_ENVIRONMENT")
    
    @Keyword("portal")
    data class PortalType(
        @SerialName("portal_type")
        val type: dev.wekend.housingtoolbox.feature.importer.data.PortalType
    ) : Condition("PORTAL_TYPE")
    
    @Keyword("damageCause")
    data class DamageCause(
        val cause: dev.wekend.housingtoolbox.feature.importer.data.DamageCause
    ) : Condition("DAMAGE_CAUSE")
    
    @Keyword("damageAmount")
    data class RequiredDamageAmount(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("DAMAGE_AMOUNT")
    
    @Keyword("blockType")
    data class BlockType(
        val item: ItemStack,
        @SerialName("match_type_only")
        val matchTypeOnly: Boolean,
    ) : Condition("BLOCK_TYPE")
    
    @Keyword("isItem")
    data class IsItem(
        val item: ItemStack,
        @SerialName("what_to_check") val whatToCheck: ItemCheck,
        @SerialName("where_to_check") val whereToCheck: InventoryLocation,
        @SerialName("required_amount") val amount: ItemAmount,
    ) : Condition("IS_ITEM")
}

enum class Comparison {
    @SerialName("EQUAL") Eq,
    @SerialName("GREATER_THAN") Gt,
    @SerialName("GREATER_THAN_OR_EQUAL") Ge,
    @SerialName("LESS_THAN") Lt,
    @SerialName("LESS_THAN_OR_EQUAL") Le;
}

enum class ItemCheck(override val key: String) : Keyed {
    ItemType("Item Type"),
    Metadata("Metadata");

    companion object {
        fun fromKey(key: String): ItemCheck? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class ItemAmount(override val key: String) : Keyed {
    Any("Any Amount"),
    Ge("Equal or Greater Amount");

    companion object {
        fun fromKey(key: String): ItemAmount? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class InventoryLocation(override val key: String) : Keyed {
    Hand("Hand"),
    Armor("Armor"),
    HotBar("Hotbar"),
    Inventory("Inventory"),
    Anywhere("Anywhere");

    companion object {
        fun fromKey(key: String): InventoryLocation? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class FishingEnvironment(override val key: String) : Keyed {
    Water("Water"),
    Lava("Lava");

    companion object {
        fun fromKey(key: String): FishingEnvironment? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class PortalType(override val key: String) : Keyed {
    EndPortal("End Portal"),
    NetherPortal("Nether Portal");

    companion object {
        fun fromKey(key: String): PortalType? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}

enum class DamageCause(override val key: String) : Keyed {
    EntityAttack("Entity Attack"),
    Projectile("Projectile"),
    Suffocation("Suffocation"),
    Fall("Fall"),
    Lava("Lava"),
    Fire("Fire"),
    FireTick("Fire Tick"),
    Drowning("Drowning"),
    Starvation("Starvation"),
    Poison("Poison"),
    Thorns("Thorns");
    
    companion object {
        fun fromKey(key: String): DamageCause? {
            return entries.find { it.key.equals(key, ignoreCase = true) }
        }
    }
}