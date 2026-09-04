package llc.redstone.htslreborn.tokenizer

import guru.zoroark.tegral.niwen.lexer.NiwenLexerException
import guru.zoroark.tegral.niwen.lexer.NiwenLexerNoMatchException
import guru.zoroark.tegral.niwen.lexer.StateLabel
import guru.zoroark.tegral.niwen.lexer.Token
import guru.zoroark.tegral.niwen.lexer.matchers.anyOf
import guru.zoroark.tegral.niwen.lexer.matchers.matches
import guru.zoroark.tegral.niwen.lexer.niwenLexer
import llc.redstone.htslreborn.parser.ActionParser
import llc.redstone.htslreborn.parser.ConditionParser
import llc.redstone.htslreborn.tokenizer.States.*
import java.nio.file.Path
import kotlin.io.path.readLines

object Tokenizer {
    fun tokenize(text: String, shortcut: Boolean = true): List<TokenWithPosition> {
        val actionKeywords = ActionParser.keywords.keys
        val conditionKeywords = ConditionParser.keywords.keys

        val lexer = niwenLexer {
            default state {
                if (shortcut) placeholderShortcuts()

                actionKeywords.forEach {
                    matches("\\b$it\\b") isToken Tokens.ACTION_KEYWORD
                }

                "{\n" isToken Tokens.DEPTH_ADD

                anyOf("} else {", "}else{", "}else {", "} else{") isToken Tokens.ELSE_KEYWORD
                word("loop") isToken Tokens.LOOP_KEYWORD
                word("copy") isToken Tokens.COPY_KEYWORD
                word("paste") isToken Tokens.PASTE_KEYWORD
                "}" isToken Tokens.DEPTH_SUBTRACT
                word("random") isToken Tokens.RANDOM_KEYWORD
                word("goto") isToken Tokens.GOTO_KEYWORD

                matches("if( (and|false))?\\s*\\(") isToken Tokens.IF_AND_CONDITION_START thenState IF_CONDITION
                matches("if (or|true)\\s*\\(") isToken Tokens.IF_OR_CONDITION_START thenState IF_CONDITION

                operatorTokens()
                comparatorTokens()

                anyOf("true", "false") isToken Tokens.BOOLEAN
                "null" isToken Tokens.NULL

                '\n' isToken Tokens.NEWLINE

                matches("//.*") isToken Tokens.COMMENT
                matches("(?s)/\\*.*?\\*/") isToken Tokens.COMMENT
                matches("-?(\\d{1,3}(,\\d{3})+|\\d+)\\.\\d+") isToken Tokens.DOUBLE
                matches("-?(\\d{1,3}(,\\d{3})+|\\d+)D") isToken Tokens.DOUBLE
                matches("-?(\\d{1,3}(,\\d{3})+|\\d+)L") isToken Tokens.LONG
                matches("-?(\\d{1,3}(,\\d{3})+|\\d+)") isToken Tokens.INT

                matches("\\s+").ignore
                matches("slot_\\d+") isToken Tokens.SLOT_INDEX

                "\"\"" isToken Tokens.STRING
                matches("\\{([^\\n}]+)}") isToken Tokens.JS_CODE
                matches(""""(?:[^"\\]|\\.)*"""") isToken Tokens.STRING
                '%' isToken Tokens.PLACEHOLDER thenState PLACEHOLDER

                matches("""define """) isToken Tokens.DEFINE_KEYWORD thenState DEFINE

                matches("[^\\s(){}%\",]+") isToken Tokens.STRING
                '{' isToken Tokens.DEPTH_ADD

            }


            IF_CONDITION state {
                if (shortcut) placeholderShortcuts()
                anyOf(*conditionKeywords.toTypedArray()) isToken Tokens.CONDITION_KEYWORD

                comparatorTokens()
                operatorTokens()

                anyOf("true", "false") isToken Tokens.BOOLEAN
                "null" isToken Tokens.NULL
                anyOf(",", ", ") isToken Tokens.COMMA

                matches("//.*") isToken Tokens.COMMENT
                matches("-?(\\d{1,3}(,\\d{3})+|\\d+)\\.\\d+") isToken Tokens.DOUBLE
                matches("-?(\\d{1,3}(,\\d{3})+|\\d+)D") isToken Tokens.DOUBLE
                matches("-?(\\d{1,3}(,\\d{3})+|\\d+)L") isToken Tokens.LONG
                matches("-?(\\d{1,3}(,\\d{3})+|\\d+)") isToken Tokens.INT

                matches("\\s+").ignore

                '!' isToken Tokens.INVERTED

                matches(""""(?:[^"\\]|\\.)*"""") isToken Tokens.STRING
                '%' isToken Tokens.PLACEHOLDER thenState PLACEHOLDER_CONDITION
                matches("\\{([^\\n}]+)}") isToken Tokens.JS_CODE

                matches("[^\\s(){}%\",]+") isToken Tokens.STRING

                ')' isToken Tokens.IF_CONDITION_END thenState default
            }

            DEFINE state {
                matches(".+(?=\\n)") isToken Tokens.DEFINE_VALUE
                '\n' isToken Tokens.NEWLINE thenState default
            }


            fun placeholderStringState(state: StateLabel, nextState: StateLabel?) {
                state state {
                    matches("""[\w./ ]+""") isToken Tokens.STRING
                    if (nextState != null) {
                        "%" isToken Tokens.PLACEHOLDER thenState nextState
                    } else {
                        "%" isToken Tokens.PLACEHOLDER thenState default
                    }
                }
            }


            placeholderStringState(PLACEHOLDER, null)
            placeholderStringState(PLACEHOLDER_CONDITION, IF_CONDITION)
        }
        try {
            return lexer.tokenize(text)
                .map { token ->
                    TokenWithPosition(
                        token,
                        text.take(token.startsAt).count { it == '\n' } + 1,
                        token.startsAt - text.lastIndexOf('\n', token.startsAt - 1)
                    )
                }
        } catch (e: NiwenLexerNoMatchException) {
            val index = e.message?.substringAfter("index ")?.substringBefore(" (") ?: throw e
            val position = index.toIntOrNull() ?: throw e
            val line = text.take(position).count { it == '\n' } + 1
            val column = position - text.lastIndexOf('\n', position - 1)
            throw NiwenLexerException("No match at line $line, column $column", e)
        }
    }

    fun tokenize(path: Path): List<TokenWithPosition> {
        val input = path.readLines().joinToString("\n") { it.trim() }
        return tokenize(input)
    }

    class TokenWithPosition(
        val token: Token,
        val line: Int,
        val column: Int,
        val quoted: Boolean = false
    ) {
        val string = token.string
        val endsAt = token.endsAt
        val startsAt = token.startsAt
        val tokenType = token.tokenType
    }
}