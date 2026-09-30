package ru.itmo.calculator

private const val GROUP_SIZE = 3
private const val GROUP_SEPARATOR = ","
private const val MINUS_SIGN = "−"

fun groupDigits(number: String): String {
    val negative = number.startsWith('-')
    val body = number.removePrefix("-")
    val integerEnd = body.indexOfFirst { !it.isDigit() }.let { if (it == -1) body.length else it }
    val grouped = body.substring(0, integerEnd)
        .reversed()
        .chunked(GROUP_SIZE)
        .joinToString(GROUP_SEPARATOR)
        .reversed()
    return (if (negative) MINUS_SIGN else "") + grouped + body.substring(integerEnd)
}
