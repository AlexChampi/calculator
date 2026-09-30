package ru.itmo.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun divisionByZeroIsErrorNotCrash() {
        val state = calc("5/0=")
        assertEquals(CalcError.DIVISION_BY_ZERO, state.error)
        assertEquals("", state.result)
        assertEquals("7", state.inputDigit(7).entry)
    }

    @Test
    fun divisionByZeroKeepsExpressionOnScreen() {
        val state = calc("12/0=")
        assertEquals(12.0, state.accumulator, 0.0)
        assertEquals(Operation.DIVIDE, state.operation)
        assertEquals("0", state.entry)
    }

    @Test
    fun overflowIsError() {
        var state = calc("999999999999999")
        while (!state.hasError) state = state.type("*999999999999999=")
        assertEquals(CalcError.OVERFLOW, state.error)
    }

    @Test
    fun clearEntryKeepsOperation() {
        val state = calc("9-4").clearEntry().type("1=")
        assertEquals("8", state.entry)
    }

    @Test
    fun clearEntryAfterErrorClearsAll() {
        val state = calc("5/0=").clearEntry()
        assertFalse(state.hasError)
        assertEquals(CalculatorState(), state)
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
        assertEquals("−1,000", groupDigits("-1000"))
        assertEquals("999", groupDigits("999"))
        assertEquals("1.5E+20", groupDigits("1.5E+20"))
    }

    @Test
    fun resultSurvivesAsDouble() {
        assertTrue(calc("7/2=").entry.toDouble() == 3.5)
    }
}
