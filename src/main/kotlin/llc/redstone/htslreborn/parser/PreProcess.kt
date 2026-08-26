package llc.redstone.htslreborn.parser

import guru.zoroark.tegral.niwen.lexer.Token
import llc.redstone.htslreborn.tokenizer.PlaceholderShortcuts
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition
import llc.redstone.htslreborn.tokenizer.Tokens
import llc.redstone.htslreborn.utils.ErrorUtils.htslCompileError
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject

object PreProcess {
    private fun retokenizeAtOriginalPosition(
        text: String,
        shortcut: Boolean,
        originalToken: TokenWithPosition
    ): List<TokenWithPosition> {
        return Tokenizer.tokenize(text, shortcut).map { token ->
            TokenWithPosition(
                Token(
                    token.string,
                    originalToken.startsAt + token.startsAt,
                    originalToken.startsAt + token.endsAt,
                    token.tokenType
                ),
                originalToken.line,
                originalToken.column + token.startsAt,
                token.quoted
            )
        }
    }

    fun preProcess(
        tokens: List<TokenWithPosition>,
        loopVarName: String? = null,
        loopIndex: Int? = null,
        defineList: MutableMap<String, String> = mutableMapOf(),
        reusedContext: Context? = null,
        reusedScope: ScriptableObject? = null
    ): List<TokenWithPosition> {
        try {
            val processedTokens = mutableListOf<TokenWithPosition>()

            val context = reusedContext ?: Context.enter()
            val scope = reusedScope ?: context.initSafeStandardObjects()
            if (loopVarName != null) {
                val index = loopIndex ?: 0
                scope.put(loopVarName, scope, index)
            }

            val iterator = tokens.listIterator()

            var loopAmount: Int
            var loopVarName: String? = loopVarName
            while (iterator.hasNext()) {
                val token = iterator.next()
                when (token.tokenType) {
                    is PlaceholderShortcuts -> {
                        val args = retokenizeAtOriginalPosition(token.string, false, token)
                        fun stringOrEmpty(index: Int): String {
                            return args.getOrNull(index)?.string ?: ""
                        }

                        val placeholder = when (token.tokenType) {
                            PlaceholderShortcuts.GLOBAL_VAR -> "%var.global/${stringOrEmpty(2)}%"
                            PlaceholderShortcuts.PLAYER_VAR -> "%var.player/${stringOrEmpty(2)}%"
                            PlaceholderShortcuts.TEAM_VAR -> "%var.team/${stringOrEmpty(2)} ${stringOrEmpty(3)}%"
                            PlaceholderShortcuts.RANDOM_INT -> "%random.whole/${stringOrEmpty(2)} ${stringOrEmpty(3)}%"
                            PlaceholderShortcuts.RANDOM_DOUBLE -> "%random.decimal/${stringOrEmpty(2)} ${stringOrEmpty(3)}%"
                            PlaceholderShortcuts.HEALTH -> "%player.health%"
                            PlaceholderShortcuts.MAX_HEALTH -> "%player.maxhealth%"
                            PlaceholderShortcuts.HUNGER -> "%player.hunger%"
                            PlaceholderShortcuts.LOC_X -> "%player.pos.x%"
                            PlaceholderShortcuts.LOC_Y -> "%player.pos.y%"
                            PlaceholderShortcuts.LOC_Z -> "%player.pos.z%"
                            PlaceholderShortcuts.UNIX -> "%date.unix%"
                        }

                        processedTokens.add(
                            args[0]
                        )

                        processedTokens.add(
                            TokenWithPosition(
                                Token(
                                    placeholder,
                                    token.startsAt,
                                    token.endsAt,
                                    Tokens.PLACEHOLDER_STRING,
                                ),
                                token.line,
                                token.column
                            )
                        )
                    }

                    Tokens.IF_CONDITION_END, Tokens.COMMENT -> {
                        // Skip these tokens
                    }

                    Tokens.ACTION_KEYWORD -> {
                        processedTokens.add(
                            TokenWithPosition(
                                token.token.copy(string = token.string.trim()),
                                token.line,
                                token.column
                            )
                        )
                    }

                    Tokens.PLACEHOLDER -> {
                        val valueToken = iterator.takeIf { it.hasNext() }?.next()
                        if (valueToken?.tokenType != Tokens.STRING) htslCompileError(
                            "Expected placeholder name after '%'",
                            valueToken ?: token
                        )
                        val placeholder = valueToken.string

                        val closingToken = iterator.takeIf { it.hasNext() }?.next()
                        if (closingToken?.tokenType != Tokens.PLACEHOLDER) htslCompileError(
                            "Expected closing '%' for placeholder '$placeholder'",
                            closingToken ?: token
                        )

                        processedTokens.add(
                            TokenWithPosition(
                                Token(
                                    "%$placeholder%",
                                    token.startsAt,
                                    closingToken.endsAt,
                                    Tokens.PLACEHOLDER_STRING,
                                ),
                                token.line,
                                token.column
                            )
                        )
                    }

                    Tokens.JS_CODE -> {
                        // Replace defined variables
                        var processedString = token.string
                        for ((key, value) in defineList) {
                            val regex = Regex("(?<!\")\\b$key\\b(?!\")") // Match whole words not inside quotes
                            processedString = processedString.replace(regex, value)
                        }

                        var result = context.evaluateString(scope, processedString, "HTSL_JS_EVAL", token.line, null)

                        if (result is String && result.contains(" ") && !result.contains("%")) {
                            result = "\"$result\""
                        }

                        processedTokens.addAll(preProcess(Tokenizer.tokenize(result.toString())))
                    }

                    Tokens.DEFINE_KEYWORD -> {
                        val valueToken = iterator.takeIf { it.hasNext() }?.next()
                        if (valueToken?.tokenType != Tokens.DEFINE_VALUE) htslCompileError(
                            "Expected value after variable name",
                            valueToken ?: token
                        )
                        val varName = valueToken.string.split(" ")[0]
                        if (listOf("goto", "//", "/*", "*/", "loop").contains(varName)) {
                            htslCompileError("Invalid variable name '$varName'", token)
                        }
                        val value = valueToken.string.substringAfter(" ")

                        defineList[varName] = value
                    }

                    Tokens.LOOP_KEYWORD -> {
                        var nextToken = iterator.takeIf { it.hasNext() }?.next()
                        if (nextToken?.tokenType != Tokens.INT) htslCompileError(
                            "Expected integer after 'loop' keyword",
                            nextToken ?: token
                        )
                        loopAmount = nextToken.string.toIntOrNull() ?: 1

                        nextToken = iterator.takeIf { it.hasNext() }?.next()
                        if (nextToken?.tokenType != Tokens.STRING) htslCompileError(
                            "Expected loop variable name after loop amount",
                            nextToken ?: token
                        )
                        loopVarName = nextToken.string
                        nextToken = iterator.takeIf { it.hasNext() }?.next()
                        if (nextToken?.tokenType != Tokens.DEPTH_ADD) htslCompileError(
                            "Expected '{' after loop variable name",
                            nextToken ?: token
                        )
                        if (loopAmount <= 0) continue

                        var depth = 1
                        val tokens = mutableListOf<TokenWithPosition>()

                        while (iterator.hasNext()) {
                            val loopToken = iterator.next()
                            when (loopToken.tokenType) {
                                Tokens.DEPTH_ADD -> depth++
                                Tokens.DEPTH_SUBTRACT -> if (--depth == 0) break
                            }
                            tokens.add(loopToken)
                        }

                        repeat(loopAmount) {
                            processedTokens.addAll(preProcess(tokens, loopVarName, it, defineList, context, scope))
                        }
                    }

                    Tokens.STRING -> {
                        var processedString = token.string
                        if (processedString == "\"\"") {
                            processedTokens.add(
                                TokenWithPosition(
                                    Token(
                                        "",
                                        token.startsAt,
                                        token.endsAt,
                                        Tokens.STRING,
                                    ),
                                    token.line,
                                    token.column
                                )
                            )
                            continue
                        }

                        if (token.string.startsWith("\"") && token.string.endsWith("\"")) {
                            processedString = token.string
                                .substring(1, token.string.length - 1) // Remove surrounding quotes
                                .replace("""\"""", "\"") // Unescape quotes

                            processedTokens.add(
                                TokenWithPosition(
                                    token.token.copy(string = processedString),
                                    token.line,
                                    token.column,
                                    quoted = true
                                )
                            )
                            continue
                        }

                        for ((key, value) in defineList) {
                            val regex =
                                Regex("(?<!\")\\b${Regex.escape(key)}\\b(?!\")") // Match whole words not inside quotes
                            processedString = processedString.replace(regex, value)
                        }
                        val loopVarNameRegex = loopVarName?.let { Regex("(?<!\")\\b${Regex.escape(it)}\\b(?!\")") }
                        if (loopVarNameRegex != null) {
                            processedString = processedString.replace(loopVarNameRegex, loopIndex.toString())
                        }


                        processedTokens.addAll(retokenizeAtOriginalPosition(processedString, true, token))
                    }

                    else -> {
                        processedTokens.add(token)
                    }
                }
            }
            return processedTokens
        } finally {
            if (reusedContext == null) {
                Context.exit()
            }
        }
    }
}