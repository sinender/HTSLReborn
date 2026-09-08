package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.Parser
import llc.redstone.htslreborn.parser.PreProcess
import llc.redstone.htslreborn.tokenizer.Tokenizer
import java.nio.file.Paths
import kotlin.io.path.Path

// Used primarily for testing the tokenizer and preprocessor
fun main(args: Array<String>) {
    val homePath = Paths.get(System.getProperty("user.home"))
    val path = homePath.resolve("Desktop/test.htsl")
    val tokens = Tokenizer.tokenize(path)
    println("Tokens:")
    tokens.forEach { println("${it.tokenType} -> ${it.string}") }

    val preProcessedTokens = PreProcess.preProcess(tokens)
    println("\nPre-Processed Tokens:")
    preProcessedTokens.forEach { println("${it.tokenType} -> ${it.string} (${it.startsAt}-${it.endsAt})") }

    val parser = Parser.parse(preProcessedTokens, Path("test.htsl"))
    println("\nParsed Actions:")
    println(parser)
}