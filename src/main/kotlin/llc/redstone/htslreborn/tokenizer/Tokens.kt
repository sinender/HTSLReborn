package llc.redstone.htslreborn.tokenizer

import guru.zoroark.tegral.niwen.lexer.StateBuilder
import guru.zoroark.tegral.niwen.lexer.StateLabel
import guru.zoroark.tegral.niwen.lexer.TokenType
import guru.zoroark.tegral.niwen.lexer.matchers.TokenRecognizer
import guru.zoroark.tegral.niwen.lexer.matchers.anyOf
import guru.zoroark.tegral.niwen.lexer.matchers.matches
import org.intellij.lang.annotations.Language

enum class Tokens : TokenType {
    ACTION_KEYWORD,
    CONDITION_KEYWORD,
    INVERTED,
    INT,
    LONG,
    DOUBLE,
    STRING,
    PLACEHOLDER_STRING,
    QUOTE,
    PLACEHOLDER,
    COMMENT,
    NEWLINE,
    BOOLEAN,
    IF_AND_CONDITION_START,
    IF_OR_CONDITION_START,
    RANDOM_KEYWORD,
    GOTO_KEYWORD,
    IF_CONDITION_END,
    DEPTH_ADD,
    DEPTH_SUBTRACT,
    ELSE_KEYWORD,
    COMMA,
    JS_CODE,
    LOOP_KEYWORD,
    DEFINE_KEYWORD,
    COPY_KEYWORD,
    PASTE_KEYWORD,
    DEFINE_VALUE,
    NULL,
    SLOT_INDEX
    ;
}

enum class Operators : TokenType {
    UNSET,
    INCREMENT,
    DECREMENT,
    SET,
    MULTIPLY,
    DIVIDE,
    BITWISE_AND,
    BITWISE_OR,
    BITWISE_XOR,
    LEFT_SHIFT,
    ARITHMETIC_RIGHT_SHIFT,
    LOGICAL_RIGHT_SHIFT
}

enum class PlaceholderShortcuts : TokenType {
    GLOBAL_VAR,
    PLAYER_VAR,
    TEAM_VAR,
    RANDOM_INT,
    RANDOM_DOUBLE,
    HEALTH,
    MAX_HEALTH,
    HUNGER,
    LOC_X,
    LOC_Y,
    LOC_Z,
    UNIX
}

fun StateBuilder.placeholderShortcuts() {
    placeholderShortcuts("globalstat (?:\"([^\"\r\n]*)\"|([^ )\r\n]*))") isToken PlaceholderShortcuts.GLOBAL_VAR
    placeholderShortcuts("globalvar (?:\"([^\"\r\n]*)\"|([^ )\r\n]*))") isToken PlaceholderShortcuts.GLOBAL_VAR
    placeholderShortcuts("stat (?:\"([^\"\r\n]*)\"|([^ )\r\n]*))") isToken PlaceholderShortcuts.PLAYER_VAR
    placeholderShortcuts("var (?:\"([^\"\r\n]*)\"|([^ )\r\n]*))") isToken PlaceholderShortcuts.PLAYER_VAR
    placeholderShortcuts("teamstat +([^ )]*)? (?:\"([^\"\r\n]*)\"|([^ )\r\n]*))") isToken PlaceholderShortcuts.TEAM_VAR
    placeholderShortcuts("teamvar +([^ )]*)? (?:\"([^\"\r\n]*)\"|([^ )\r\n]*))") isToken PlaceholderShortcuts.TEAM_VAR
    placeholderShortcuts("randomint +([^ )]*)? +?([^ )]*)?") isToken PlaceholderShortcuts.RANDOM_INT
    placeholderShortcuts("randomdouble +([^ )]*)? +?([^ )]*)?") isToken PlaceholderShortcuts.RANDOM_DOUBLE
    placeholderShortcuts("health") isToken PlaceholderShortcuts.HEALTH
    placeholderShortcuts("maxhealth") isToken PlaceholderShortcuts.MAX_HEALTH
    placeholderShortcuts("hunger") isToken PlaceholderShortcuts.HUNGER
    placeholderShortcuts("locX") isToken PlaceholderShortcuts.LOC_X
    placeholderShortcuts("locY") isToken PlaceholderShortcuts.LOC_Y
    placeholderShortcuts("locZ") isToken PlaceholderShortcuts.LOC_Z
    placeholderShortcuts("unix") isToken PlaceholderShortcuts.UNIX
}

fun StateBuilder.operatorTokens() {
    word("unset") isToken Operators.UNSET
    "increment" isToken Operators.INCREMENT
    "decrement" isToken Operators.DECREMENT
    "multiply" isToken Operators.MULTIPLY
    "divide" isToken Operators.DIVIDE
    word("inc") isToken Operators.INCREMENT
    "+=" isToken Operators.INCREMENT
    word("dec") isToken Operators.DECREMENT
    "-=" isToken Operators.DECREMENT
    word("set") isToken Operators.SET
    "=" isToken Operators.SET
    word("mult") isToken Operators.MULTIPLY
    "*=" isToken Operators.MULTIPLY
    word("div") isToken Operators.DIVIDE
    "/=" isToken Operators.DIVIDE
    word("and") isToken Operators.BITWISE_AND
    "&=" isToken Operators.BITWISE_AND
    word("xor") isToken Operators.BITWISE_XOR
    "^=" isToken Operators.BITWISE_XOR
    word("or") isToken Operators.BITWISE_OR
    "|=" isToken Operators.BITWISE_OR
    word("shl") isToken Operators.LEFT_SHIFT
    word("shr") isToken Operators.ARITHMETIC_RIGHT_SHIFT
    word("lshr") isToken Operators.LOGICAL_RIGHT_SHIFT
    anyOf("leftShift", "<<=") isToken Operators.LEFT_SHIFT
    anyOf("arithmeticRightShift", ">>=") isToken Operators.ARITHMETIC_RIGHT_SHIFT
    anyOf("logicalRightShift", ">>>=") isToken Operators.LOGICAL_RIGHT_SHIFT
}

enum class Comparators : TokenType {
    EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    NOT_EQUALS
}

fun StateBuilder.comparatorTokens() {
    anyOf("==", "equals") isToken Comparators.EQUALS
    anyOf("<=", "lessThanOrEquals") isToken Comparators.LESS_THAN_OR_EQUAL
    anyOf("<", "lessThan") isToken Comparators.LESS_THAN
    anyOf(">=", "greaterThanOrEquals") isToken Comparators.GREATER_THAN_OR_EQUAL
    anyOf(">", "greaterThan") isToken Comparators.GREATER_THAN
    anyOf("!=", "notEquals") isToken Comparators.NOT_EQUALS
}

enum class States : StateLabel {
    PLACEHOLDER,
    PLACEHOLDER_CONDITION,
    IF_CONDITION,
    DEFINE,
    IN_MULTI_LINE_COMMENT
}

fun StateBuilder.word(word: String): TokenRecognizer {
    return matches("(?i)\\b$word\\b")
}

fun StateBuilder.placeholderShortcuts(@Language("RegExp") placeholderWithParameters: String): TokenRecognizer {
    return matches("(\\*=|=|/=|\\+=|-=|>|<|set|dec|mult|div|ment|inc|multiply|divide|equal|Less Than|Less Than or Equal|Greater Than|Greater Than or Equal) +$placeholderWithParameters")
}