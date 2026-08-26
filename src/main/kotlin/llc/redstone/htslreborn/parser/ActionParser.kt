package llc.redstone.htslreborn.parser

import llc.redstone.htslreborn.tokenizer.Operators
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import llc.redstone.htslreborn.utils.ItemUtils
import llc.redstone.systemsdata.*
import llc.redstone.systemsdata.Action.*
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

object ActionParser {
    val keywords = linkedMapOf(
        "applyLayout" to ApplyInventoryLayout::class,
        "applyPotion" to ApplyPotionEffect::class,
        "cancelEvent" to CancelEvent::class,
        "globalvar" to GlobalVariable::class,
        "globalstat" to GlobalVariable::class,
        "changeHealth" to ChangeHealth::class,
        "hungerLevel" to ChangeHunger::class,
        "maxHealth" to ChangeMaxHealth::class,
        "changeGroup" to ChangePlayerGroup::class,
        "changePlayerGroup" to ChangePlayerGroup::class,
        "var" to PlayerVariable::class,
        "stat" to PlayerVariable::class,
        "teamvar" to TeamVariable::class,
        "teamstat" to TeamVariable::class,
        "clearEffects" to ClearAllPotionEffects::class,
        "closeMenu" to CloseMenu::class,
        "actionBar" to DisplayActionBar::class,
        "displayMenu" to DisplayMenu::class,
        "title" to DisplayTitle::class,
        "enchant" to EnchantHeldItem::class,
        "exit" to Exit::class,
        "failParkour" to FailParkour::class,
        "fullHeal" to FullHeal::class,
        "xpLevel" to GiveExperienceLevels::class,
        "giveItem" to GiveItem::class,
        "kill" to KillPlayer::class,
        "parkCheck" to ParkourCheckpoint::class,
        "pause" to PauseExecution::class,
        "sound" to PlaySound::class,
        "removeItem" to RemoveItem::class,
        "resetInventory" to ResetInventory::class,
        "chat" to SendMessage::class,
        "lobby" to SendToLobby::class,
        "compassTarget" to SetCompassTarget::class,
        "gamemode" to SetGameMode::class,
        "setTeam" to SetPlayerTeam::class,
        "tp" to TeleportPlayer::class,
        "function" to ExecuteFunction::class,
        "consumeItem" to UseHeldItem::class,
        "dropItem" to DropItem::class,
        "changeVelocity" to ChangeVelocity::class,
        "launchTarget" to LaunchToTarget::class,
        "playerWeather" to SetPlayerWeather::class,
        "playerTime" to SetPlayerTime::class,
        "displayNametag" to ToggleNametagDisplay::class,
    )

    fun getSyntax(keyword: String): String {
        val clazz = keywords[keyword] ?: return "Unknown action '$keyword'"
        if (clazz == Conditional::class) {
            return "if (<condition>) {\n    <actions>\n} else {\n    <actions>\n}"
        }
        if (clazz == RandomAccess::class) {
            return "random {\n    <actions>\n}"
        }
        val constructor = clazz.primaryConstructor ?: return "No primary constructor for action '$keyword'"
        val params = constructor.parameters.joinToString(" ") { "<${it.name}>" }
        return "$keyword $params"
    }

