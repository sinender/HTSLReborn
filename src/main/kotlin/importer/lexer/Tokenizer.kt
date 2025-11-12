package dev.wekend.housingtoolbox.feature.importer.lexar

import dev.wekend.housingtoolbox.feature.importer.data.Action
import dev.wekend.housingtoolbox.feature.importer.data.Condition
import dev.wekend.housingtoolbox.feature.importer.data.Keyword
import dev.wekend.housingtoolbox.feature.importer.lexar.States.*
import guru.zoroark.tegral.niwen.lexer.StateLabel
import guru.zoroark.tegral.niwen.lexer.matchers.anyOf
import guru.zoroark.tegral.niwen.lexer.matchers.matches
import guru.zoroark.tegral.niwen.lexer.niwenLexer

fun main() {
    val actionKeywords = Action::class.sealedSubclasses.flatMap { action ->
        action.annotations.filterIsInstance<Keyword>().map { it.keyword }
    }

    val conditionKeywords = Condition::class.sealedSubclasses.flatMap { condition ->
        condition.annotations.filterIsInstance<Keyword>().map { it.keyword }
    }

    val lexer = niwenLexer {
        default state {
            anyOf(*actionKeywords.toTypedArray()) isToken Tokens.ACTION_KEYWORD

            "{\n" isToken Tokens.DEPTH_ADD

            anyOf("} else {", "}else{", "}else {", "} else{") isToken Tokens.ELSE_KEYWORD
            '}' isToken Tokens.DEPTH_SUBTRACT
            "random" isToken Tokens.RANDOM_KEYWORD

            matches("if\\(|if \\(|if and\\(|if and \\(") isToken Tokens.IF_AND_CONDITION_START thenState IF_CONDITION
            matches("if or\\(|if or \\(") isToken Tokens.IF_OR_CONDITION_START thenState IF_CONDITION

            comparatorTokens()
            operatorTokens()

            anyOf("true", "false") isToken Tokens.BOOLEAN

            matches("//.*") isToken Tokens.COMMENT
            matches("\\d+\\.\\d+") isToken Tokens.DOUBLE
            matches("\\d+?L") isToken Tokens.LONG
            matches("\\d+") isToken Tokens.INT
            matches("\\s+").ignore

            '\"' isToken Tokens.QUOTE thenState IN_STRING
            '\n' isToken Tokens.NEWLINE

            matches("\\w+") isToken Tokens.STRING
        }

        IF_CONDITION state {
            anyOf(*conditionKeywords.toTypedArray()) isToken Tokens.CONDITION_KEYWORD

            comparatorTokens()
            operatorTokens()

            anyOf("true", "false") isToken Tokens.BOOLEAN
            anyOf(",", ", ") isToken Tokens.COMMA

            matches("//.*") isToken Tokens.COMMENT
            matches("\\d+\\.\\d+") isToken Tokens.DOUBLE
            matches("\\d+?L") isToken Tokens.LONG
            matches("\\d+") isToken Tokens.INT
            matches("\\s+").ignore

            '!' isToken Tokens.INVERTED

            '\"' isToken Tokens.QUOTE thenState IN_CONDITION_STRING

            matches("\\w+") isToken Tokens.STRING

            ')' isToken Tokens.IF_CONDITION_END thenState default
        }

        fun stringState(state: StateLabel, nextState: StateLabel?) {
            state state {
                matches("""(\\"|[^"])+""") isToken Tokens.STRING
                if (nextState != null) {
                    '\"' isToken Tokens.QUOTE thenState nextState
                } else {
                    '\"' isToken Tokens.QUOTE thenState default
                }
            }
        }

        stringState(IN_STRING, null)
        stringState(IN_CONDITION_STRING, IF_CONDITION)
    }

    val tokens = lexer.tokenize(
        """
            random {
                giveItem "test" false "Helmet" false
                sound "Anvil Land" 0.7 1 custom_coordinates "2 90 1 ~ ~"
                changeHealth dec 5
                changeHunger inc 10
            }
            if or (!var "hello world" <= 10) {
                changeHealth inc 10
            } else {
                title "\"Hello World!" "This is a subtitle!" 5 2 3
            }
        """.trimIndent()
    ).filter { it.tokenType != Tokens.QUOTE }

    for (token in tokens) {
        println("${token.tokenType} -> '${token.string}'")
    }

    val compiledActions = mutableListOf<Action>()

    var conditions = mutableListOf<Condition>()
    var conditional: String? = null
    var isRandom = false
    var depth = mutableMapOf<Int, Pair<MutableList<Action>, MutableList<Action>>>(
        0 to Pair(mutableListOf(), mutableListOf()),
    )
    var isElse = false
    val iterator = tokens.iterator()
    while (iterator.hasNext()) {
        val token = iterator.next()

        when (token.tokenType) {
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
                        conditions.add(Condition.createCondition(token.string, iterator, true) ?: run {
                            println("Did not expect null condition")
                            continue
                        })
                        continue
                    }
                    conditions.add(Condition.createCondition(token.string, iterator) ?: run {
                        println("Did not expect null condition")
                        continue
                    })
                }
            }

            Tokens.COMMA -> {
                if (conditional != null && iterator.hasNext()) {
                    conditions.add(Condition.createCondition(iterator.next().string, iterator) ?: run {
                        println("Did not expect null condition")
                        continue
                    })
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
                    compiledActions.add(Action.RandomAction(actions.first))
                    isRandom = false
                    depth.remove(depth.size - 1)
                    continue
                }

                if (isElse) {
                    isElse = false
                }

                val actions = depth[depth.size - 1]!!
                compiledActions.add(Action.Conditional(conditions, conditional == "or", actions.first, actions.second))
                depth.remove(depth.size - 1)
            }

            Tokens.ACTION_KEYWORD -> {
                val action = Action.createAction(token.string, iterator) ?: run {
                    println("Did not expect null")
                    continue
                }
                println("Created action: $action")
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

    println(compiledActions)
}