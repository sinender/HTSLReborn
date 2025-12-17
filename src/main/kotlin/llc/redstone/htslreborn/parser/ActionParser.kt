package llc.redstone.htslreborn.parser

import llc.redstone.systemsapi.data.*
import llc.redstone.systemsapi.data.Action.*
import guru.zoroark.tegral.niwen.lexer.Token
import llc.redstone.htslreborn.tokenizer.Operators
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import llc.redstone.htslreborn.utils.ItemConvertUtils
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.nbt.StringNbtReader
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.nio.charset.Charset
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

object ActionParser {
    val keywords = mapOf(
        "applyLayout" to ApplyInventoryLayout::class,
        "applyPotion" to ApplyPotionEffect::class,
        "cancelEvent" to CancelEvent::class,
        "globalstat" to GlobalVariable::class,
        "globalvar" to GlobalVariable::class,
        "changeHealth" to ChangeHealth::class,
        "hungerLevel" to ChangeHunger::class,
        "maxHealth" to ChangeMaxHealth::class,
        "changeGroup" to ChangePlayerGroup::class,
        "stat" to PlayerVariable::class,
        "var" to PlayerVariable::class,
        "teamstat" to TeamVariable::class,
        "teamvar" to TeamVariable::class,
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
    }

    fun createAction(keyword: String, iterator: Iterator<TokenWithPosition>, file: File): Action? {
        //Get the action class
        val clazz = keywords[keyword] ?: return null

        val constructor = clazz.primaryConstructor ?: return null

        val args: MutableMap<KParameter, Any?> = mutableMapOf()

        val parameters = constructor.parameters.toMutableList()
        handleSwaps(parameters, clazz)

        for (param in parameters) {
            val prop = clazz.memberProperties.find { it.name == param.name }!!

            if (!iterator.hasNext()) continue
            val token = iterator.next()
            //End of action
            if (token.tokenType == Tokens.NEWLINE) continue

            try {
                args[param] = when (prop.returnType.classifier) {
                    String::class -> token.string
                    Int::class -> token.string.toInt()
                    Long::class -> token.string.removeSuffix("L").toLong()
                    Double::class -> token.string.removeSuffix("D").toDouble()
                    Boolean::class -> token.string.toBoolean()
                    //Stat Values
                    StatValue::class -> {
                        when (token.tokenType) {
                            Tokens.STRING -> StatValue.Str(token.string)
                            Tokens.INT -> StatValue.I32(token.string.toInt())
                            Tokens.LONG -> StatValue.Lng(token.string.removeSuffix("L").toLong())
                            Tokens.DOUBLE -> StatValue.Dbl(token.string.removeSuffix("D").toDouble())
                            else -> error("Unknown StatValue token: ${token.string}")
                        }
                    }

                    Location::class -> LocationParser.parse(token.string, iterator)

                    ItemStack::class -> {
                        val relativeFileLocation = token.string
                        val parent = if (file.isDirectory) file else file.parentFile
                        val file = File(parent, relativeFileLocation)
                        val nbt = if (!file.exists()) {
                            try {
                                ItemConvertUtils.stringToNbtCompound(relativeFileLocation.replace("\\\"", "\""))
                            } catch (e: Exception) {
                                error("Failed to parse ItemStack NBT from string or find file at location: $relativeFileLocation")
                            }
                        } else {
                            ItemConvertUtils.fileToNbtCompound(file)
                        }

                        ItemStack(
                            nbt = nbt,
                            relativeFileLocation = relativeFileLocation,
                        )
                    }

                    StatOp::class -> when (token.tokenType) {
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

                if (prop.returnType.isSubtypeOf(Keyed::class.starProjectedType)) {
                    val companion = prop.returnType.classifier
                        .let { it as? kotlin.reflect.KClass<*> }
                        ?.companionObjectInstance
                        ?: error("No companion object for keyed enum: ${prop.returnType}")

                    val getByKeyMethod = companion::class.members.find { it.name == "fromKey" }
                        ?: error("No getByKey method for keyed enum: ${prop.returnType}")

                    args[param] = getByKeyMethod.call(companion, token.string)
                }

            } catch (e: Exception) {
                htslCompileError("Failed to parse action parameter '${param.name}': ${e.message}", token)
            }
        }

        if (args.size != constructor.parameters.size) {
            clazz.constructors.forEach { newCon ->
                if (constructor.parameters.size == newCon.parameters.size) {
                    return newCon.callBy(args)
                }
            }
        }

        return constructor.callBy(args)
    }
}