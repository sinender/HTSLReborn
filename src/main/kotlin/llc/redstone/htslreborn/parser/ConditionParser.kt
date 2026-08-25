package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Comparators
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import llc.redstone.htslreborn.utils.ItemUtils
import llc.redstone.systemsdata.*
import llc.redstone.systemsdata.Action.Conditional
import llc.redstone.systemsdata.Condition.*
import llc.redstone.systemsdata.Condition.DamageCause
import llc.redstone.systemsdata.Condition.FishingEnvironment
import llc.redstone.systemsdata.Condition.PortalType
import net.minecraft.nbt.TagParser
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

object ConditionParser {
    val keywords = linkedMapOf(
        "blockType" to BlockType::class,
        "damageAmount" to RequiredDamageAmount::class,
        "damageCause" to DamageCause::class,
        "doingParkour" to InParkour::class,
        "fishingEnv" to FishingEnvironment::class,
        "globalvar" to GlobalVariableRequirement::class,
        "globalstat" to GlobalVariableRequirement::class,
        "hasItem" to HasItem::class,
        "hasPotion" to RequiredEffect::class,
        "isItem" to IsItem::class,
        "isSneaking" to PlayerSneaking::class,
        "maxHealth" to RequiredMaxHealth::class,
        "placeholder" to RequiredPlaceholderNumber::class,
        "isFlying" to PlayerFlying::class,
        "health" to RequiredHealth::class,
        "hunger" to RequiredHungerLevel::class,
        "var" to PlayerVariableRequirement::class,
        "stat" to PlayerVariableRequirement::class,
        "portal" to PortalType::class,
        "canPvp" to PvpEnabled::class,
        "gamemode" to RequiredGameMode::class,
        "hasGroup" to RequiredGroup::class,
        "inGroup" to RequiredGroup::class,
        "hasPermission" to HasPermission::class,
        "hasTeam" to RequiredTeam::class,
        "inTeam" to RequiredTeam::class,
        "teamvar" to TeamVariableRequirement::class,
        "teamstat" to TeamVariableRequirement::class,
        "inRegion" to InRegion::class,
    )

    fun getSyntax(keyword: String): String {
        val clazz = keywords[keyword] ?: return "Unknown action '$keyword'"
        val constructor = clazz.primaryConstructor ?: return "No primary constructor for action '$keyword'"
        val params = constructor.parameters.joinToString(" ") { "<${it.name}>" }
        return "$keyword $params"
    }

    private fun isStringLike(token: TokenWithPosition): Boolean {
        return token.tokenType == Tokens.STRING || token.tokenType == Tokens.PLACEHOLDER_STRING || token.tokenType == Tokens.NULL
    }

    private fun readStringArgument(firstToken: TokenWithPosition, iterator: ListIterator<TokenWithPosition>): String {
        if (firstToken.quoted) return firstToken.string

        val value = StringBuilder(firstToken.string)
        var end = firstToken.endsAt

        while (iterator.hasNext()) {
            val next = iterator.next()
            if (!isStringLike(next) || next.startsAt != end) {
                iterator.previous()
                break
            }

            value.append(next.string)
            end = next.endsAt
        }

        return value.toString()
    }

