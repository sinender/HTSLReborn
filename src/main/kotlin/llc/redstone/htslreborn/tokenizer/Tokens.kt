package llc.redstone.htslreborn.tokenizer

import guru.zoroark.tegral.niwen.lexer.StateBuilder
import guru.zoroark.tegral.niwen.lexer.StateLabel
import guru.zoroark.tegral.niwen.lexer.TokenType
import guru.zoroark.tegral.niwen.lexer.matchers.anyOf

enum class Tokens: TokenType {
    ACTION_KEYWORD,
    CONDITION_KEYWORD,
    INVERTED,
    INT,
    LONG,
    DOUBLE,
    STRING,
    QUOTE,
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
    COMMA
}

enum class Operators: TokenType {
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

fun StateBuilder.operatorTokens() {
    "unset" isToken Operators.UNSET
    "increment" isToken Operators.INCREMENT
    "decrement" isToken Operators.DECREMENT
    "multiply" isToken Operators.MULTIPLY
    "divide" isToken Operators.DIVIDE
    anyOf("inc", "+=") isToken Operators.INCREMENT
    anyOf("dec", "-=") isToken Operators.DECREMENT
    anyOf("set", "=") isToken Operators.SET
    anyOf("mult", "*=") isToken Operators.MULTIPLY
    anyOf("div", "/=") isToken Operators.DIVIDE
    anyOf("and", "&=") isToken Operators.BITWISE_AND
    anyOf("xor", "^=") isToken Operators.BITWISE_XOR
    anyOf("or", "|=") isToken Operators.BITWISE_OR
    anyOf("leftShift", "shl", "<<=") isToken Operators.LEFT_SHIFT
    anyOf("arithmeticRightShift", "shr", ">>=") isToken Operators.ARITHMETIC_RIGHT_SHIFT
    anyOf("logicalRightShift", "lshr", ">>>=") isToken Operators.LOGICAL_RIGHT_SHIFT
}

enum class Comparators: TokenType {
    EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
}

fun StateBuilder.comparatorTokens() {
    anyOf("==", "equals") isToken Comparators.EQUALS
    anyOf("<=", "lessThanOrEquals") isToken Comparators.LESS_THAN_OR_EQUAL
    anyOf("<", "lessThan") isToken Comparators.LESS_THAN
    anyOf(">=", "greaterThanOrEquals") isToken Comparators.GREATER_THAN_OR_EQUAL
    anyOf(">", "greaterThan") isToken Comparators.GREATER_THAN
}

enum class States: StateLabel {
    IN_STRING,
    IN_CONDITION_STRING,
    IF_CONDITION
}