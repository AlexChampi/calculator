package ru.itmo.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

private val CalculatorStateSaver = listSaver<CalculatorState, Any>(
    save = { listOf(it.entry, it.accumulator, it.operation.name, it.startNew) },
    restore = {
        CalculatorState(
            entry = it[0] as String,
            accumulator = it[1] as Double,
            operation = Operation.valueOf(it[2] as String),
            startNew = it[3] as Boolean,
        )
    },
)

private class KeypadActions(
    val onDigit: (Int) -> Unit,
    val onDot: () -> Unit,
    val onOperation: (Operation) -> Unit,
    val onEquals: () -> Unit,
    val onClear: () -> Unit,
    val onClearEntry: () -> Unit,
    val onCopy: () -> Unit,
)

private data class KeySizes(
    val width: Dp,
    val height: Dp,
    val columnGap: Dp,
    val rowGap: Dp,
    val digit: TextUnit,
    val operation: TextUnit,
    val clearEntry: TextUnit,
    val equals: TextUnit,
    val icon: Dp,
)

private data class DisplaySizes(
    val expressionMax: TextUnit,
    val preview: TextUnit,
    val gap: Dp,
    val padding: Dp,
)

const val RESULT_TAG = "result"

private const val COLUMNS = 4
private const val ROWS = 5
private val MinTextSize = 16.sp

@Composable
fun CalculatorScreen() {
    var state by rememberSaveable(stateSaver = CalculatorStateSaver) { mutableStateOf(CalculatorState()) }
    val context = LocalContext.current
    val clipLabel = stringResource(R.string.app_name)
    val copiedMessage = stringResource(R.string.copied)

    val copy = {
        val result = state.result
        if (result.isNotEmpty()) copyToClipboard(context, clipLabel, result, copiedMessage)
    }
    val actions = KeypadActions(
        onDigit = { state = state.inputDigit(it) },
        onDot = { state = state.inputDot() },
        onOperation = { state = state.inputOperation(it) },
        onEquals = { state = state.equals() },
        onClear = { state = state.clear() },
        onClearEntry = { state = state.clearEntry() },
        onCopy = copy,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        containerColor = LeafColors.Surface,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        BoxWithConstraints(Modifier.padding(innerPadding)) {
            if (maxWidth > maxHeight) {
                LandscapeLayout(state, actions, maxWidth, maxHeight)
            } else {
                PortraitLayout(state, actions, maxWidth, maxHeight)
            }
        }
    }
}

@Composable
private fun PortraitLayout(state: CalculatorState, actions: KeypadActions, width: Dp, height: Dp) {
    val columnGap = 18.dp
    val rowGap = 8.dp
    val keySize = min(
        76.dp,
        min((width - 32.dp - columnGap * (COLUMNS - 1)) / COLUMNS, (height * 0.6f - 40.dp - rowGap * (ROWS - 1)) / ROWS),
    )
    val scale = keySize / 76.dp
    val sizes = KeySizes(
        width = keySize,
        height = keySize,
        columnGap = columnGap,
        rowGap = rowGap,
        digit = (32 * scale).sp,
        operation = (36 * scale).sp,
        clearEntry = (28 * scale).sp,
        equals = (40 * scale).sp,
        icon = 30.dp * scale,
    )
    Column(Modifier.fillMaxSize()) {
        Display(
            state = state,
            sizes = DisplaySizes(expressionMax = 48.sp, preview = 32.sp, gap = 20.dp, padding = 24.dp),
            onCopy = actions.onCopy,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = LeafColors.KeyPressed)
        Keypad(
            state = state,
            actions = actions,
            sizes = sizes,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun LandscapeLayout(state: CalculatorState, actions: KeypadActions, width: Dp, height: Dp) {
    val columnGap = 12.dp
    val rowGap = 8.dp
    val keypadWidth = min(440.dp, width * 0.55f)
    val sizes = KeySizes(
        width = (keypadWidth - columnGap * (COLUMNS - 1)) / COLUMNS,
        height = min(64.dp, (height - 32.dp - rowGap * (ROWS - 1)) / ROWS),
        columnGap = columnGap,
        rowGap = rowGap,
        digit = 24.sp,
        operation = 28.sp,
        clearEntry = 22.sp,
        equals = 30.sp,
        icon = 24.dp,
    )
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Display(
            state = state,
            sizes = DisplaySizes(expressionMax = 40.sp, preview = 26.sp, gap = 12.dp, padding = 8.dp),
            onCopy = actions.onCopy,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        VerticalDivider(color = LeafColors.KeyPressed)
        Keypad(
            state = state,
            actions = actions,
            sizes = sizes,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun Display(state: CalculatorState, sizes: DisplaySizes, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val currentOnCopy by rememberUpdatedState(onCopy)
    val copyLabel = stringResource(R.string.copy_action)
    val expression = expressionText(state, operationSymbol(state.operation))

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnCopy()
                })
            }
            .semantics {
                onLongClick(label = copyLabel) {
                    currentOnCopy()
                    true
                }
            }
            .padding(sizes.padding),
        verticalArrangement = Arrangement.spacedBy(sizes.gap, Alignment.Bottom),
        horizontalAlignment = Alignment.End,
    ) {
        BasicText(
            text = expression,
            modifier = Modifier.fillMaxWidth().testTag(RESULT_TAG),
            style = numberStyle(LeafColors.OnKey),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.StartEllipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = MinTextSize, maxFontSize = sizes.expressionMax),
        )
        BasicText(
            text = groupDigits(state.preview),
            modifier = Modifier.fillMaxWidth(),
            style = numberStyle(LeafColors.Preview),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = MinTextSize, maxFontSize = sizes.preview),
        )
    }
}

