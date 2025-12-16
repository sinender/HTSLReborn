package llc.redstone.htslreborn.utils

import guru.zoroark.tegral.niwen.lexer.Token
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition

object ErrorUtils {
    fun htslCompileError(message: String, token: TokenWithPosition): Nothing {
        val errorMessage = "HTSL Compile Error at line ${token.line}, column ${token.column}\n- $message"
        throw HTSLCompileException(errorMessage)
    }

    class HTSLCompileException(message: String) : Exception(message)
}