    fun handleSwaps(parameters: MutableList<KParameter>, clazz: KClass<out Action>) {
        fun swapParams(name: String, name2: String) {
            val index1 = parameters.indexOfFirst { it.name == name }
            val index2 = parameters.indexOfFirst { it.name == name2 }
            if (index1 != -1 && index2 != -1) {
                val temp = parameters[index1]
                parameters[index1] = parameters[index2]
                parameters[index2] = temp
            }
        }

        if (clazz == ChangeHunger::class || clazz == ChangeMaxHealth::class || clazz == ChangeHealth::class) swapParams("amount", "op")
        if (clazz == TeamVariable::class) swapParams("teamName", "variable")
        if (clazz == DropItem::class) {
            swapParams("despawnDurationTicks", "prioritizePlayer")
            swapParams("pickupDelayTicks", "inventoryFallback")
        }
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

    fun createAction(keyword: String, iterator: ListIterator<TokenWithPosition>, path: Path?): Action? {
        //Get the action class
        val clazz = keywords[keyword] ?: return null

        val constructor = clazz.primaryConstructor ?: return null
        var args: MutableMap<KParameter, Any?> = mutableMapOf()

        val parameters = constructor.parameters.toMutableList()
        handleSwaps(parameters, clazz)

        try {

            for (param in parameters) {
                val prop = clazz.memberProperties.find { it.name == param.name }!!

                if (!iterator.hasNext()) break
                val token = iterator.next()
                //End of action
                if (token.tokenType == Tokens.NEWLINE) break

                if (token.tokenType == Tokens.NULL && prop.returnType.isMarkedNullable) {
                    args[param] = null
                    continue
                }

                try {
                    args[param] = when (prop.returnType.classifier) {
                        String::class -> readStringArgument(token, iterator)
                        Int::class -> token.string.replace(",", "").toInt()
                        Long::class -> token.string.replace(",", "").removeSuffix("L").toLong()
                        Double::class -> token.string.replace(",", "").removeSuffix("D").toDouble()
                        Boolean::class -> token.string.toBoolean()
                        StatValue::class -> {
                            when (token.tokenType) {
                                Tokens.STRING -> if (!token.quoted) {
                                    StatValue.UnquotedStr(token.string)
                                } else {
                                    StatValue.Str(token.string)
                                }

                                else -> StatValue.fromString(token.string)
                            }
                        }

                        Location::class -> LocationParser.parse(token.string, iterator)

                        ItemStack::class -> {
                            if (path == null) {
                                htslCompileError("Cannot load ItemStack from file when file is null", token)
                            }
                            var relativeFileLocation = token.string
                            if (token.tokenType == Tokens.NULL) {
                                args[param] = null
                                continue
                            }

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
                                if (!relativeFileLocation.endsWith(".nbt")) {
                                    relativeFileLocation += ".nbt"
                                }
                                val file = parent.resolve(relativeFileLocation)
                                ItemUtils.fileToNbtCompound(file)
                            } catch (_: Exception) {
                                try {
                                    ItemUtils.stringToNbtCompound(token.string.replace("\\\"", "\""))
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

                        StatOp::class -> when (token.tokenType) {
                            Operators.UNSET -> StatOp.UnSet
                            Operators.SET -> StatOp.Set
                            Operators.INCREMENT -> StatOp.Inc
                            Operators.DECREMENT -> StatOp.Dec
                            Operators.MULTIPLY -> StatOp.Mul
                            Operators.DIVIDE -> StatOp.Div
                            Operators.BITWISE_AND -> StatOp.BitAnd
                            Operators.BITWISE_OR -> StatOp.BitOr
                            Operators.BITWISE_XOR -> StatOp.BitXor
                            Operators.LEFT_SHIFT -> StatOp.LS
                            Operators.LOGICAL_RIGHT_SHIFT -> StatOp.LRS
                            Operators.ARITHMETIC_RIGHT_SHIFT -> StatOp.ARS
                            else -> error("Unknown StatOp: ${token.string}")
                        }

                        InventorySlot::class -> InventorySlot.fromKey(token.string)
                        else -> null
                    }

                    if (args.containsKey(param) && args[param] != null) {
                        continue
                    }
                    if (prop.returnType.isSubtypeOf(Keyed::class.starProjectedType.withNullability(true))) {
                        val companion = prop.returnType.classifier
                            .let { it as? KClass<*> }
                            ?.companionObjectInstance
                            ?: error("No companion object for keyed enum: ${prop.returnType}")

                        val getByKeyMethod = companion::class.members.find { it.name == "fromKey" }
                            ?: error("No getByKey method for keyed enum: ${prop.returnType}")

                        args[param] = getByKeyMethod.call(companion, token.string)
                    }

                } catch (e: Exception) {
                    htslCompileError(
                        "Failed to parse action parameter '${param.name}': ${e.message} in file ${path?.name}",
                        token
                    )
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
            return try {
                constructor.callBy(args)
            } catch (_: Exception) {
                constructor.callBy(newArgs)
            }
        } catch (e: Exception) {
            htslCompileError(
                "Failed to create action instance: ${e.message} in file ${path?.name}",
                iterator.previous()
            )
        }
    }
}