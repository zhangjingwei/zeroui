package com.zero.zero_tools.zeroui.value

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zero.zero_tools.zeroui.condition.Condition
import com.zero.zero_tools.zeroui.expression.evaluate
import com.zero.zero_tools.zeroui.page.Layout
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.asBoolean
import com.zero.zero_tools.zeroui.state.getBoolean
import com.zero.zero_tools.zeroui.state.getText
import com.zero.zero_tools.zeroui.text.Text
import com.zero.zero_tools.zeroui.text.TextStyle
import com.zero.zero_tools.zeroui.text.Tone
import com.zero.zero_tools.zeroui.skin.LocalZeroStyleResolver

@Composable
internal fun TextStyle.toTextStyle() = when (this) {
    TextStyle.Title,
    TextStyle.SectionTitle,
    TextStyle.Body,
    TextStyle.Label -> LocalZeroStyleResolver.current.textStyle(this)
}

@Composable
internal fun Tone.toColor() = LocalZeroStyleResolver.current.contentColor(this)

@Composable
internal fun Tone.toContainerColor() = LocalZeroStyleResolver.current.containerColor(this)

internal fun Layout.toModifier(): Modifier {
    var result: Modifier = Modifier

    if (fillMaxWidth) {
        result = result.fillMaxWidth()
    }

    if (padding > 0) {
        result = result.padding(padding.dp)
    }

    if (maxHeight > 0) {
        result = result.heightIn(max = maxHeight.dp)
    }

    return result
}

internal fun Text.resolve(state: State): String {
    return when (this) {
        is Text.Value -> value
        is Text.Binding -> {
            val value = state.getText(key).ifBlank { fallback }
            format?.replace("{value}", value) ?: value
        }
    }
}

internal fun Condition.matches(state: State): Boolean {
    return when (this) {
        is Condition.Truthy -> state.getBoolean(key)
        is Condition.NotBlank -> state.getText(key).isNotBlank()
        is Condition.Expr -> expression.evaluate(state).asBoolean()
    }
}
