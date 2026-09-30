package ru.itmo.calculator

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

enum class Operation {
    NONE, PLUS, MINUS, MULTIPLY, DIVIDE;

    fun apply(left: Double, right: Double): Double = when (this) {
        PLUS -> left + right
        MINUS -> left - right
        MULTIPLY -> left * right
        DIVIDE -> left / right
        NONE -> right
    }
}

enum class CalcError { NONE, DIVISION_BY_ZERO, OVERFLOW }

data class CalculatorState(
    val entry: String = ZERO,
    val accumulator: Double = 0.0,
    val operation: Operation = Operation.NONE,
    val startNew: Boolean = true,
    val error: CalcError = CalcError.NONE,
) {
    val hasError: Boolean
        get() = error != CalcError.NONE

    val hasRightOperand: Boolean
        get() = operation != Operation.NONE && !startNew

    private val signOnly: Boolean
        get() = entry == NEGATIVE

    private val entryValue: Double
        get() = entry.toDoubleOrNull() ?: 0.0

    val preview: String
        get() {
            if (hasError || !hasRightOperand || signOnly) return ""
            val next = computePending()
            return if (next.hasError) "" else next.entry
        }

    val result: String
        get() = when {
            hasError -> ""
            hasRightOperand -> preview
            operation != Operation.NONE -> format(accumulator)
            else -> format(entryValue)
        }

    fun inputDigit(digit: Int): CalculatorState {
        val base = startTyping()
        if (base.entry.count { it.isDigit() } >= MAX_DIGITS) return base
        val entry = when (base.entry) {
            ZERO -> digit.toString()
            NEGATIVE + ZERO -> NEGATIVE + digit
            else -> base.entry + digit
        }
        return base.copy(entry = entry)
    }

    fun inputDot(): CalculatorState {
        val base = startTyping()
        return when {
            base.entry.contains(DOT) -> base
            base.signOnly -> base.copy(entry = NEGATIVE + ZERO + DOT)
            else -> base.copy(entry = base.entry + DOT)
        }
    }

    fun inputOperation(op: Operation): CalculatorState = when {
        hasError -> this
        op == Operation.MINUS && startsNegativeNumber() -> copy(entry = NEGATIVE, startNew = false)
        signOnly -> dropSign().inputOperation(op)
        hasRightOperand -> computePending().let { if (it.hasError) it else it.copy(operation = op, startNew = true) }
        operation == Operation.NONE -> copy(accumulator = entryValue, operation = op, startNew = true)
        else -> copy(operation = op)
    }

    fun equals(): CalculatorState {
        if (hasError || operation == Operation.NONE || signOnly) return this
        val next = computePending()
        return if (next.hasError) next else next.copy(operation = Operation.NONE, startNew = true)
    }

    fun clear(): CalculatorState = CalculatorState()

    fun clearEntry(): CalculatorState =
        if (hasError) clear() else copy(entry = ZERO, startNew = true)

    private fun startTyping(): CalculatorState = when {
        hasError -> CalculatorState(startNew = false)
        startNew -> copy(entry = ZERO, startNew = false)
        else -> this
    }

    // Минус в начале числа (сразу после запуска, CE или знака операции) делает число отрицательным
    private fun startsNegativeNumber(): Boolean =
        signOnly || (startNew && (operation != Operation.NONE || entry == ZERO))

    private fun dropSign(): CalculatorState = copy(entry = ZERO, startNew = true)

    private fun computePending(): CalculatorState {
        val right = entryValue
        // 0÷0 в double — это NaN, а не ошибка; ошибкой остаётся только деление ненулевого числа на 0
        if (operation == Operation.DIVIDE && right == 0.0 && accumulator != 0.0 && !accumulator.isNaN()) {
            return copy(error = CalcError.DIVISION_BY_ZERO)
        }
        val value = operation.apply(accumulator, right)
        if (value.isInfinite()) return copy(error = CalcError.OVERFLOW)
        return copy(accumulator = value, entry = format(value))
    }

    companion object {
        private const val ZERO = "0"
        private const val DOT = '.'
        private const val NEGATIVE = "-"
        private const val NAN = "NaN"
        private const val MAX_DIGITS = 15
        private const val SIGNIFICANT_DIGITS = 12
        private const val MAX_PLAIN = 1e15
        private const val MIN_PLAIN = 1e-6

        fun format(value: Double): String {
            if (value.isNaN()) return NAN
            if (value == 0.0) return ZERO
            val rounded = BigDecimal(value).round(MathContext(SIGNIFICANT_DIGITS)).stripTrailingZeros()
            val magnitude = abs(value)
            return if (magnitude >= MAX_PLAIN || magnitude < MIN_PLAIN) rounded.toString() else rounded.toPlainString()
        }
    }
}