    fun createCondition(keyword: String, iterator: ListIterator<TokenWithPosition>, path: Path?, inverted: Boolean = false): Condition? {
        val clazz = keywords[keyword] ?: run {
            println("Unknown condition keyword: $keyword")
            return null
        }
        var inverted = inverted

        val constructor = clazz.primaryConstructor ?: return null

        val args: MutableMap<KParameter, Any?> = mutableMapOf()

        for (param in constructor.parameters) {
            val prop = clazz.memberProperties.find { it.name == param.name }!!

            val token = iterator.next()
            if (token.tokenType == Tokens.COMMA || token.tokenType == Tokens.IF_CONDITION_END || token.tokenType == Tokens.DEPTH_ADD) {
                iterator.previous()
                break
            }

            if (token.tokenType == Tokens.NULL && prop.returnType.isMarkedNullable) {
                args[param] = null
                continue
            }

            args[param] = when (prop.returnType.classifier) {
                String::class -> readStringArgument(token, iterator)
                Int::class -> token.string.replace(",", "").toInt()
                Long::class -> token.string.replace(",", "").removeSuffix("L").toLong()
                Double::class -> token.string.replace(",", "").removeSuffix("D").toDouble()
                Boolean::class -> token.string.toBoolean()
                StatValue::class -> {
                    when (token.tokenType) {
                        Tokens.STRING -> if (token.string.contains("%")) {
                            StatValue.UnquotedStr(token.string)
                        } else {
                            StatValue.Str(token.string)
                        }

                        else -> StatValue.fromString(token.string)
                    }
                }

                ItemStack::class -> {
                    if (path == null) {
                        htslCompileError("Cannot load ItemStack from file when file is null", token)
                    }
                    if (token.tokenType == Tokens.NULL) {
                        args[param] = null
                        continue
                    }
                    val relativeFileLocation = token.string
                    if (token.string.startsWith("slot_")) {
                        val slot = token.string.removePrefix("slot_").toIntOrNull()
                        if (slot == null) {
                            htslCompileError("Invalid slot index: ${token.string}", token)
                        }
                        args[param] = ItemStack(
                            slot = slot,
                            relativeFileLocation = relativeFileLocation,
                        )
                        continue
                    }
                    val nbt = try {
                        val parent = if (path.isDirectory()) path else path.parent
                        val file = parent.resolve(relativeFileLocation)
                        ItemUtils.fileToNbtCompound(file)
                    } catch (_: Exception) {
                        try {
                            ItemUtils.stringToNbtCompound(relativeFileLocation.replace("\\\"", "\""))
                        } catch (e: Exception) {
                            htslCompileError(
                                "Failed to parse ItemStack NBT from string or file: ${e.message}",
                                token
                            )
                        }
                    }

                    ItemStack(
                        nbt = nbt.toString(),
                        relativeFileLocation = relativeFileLocation,
                    )
                }

                Comparison::class -> when (token.tokenType) {
                    Comparators.EQUALS -> Comparison.Eq
                    Comparators.GREATER_THAN -> Comparison.Gt
                    Comparators.LESS_THAN -> Comparison.Lt
                    Comparators.LESS_THAN_OR_EQUAL -> Comparison.Le
                    Comparators.GREATER_THAN_OR_EQUAL -> Comparison.Ge
                    Comparators.NOT_EQUALS -> {
                        inverted = !inverted
                        Comparison.Eq
                    }
                    else -> null
                }
                else -> null
            }

            if (args.containsKey(param) && args[param] != null) {
                continue
            }

            if (prop.returnType.isSubtypeOf(Keyed::class.starProjectedType.withNullability(true))) {
                val companion = prop.returnType.classifier
                    .let { it as? kotlin.reflect.KClass<*> }
                    ?.companionObjectInstance
                    ?: htslCompileError("No companion object for keyed enum: ${prop.returnType}", token)

                val getByKeyMethod = companion::class.members.find { it.name == "fromKey" }
                    ?: htslCompileError("No getByKey method for keyed enum: ${prop.returnType}", token)

                args[param] = getByKeyMethod.call(companion, token.string)
            }
        }

        val newArgs = args.filterValues { it != null }.toMutableMap()

        if (newArgs.size != constructor.parameters.size) {
            clazz.constructors.forEach { newCon ->
                if (newArgs.size == newCon.parameters.size) {
                    return newCon.callBy(newArgs)
                }
            }
        }
        val con =  try {
            constructor.callBy(args)
        } catch (_: Exception) {
            constructor.callBy(newArgs)
        }
        con.inverted = inverted
        return con
    }
}