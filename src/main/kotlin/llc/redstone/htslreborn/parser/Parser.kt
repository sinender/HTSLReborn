package llc.redstone.htslreborn.parser

import llc.redstone.systemsapi.data.Action
import llc.redstone.systemsapi.data.Action.Conditional
import llc.redstone.systemsapi.data.Action.RandomAction
import llc.redstone.systemsapi.data.Condition
import guru.zoroark.tegral.niwen.lexer.Token
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import java.io.File

object Parser {
    fun parse(tokens: List<TokenWithPosition>, file: File): MutableMap<String, List<Action>> {
        val compiledActions = mutableListOf<Action>()
        val gotoCompiled = mutableMapOf<String, List<Action>?>()

        var conditions = mutableListOf<Condition>()
        var conditional: String? = null
        var isRandom = false
        val depth = mutableMapOf<Int, Pair<MutableList<Action>, MutableList<Action>>>(
            0 to Pair(mutableListOf(), mutableListOf()),
        )
        var isElse = false

        val iterator = tokens.iterator()

        while (iterator.hasNext()) {
            val token = iterator.next()

            when (token.tokenType) {
                Tokens.GOTO_KEYWORD -> {
                    val previous = gotoCompiled.entries.find { it.value == null }
                    if (previous != null) {
                        gotoCompiled[previous.key] = compiledActions.toMutableList()
                        compiledActions.clear()
                    }

                    val type = iterator.next()
                    val args = when(type.string.lowercase()) {
                        "function" -> {
                            val name = iterator.next().string
                            "function $name"
                        }
                        "event" -> {
                            val name = iterator.next().string
                            "event $name"
                        }
                        "menu" -> {
                            val name = iterator.next().string
                            val slot = iterator.next().string.toInt()
                            "menu $name $slot"
                        }
                        "command" -> {
                            val name = iterator.next().string
                            "command $name"
                        }
                        else -> error("Unexpected token type $type")
                    }

                    if (gotoCompiled.isEmpty()) {
                        gotoCompiled["base"] = compiledActions.toMutableList()
                        compiledActions.clear()
                    }

                    gotoCompiled[args] = null
                }

                Tokens.RANDOM_KEYWORD -> {
                    isRandom = true
                }

                Tokens.IF_OR_CONDITION_START -> {
                    conditional = "or"
                    conditions = mutableListOf()
                }

                Tokens.IF_AND_CONDITION_START -> {
                    conditional = "and"
                    conditions = mutableListOf()
                }

                Tokens.CONDITION_KEYWORD -> {
                    if (conditional != null) {
                        val index = tokens.indexOf(token)
                        if (index > 0 && tokens[index - 1].tokenType == Tokens.INVERTED) {
                            println("Found inverted condition")
                            conditions.add(ConditionParser.createCondition(token.string, iterator, file, true) ?: error("Did not expect null condition"))
                            continue
                        }

                        conditions.add(ConditionParser.createCondition(token.string, iterator, file) ?: error("Did not expect null condition"))
                    }
                }

                Tokens.COMMA -> {
                    if (conditional != null && iterator.hasNext()) {
                        conditions.add(ConditionParser.createCondition(iterator.next().string, iterator, file) ?: error("Did not expect null condition"))
                    }
                }

                Tokens.DEPTH_ADD -> {
                    depth[depth.size] = Pair(mutableListOf(), mutableListOf())
                }

                Tokens.ELSE_KEYWORD -> {
                    val actions = depth[depth.size - 1]!!
                    depth[depth.size - 1] = Pair(actions.first, mutableListOf())
                    isElse = true
                }

                Tokens.DEPTH_SUBTRACT -> {
                    if (isRandom) {
                        val actions = depth[depth.size - 1]!!
                        compiledActions.add(RandomAction(actions.first))
                        isRandom = false
                        depth.remove(depth.size - 1)
                        continue
                    }

                    if (isElse) {
                        isElse = false
                    }

                    val actions = depth[depth.size - 1]!!
                    compiledActions.add(Conditional(conditions, conditional == "or", actions.first, actions.second))
                    depth.remove(depth.size - 1)
                }

                Tokens.ACTION_KEYWORD -> {
                    val action = ActionParser.createAction(token.string, iterator, file) ?: error("Did not action to be null")
                    if (depth.size - 1 == 0) {
                        compiledActions.add(action)
                        continue
                    } else {
                        if (isElse) {
                            depth[depth.size - 1]!!.second.add(action)
                        } else {
                            depth[depth.size - 1]!!.first.add(action)
                        }
                    }
                }
            }
        }

        if (gotoCompiled.isEmpty()) {
            gotoCompiled["base"] = compiledActions.toMutableList()
        } else {
            val previous = gotoCompiled.entries.find { it.value == null }
            if (previous != null) {
                gotoCompiled[previous.key] = compiledActions.toMutableList()
            }
        }

        val returned = mutableMapOf<String, List<Action>>()
        for (entry in gotoCompiled) {
            returned[entry.key] = entry.value ?: continue
        }

        return returned
    }
}