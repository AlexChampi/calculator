package ru.itmo.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorTest {

    private fun CalculatorState.type(text: String): CalculatorState = text.fold(this) { state, ch ->
        when (ch) {
            in '0'..'9' -> state.inputDigit(ch - '0')
            '.' -> state.inputDot()
            '=' -> state.equals()
            '+' -> state.inputOperation(Operation.PLUS)
            '-' -> state.inputOperation(Operation.MINUS)
            '*' -> state.inputOperation(Operation.MULTIPLY)
            '/' -> state.inputOperation(Operation.DIVIDE)
            else -> error("Unknown key $ch")
        }
    }

    private fun calc(text: String) = CalculatorState().type(text)

    @Test
    fun addsDoubles() {
        assertEquals("0.3", calc("0.1+0.2=").entry)
    }

    @Test
    fun chainsOperationsLeftToRight() {
        assertEquals("20", calc("2+3*4=").entry)
    }

    @Test
    fun divisionByZeroIsInfinity() {
        assertEquals("Infinity", calc("1/0=").entry)
        assertEquals("-Infinity", calc("-1/0=").entry)
        assertEquals("7", calc("5/0=").inputDigit(7).entry)
    }

    @Test
    fun overflowIsInfinity() {
        var state = calc("999999999999999")
        while (state.entry != "Infinity") state = state.type("*999999999999999=")
        assertEquals(Double.POSITIVE_INFINITY, state.entry.toDouble(), 0.0)
    }

    @Test
    fun clearEntryKeepsOperation() {
        val state = calc("9-4").clearEntry().type("1=")
        assertEquals("8", state.entry)
    }

    @Test
    fun clearEntryRemovesRightOperandFromExpression() {
        val state = calc("12+34").clearEntry()
        assertTrue(state.startNew)
        assertEquals(Operation.PLUS, state.operation)
        assertEquals("17", state.type("5=").entry)
    }

    @Test
    fun negativeNumberAtStart() {
        assertEquals("-5", calc("-5").entry)
        assertEquals("-2", calc("-5+3=").entry)
        assertEquals("-0.5", calc("-.5").entry)
    }

    @Test
    fun negativeNumberAfterOperation() {
        assertEquals("-15", calc("5*-3=").entry)
        assertEquals("8", calc("5--3=").entry)
        assertEquals("-2", calc("6/-3=").entry)
    }

    @Test
    fun minusAfterResultSubtracts() {
        assertEquals("2", calc("2+3=-3=").entry)
    }

    @Test
    fun zeroDividedByZeroIsNaN() {
        val state = calc("0/0=")
        assertEquals("NaN", state.entry)
        assertEquals("NaN", state.result)
        assertEquals("NaN", calc("0/0=+1=").entry)
    }

    @Test
    fun clearResetsAll() {
        assertEquals(CalculatorState(), calc("9+4").clear())
    }

    @Test
    fun secondDotIgnored() {
        assertEquals("1.23", calc("1.2.3").entry)
    }

    @Test
    fun replacesOperation() {
        assertEquals("12", calc("6+*2=").entry)
    }

    @Test
    fun limitsTypedDigits() {
        assertEquals("123456789012345", calc("1234567890123456789").entry)
    }

    @Test
    fun previewShowsPendingResult() {
        val state = calc("89552/620.5")
        assertEquals("144.322320709", state.preview)
        assertEquals("144.322320709", state.result)
    }

    @Test
    fun resultIsWhatCopyTakes() {
        assertEquals("12", calc("12").result)
        assertEquals("12", calc("12+").result)
        assertEquals("5", calc("2+3=").result)
        assertEquals("0.5", calc("0.50").result)
    }

    @Test
    fun formatsWithoutTailsAndHugeNumbersWithExponent() {
        assertEquals("0.3", CalculatorState.format(0.1 + 0.2))
        assertEquals("1E+20", CalculatorState.format(1e20))
        assertEquals("-2.5", CalculatorState.format(-2.5))
    }

    @Test
    fun groupsDigitsForDisplay() {
        assertEquals("89,552", groupDigits("89552"))
        assertEquals("1,234.50", groupDigits("1234.50"))
        assertEquals("-1,000", groupDigits("-1000"))
        assertEquals("999", groupDigits("999"))
        assertEquals("1.5E+20", groupDigits("1.5E+20"))
    }

    @Test
    fun resultSurvivesAsDouble() {
        assertTrue(calc("7/2=").entry.toDouble() == 3.5)
    }
}