@Composable
private fun Keypad(state: CalculatorState, actions: KeypadActions, sizes: KeySizes, modifier: Modifier = Modifier) {
    val rowArrangement = Arrangement.spacedBy(sizes.columnGap)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(sizes.rowGap)) {
        Row(horizontalArrangement = rowArrangement) {
            GlyphKey(R.string.key_clear, R.string.cd_clear, LeafColors.Clear, sizes.digit, sizes, actions.onClear)
            GlyphKey(R.string.key_clear_entry, R.string.cd_clear_entry, LeafColors.Clear, sizes.clearEntry, sizes, actions.onClearEntry)
            CopyKey(enabled = state.result.isNotEmpty(), sizes = sizes, onClick = actions.onCopy)
            OperationKey(Operation.DIVIDE, sizes, actions)
        }
        DigitRow(7, Operation.MULTIPLY, sizes, actions)
        DigitRow(4, Operation.MINUS, sizes, actions)
        DigitRow(1, Operation.PLUS, sizes, actions)
        Row(horizontalArrangement = rowArrangement) {
            Key(
                onClick = { actions.onDigit(0) },
                width = sizes.width * 2 + sizes.columnGap,
                height = sizes.height,
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.width(sizes.width), contentAlignment = Alignment.Center) {
                    KeyGlyph("0", LeafColors.OnKey, sizes.digit)
                }
            }
            GlyphKey(R.string.key_dot, R.string.cd_dot, LeafColors.OnKey, sizes.digit, sizes, actions.onDot)
            Key(
                onClick = actions.onEquals,
                width = sizes.width,
                height = sizes.height,
                fill = LeafColors.Equals,
                description = stringResource(R.string.cd_equals),
            ) {
                KeyGlyph(stringResource(R.string.key_equals), LeafColors.OnEquals, sizes.equals)
            }
        }
    }
}

@Composable
private fun DigitRow(first: Int, operation: Operation, sizes: KeySizes, actions: KeypadActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(sizes.columnGap)) {
        for (digit in first..first + 2) {
            Key(onClick = { actions.onDigit(digit) }, width = sizes.width, height = sizes.height) {
                KeyGlyph(digit.toString(), LeafColors.OnKey, sizes.digit)
            }
        }
        OperationKey(operation, sizes, actions)
    }
}

@Composable
private fun OperationKey(operation: Operation, sizes: KeySizes, actions: KeypadActions) {
    Key(
        onClick = { actions.onOperation(operation) },
        width = sizes.width,
        height = sizes.height,
        description = stringResource(operationDescription(operation)),
    ) {
        KeyGlyph(operationSymbol(operation), LeafColors.Primary, sizes.operation)
    }
}

@Composable
private fun GlyphKey(
    @StringRes label: Int,
    @StringRes description: Int,
    color: Color,
    fontSize: TextUnit,
    sizes: KeySizes,
    onClick: () -> Unit,
) {
    Key(onClick = onClick, width = sizes.width, height = sizes.height, description = stringResource(description)) {
        KeyGlyph(stringResource(label), color, fontSize)
    }
}

@Composable
private fun CopyKey(enabled: Boolean, sizes: KeySizes, onClick: () -> Unit) {
    Key(
        onClick = onClick,
        width = sizes.width,
        height = sizes.height,
        enabled = enabled,
        description = stringResource(R.string.cd_copy),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_copy),
            contentDescription = null,
            modifier = Modifier.size(sizes.icon),
            tint = if (enabled) LeafColors.Icon else LeafColors.IconDisabled,
        )
    }
}

@Composable
private fun Key(
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    fill: Color = LeafColors.Key,
    enabled: Boolean = true,
    description: String = "",
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(width, height)
            .semantics { if (description.isNotEmpty()) contentDescription = description },
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        color = fill,
        contentColor = LeafColors.OnKey,
    ) {
        Box(contentAlignment = contentAlignment) { content() }
    }
}

@Composable
private fun KeyGlyph(text: String, color: Color, fontSize: TextUnit) {
    Text(text = text, color = color, fontSize = fontSize, fontWeight = FontWeight.Light, maxLines = 1)
}

private fun numberStyle(color: Color) = TextStyle(color = color, fontWeight = FontWeight.Light, textAlign = TextAlign.End)

private fun expressionText(state: CalculatorState, symbol: String): AnnotatedString = buildAnnotatedString {
    if (state.operation == Operation.NONE) {
        append(groupDigits(state.entry))
    } else {
        append(groupDigits(CalculatorState.format(state.accumulator)))
        withStyle(SpanStyle(color = LeafColors.Primary)) { append(symbol) }
        if (!state.startNew) append(groupDigits(state.entry))
    }
}

@Composable
private fun operationSymbol(operation: Operation): String = when (operation) {
    Operation.PLUS -> stringResource(R.string.op_plus)
    Operation.MINUS -> stringResource(R.string.op_minus)
    Operation.MULTIPLY -> stringResource(R.string.op_multiply)
    Operation.DIVIDE -> stringResource(R.string.op_divide)
    Operation.NONE -> ""
}

@StringRes
private fun operationDescription(operation: Operation): Int = when (operation) {
    Operation.PLUS -> R.string.cd_plus
    Operation.MINUS -> R.string.cd_minus
    Operation.MULTIPLY -> R.string.cd_multiply
    Operation.DIVIDE, Operation.NONE -> R.string.cd_divide
}

private fun copyToClipboard(context: Context, label: String, text: String, copiedMessage: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PortraitPreview() {
    LeafTheme { CalculatorScreen() }
}

@Preview(widthDp = 844, heightDp = 390)
@Composable
private fun LandscapePreview() {
    LeafTheme { CalculatorScreen() }
}
