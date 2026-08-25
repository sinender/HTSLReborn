package llc.redstone.htslreborn.htslio

import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.exporting
import llc.redstone.htslreborn.HTSLReborn.exportingFile
import llc.redstone.htslreborn.parser.ActionParser
import llc.redstone.htslreborn.parser.ActionParser.handleSwaps
import llc.redstone.htslreborn.parser.ConditionParser
import llc.redstone.htslreborn.utils.UIErrorToast
import llc.redstone.htslreborn.utils.UISuccessToast
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsdata.*
import net.minecraft.sounds.SoundEvents
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

object HTSLExporter {
    fun exportFile(path: Path, onComplete: (Boolean) -> Unit = {}) {
        SystemsAPI.launch {
            exportingFile = path
            exporting = true

            try {
                val actionContainer = SystemsAPI.getHousingImporter().getOpenActionContainer()

                if (actionContainer == null) {
                    UIErrorToast.report("You must have an action gui open to export HTSL code.")
                    onComplete(false)
                    return@launch
                }

                val actions = actionContainer.getActions()
                val lines = export(actions)
                path.parent?.let {
                    if (!it.exists()) {
                        it.createDirectories()
                    }
                }
                path.writeText(lines.joinToString("\n"))

                if (HTSLReborn.CONFIG.playCompleteSound) MC.player?.playSound(
                    SoundEvents.NOTE_BLOCK_BELL.value(),
                    1.0f,
                    1.0f
                )
                UISuccessToast.report("Successfully exported HTSL code to ${path.name}")
                onComplete(true)
            }
            catch (e: CancellationException) {
                onComplete(false)
            } catch (e: Exception) {
                if (HTSLReborn.CONFIG.playCompleteSound) MC.player?.playSound(
                    SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(),
                    1.0f,
                    0.8f
                )
                UIErrorToast.report(e)
                e.printStackTrace()
                onComplete(false)
            } finally {
                exporting = false
                exportingFile = null
            }
        }
    }

    private fun quoteIfNeeded(value: String): String {
        val escaped = value.replace("\"", "\\\"")
        return if (escaped.isEmpty() || escaped.any { it.isWhitespace() } || escaped == "null" || escaped.contains("\\\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    //This class is a little gross :)
    fun handleProperty(property: KProperty1<PropertyHolder, *>, value: Any?): List<String> {
        val properties = mutableListOf<String>()

        if (value == null) {
            properties.add("null")
            return properties
        }

        when (property.returnType.classifier) {
            String::class -> {
                if (value == "Not Set") {
                    properties.add("null")
                    return properties
                }
                properties.add(quoteIfNeeded(value as String))
            }

            StatValue::class -> {
                when (value) {
                    null -> properties.add("null")
                    is StatValue.Str -> properties.add("\"${value.value.replace("\"", "\\\"")}\"")
                    is StatValue.UnquotedStr -> properties.add(quoteIfNeeded(value.value))
                    else -> properties.add(value.toString())
                }
            }

            Int::class, Double::class, Long::class, Boolean::class -> {
                properties.add(value.toString())
            }

            StatOp::class -> {
                val statOp = value as StatOp
                when (statOp) {
                    StatOp.Inc -> properties.add("+=")
                    StatOp.Dec -> properties.add("-=")
                    StatOp.Set -> properties.add("=")
                    StatOp.Mul -> properties.add("*=")
                    StatOp.Div -> properties.add("/=")
                    StatOp.BitAnd -> properties.add("&=")
                    StatOp.BitOr -> properties.add("|=")
                    StatOp.BitXor -> properties.add("^=")
                    StatOp.LS -> properties.add("<<=")
                    StatOp.ARS -> properties.add(">>=")
                    StatOp.LRS -> properties.add(">>>=")
                    StatOp.UnSet -> properties.add("unset")
                }
            }

            Location::class -> {
                val location = value as Location
                if (location !is Location.Custom) {
                    properties.add("\"${location.key}\"")
                } else {
                    properties.add("\"custom_coordinates\" \"$location\"")
                }
            }

            Comparison::class -> {
                val comparison = value as Comparison
                when (comparison) {
                    Comparison.Eq -> properties.add("==")
                    Comparison.Gt -> properties.add(">")
                    Comparison.Lt -> properties.add("<")
                    Comparison.Ge -> properties.add(">=")
                    Comparison.Le -> properties.add("<=")
                }
            }

            InventorySlot::class -> {
                val inventorySlot = value as InventorySlot
                properties.add("\"${inventorySlot.key}\"")
            }

            ItemStack::class -> {
                val itemStack = value as ItemStack
                properties.add(
                    "\"${itemStack.nbt.toString().replace("\"", "\\\"")}\""
                )
            }

            else -> {
                if (property.returnType.isSubtypeOf(Keyed::class.starProjectedType.withNullability(true))) {
                    val keyed = value as Keyed
                    if (keyed::class.hasAnnotation<CustomKey>()) {
                        properties.add(keyed.toString())
                    } else if (keyed is KeyedLabeled) {
                        properties.add("\"${keyed.label}\"")
                    } else {
                        properties.add("\"${keyed.key}\"")
                    }
                } else {
                    properties.add(value.toString()) //More than likely null
                }
            }
        }

