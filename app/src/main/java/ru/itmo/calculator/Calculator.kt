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

data class CalculatorState(
    val entry: String = ZERO,
    val accumulator: Double = 0.0,
    val operation: Operation = Operation.NONE,
    val startNew: Boolean = true,
) {
    val hasRightOperand: Boolean
        get() = operation != Operation.NONE && !startNew

    private val signOnly: Boolean
        get() = entry == NEGATIVE

    private val entryValue: Double
        get() = entry.toDoubleOrNull() ?: 0.0

    val preview: String
        get() {
            if (!hasRightOperand || signOnly) return ""
            return computePending().entry
        }

    val result: String
        get() = when {
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
        op == Operation.MINUS && startsNegativeNumber() -> copy(entry = NEGATIVE, startNew = false)
        signOnly -> dropSign().inputOperation(op)
        hasRightOperand -> computePending().copy(operation = op, startNew = true)
        operation == Operation.NONE -> copy(accumulator = entryValue, operation = op, startNew = true)
        else -> copy(operation = op)
    }

    fun equals(): CalculatorState {
        if (operation == Operation.NONE || signOnly) return this
        return computePending().copy(operation = Operation.NONE, startNew = true)
    }

    fun clear(): CalculatorState = CalculatorState()

    fun clearEntry(): CalculatorState = copy(entry = ZERO, startNew = true)

    private fun startTyping(): CalculatorState = when {
        startNew -> copy(entry = ZERO, startNew = false)
        else -> this
    }

    // Минус в начале числа (сразу после запуска, CE или знака операции) делает число отрицательным
    private fun startsNegativeNumber(): Boolean =
        signOnly || (startNew && (operation != Operation.NONE || entry == ZERO))

    private fun dropSign(): CalculatorState = copy(entry = ZERO, startNew = true)

    // Считаем строго по правилам double: 1÷0 = Infinity, 0÷0 = NaN
    private fun computePending(): CalculatorState {
        val value = operation.apply(accumulator, entryValue)
        return copy(accumulator = value, entry = format(value))
    }

    companion object {
        private const val ZERO = "0"
        private const val DOT = '.'
        private const val NEGATIVE = "-"
        private const val MAX_DIGITS = 15
        private const val SIGNIFICANT_DIGITS = 12
        private const val MAX_PLAIN = 1e15
        private const val MIN_PLAIN = 1e-6

        fun format(value: Double): String {
            if (value.isNaN() || value.isInfinite()) return value.toString()
            if (value == 0.0) return ZERO
            val rounded = BigDecimal(value).round(MathContext(SIGNIFICANT_DIGITS)).stripTrailingZeros()
            val magnitude = abs(value)
            return if (magnitude >= MAX_PLAIN || magnitude < MIN_PLAIN) rounded.toString() else rounded.toPlainString()
        }
    }
}
