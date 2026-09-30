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

    val preview: String
        get() {
            if (hasError || !hasRightOperand) return ""
            val next = computePending()
            return if (next.hasError) "" else next.entry
        }

    val result: String
        get() = when {
            hasError -> ""
            hasRightOperand -> preview
            operation != Operation.NONE -> format(accumulator)
            else -> format(entry.toDouble())
        }

    fun inputDigit(digit: Int): CalculatorState {
        val base = startTyping()
        if (base.entry.count { it.isDigit() } >= MAX_DIGITS) return base
        return base.copy(entry = if (base.entry == ZERO) digit.toString() else base.entry + digit)
    }

    fun inputDot(): CalculatorState {
        val base = startTyping()
        return if (base.entry.contains(DOT)) base else base.copy(entry = base.entry + DOT)
    }

    fun inputOperation(op: Operation): CalculatorState = when {
        hasError -> this
        hasRightOperand -> computePending().let { if (it.hasError) it else it.copy(operation = op, startNew = true) }
        operation == Operation.NONE -> copy(accumulator = entry.toDouble(), operation = op, startNew = true)
        else -> copy(operation = op)
    }

    fun equals(): CalculatorState {
        if (hasError || operation == Operation.NONE) return this
        val next = computePending()
        return if (next.hasError) next else next.copy(operation = Operation.NONE, startNew = true)
    }

    fun clear(): CalculatorState = CalculatorState()

    fun clearEntry(): CalculatorState =
        if (hasError) clear() else copy(entry = ZERO, startNew = false)

    private fun startTyping(): CalculatorState = when {
        hasError -> CalculatorState(startNew = false)
        startNew -> copy(entry = ZERO, startNew = false)
        else -> this
    }

    private fun computePending(): CalculatorState {
        val right = entry.toDouble()
        if (operation == Operation.DIVIDE && right == 0.0) return copy(error = CalcError.DIVISION_BY_ZERO)
        val value = operation.apply(accumulator, right)
        if (value.isNaN() || value.isInfinite()) return copy(error = CalcError.OVERFLOW)
        return copy(accumulator = value, entry = format(value))
    }

    companion object {
        private const val ZERO = "0"
        private const val DOT = '.'
        private const val MAX_DIGITS = 15
        private const val SIGNIFICANT_DIGITS = 12
        private const val MAX_PLAIN = 1e15
        private const val MIN_PLAIN = 1e-6

        fun format(value: Double): String {
            if (value == 0.0) return ZERO
            val rounded = BigDecimal(value).round(MathContext(SIGNIFICANT_DIGITS)).stripTrailingZeros()
            val magnitude = abs(value)
            return if (magnitude >= MAX_PLAIN || magnitude < MIN_PLAIN) rounded.toString() else rounded.toPlainString()
        }
    }
}