        return properties
    }

    fun export(actions: List<Action>): List<String> {
        val lines = mutableListOf<String>()
        for (action in actions) {
            if (action is Action.Conditional) {
                val conditional = action as Action.Conditional
                val exportedConditions = exportConditions(conditional.conditions)
                lines.add("if${if (conditional.matchAnyCondition) " or" else ""} (${exportedConditions.joinToString(", ")}) {")
                val exportedActions = export(conditional.ifActions)
                lines.addAll(exportedActions.map { "    $it" })
                if (conditional.elseActions.isNotEmpty()) {
                    lines.add("} else {")
                    val exportedElseActions = export(conditional.elseActions)
                    lines.addAll(exportedElseActions.map { "    $it" })
                }
                lines.add("}")
                continue
            }

            if (action is Action.RandomAction) {
                val randomAction = action as Action.RandomAction
                lines.add("random {")
                val exportedActions = export(randomAction.actions)
                lines.addAll(exportedActions.map { "    $it" })
                lines.add("}")
                continue
            }

            val actionClass = action::class
            val constructor = actionClass.primaryConstructor!!
            val parameters = constructor.parameters.toMutableList()

            handleSwaps(parameters, actionClass)

            val actionProperties = actionClass.memberProperties
            val newActionProperties = mutableListOf<KProperty1<Action, *>>()

            for (parm in parameters) {
                newActionProperties.add(actionProperties.find { it.name == parm.name } as KProperty1<Action, *>)
            }

            val keyword = ActionParser.keywords.entries.find { it.value == action::class }?.key ?: continue
            val properties = mutableListOf<String>()

            for (property in newActionProperties) {
                if (property.name == "actionName") continue
                val value = property.getter.call(action)
                // Add only the first string, because only conditionals and random actions should have lists
                properties.add(handleProperty(property as KProperty1<PropertyHolder, *>, value).first())
                if (value == StatOp.UnSet) break
            }

            val line = if (properties.isNotEmpty()) {
                "$keyword ${properties.joinToString(" ")}"
            } else {
                keyword
            }
            lines.add(line)
        }
        return lines
    }

    fun exportConditions(conditions: List<Condition>): List<String> {
        val conditionStrings = mutableListOf<String>()
        for (condition in conditions) {
            val conditionClass = condition::class
            val constructor = conditionClass.primaryConstructor!!
            val parameters = constructor.parameters.toMutableList()

            val conditionProperties = conditionClass.memberProperties
            val newConditionProperties = mutableListOf<KProperty1<Condition, *>>()

            for (parm in parameters) {
                newConditionProperties.add(conditionProperties.find { it.name == parm.name } as KProperty1<Condition, *>)
            }
            val keyword = ConditionParser.keywords.entries.find { it.value == condition::class }?.key ?: continue
            val properties = mutableListOf<String>()

            for (property in newConditionProperties) {
                if (property.name == "conditionName" || property.name == "inverted") continue
                val value = property.getter.call(condition)
                // Add only the first string, because only conditionals and random actions should have lists
                properties.add(handleProperty(property as KProperty1<PropertyHolder, *>, value).first())
            }

            val conditionString = "${if (condition.inverted) "!" else ""}${
                if (properties.isNotEmpty()) {
                    "$keyword ${properties.joinToString(" ")}"
                } else {
                    keyword
                }
            }"
            conditionStrings.add(conditionString)
        }
        return conditionStrings
    }
}