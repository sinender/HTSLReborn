package llc.redstone.htslreborn.tokenizer

import llc.redstone.htslreborn.tokenizer.States.*
import guru.zoroark.tegral.niwen.lexer.StateLabel
import guru.zoroark.tegral.niwen.lexer.Token
import guru.zoroark.tegral.niwen.lexer.matchers.anyOf
import guru.zoroark.tegral.niwen.lexer.matchers.matches
import guru.zoroark.tegral.niwen.lexer.niwenLexer
import llc.redstone.htslreborn.parser.ActionParser
import llc.redstone.htslreborn.parser.ConditionParser
import java.io.File

object Tokenizer {
    fun tokenize(file: File): List<TokenWithPosition> {
        val actionKeywords = ActionParser.keywords.keys
        val conditionKeywords = ConditionParser.keywords.keys

        val lexer = niwenLexer {
            default state {
                anyOf(*actionKeywords.toTypedArray()) isToken Tokens.ACTION_KEYWORD

                "{\n" isToken Tokens.DEPTH_ADD

                anyOf("} else {", "}else{", "}else {", "} else{") isToken Tokens.ELSE_KEYWORD
                '}' isToken Tokens.DEPTH_SUBTRACT
                "random" isToken Tokens.RANDOM_KEYWORD
                "goto" isToken Tokens.GOTO_KEYWORD

                matches("if\\(|if \\(|if and\\(|if and \\(") isToken Tokens.IF_AND_CONDITION_START thenState IF_CONDITION
                matches("if or\\(|if or \\(") isToken Tokens.IF_OR_CONDITION_START thenState IF_CONDITION

                comparatorTokens()
                operatorTokens()

                anyOf("true", "false") isToken Tokens.BOOLEAN

                '\n' isToken Tokens.NEWLINE

                matches("//.*") isToken Tokens.COMMENT
                matches("\\d+\\.\\d+") isToken Tokens.DOUBLE
                matches("\\d+?L") isToken Tokens.LONG
                matches("\\d+") isToken Tokens.INT
                matches("\\s+").ignore

                '\"' isToken Tokens.QUOTE thenState IN_STRING

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

        val fileStr = file.readLines().joinToString("\n")

        return lexer.tokenize(fileStr)
            .filter { it.tokenType != Tokens.QUOTE } //Filter out unused and wasted tokens
//            .filter { it.tokenType != Tokens.NEWLINE }
            .map { token ->
                TokenWithPosition(
                    token,
                    fileStr.take(token.startsAt).count { it == '\n' } + 1,
                    token.startsAt - fileStr.lastIndexOf('\n', token.startsAt - 1)
                )
            }
    }

    class TokenWithPosition(
        val token: Token,
        val line: Int,
        val column: Int
    ) {
        val string = token.string
        val endsAt = token.endsAt
        val startsAt = token.startsAt
        val tokenType = token.tokenType
    }
}