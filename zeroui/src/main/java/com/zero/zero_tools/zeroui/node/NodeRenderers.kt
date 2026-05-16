package com.zero.zero_tools.zeroui.node

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zero.zero_tools.zeroui.core.ZeroUiRenderer
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.skin.LocalZeroSkin
import com.zero.zero_tools.zeroui.skin.LocalZeroStyleResolver
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.getBoolean
import com.zero.zero_tools.zeroui.state.getList
import com.zero.zero_tools.zeroui.state.getText
import com.zero.zero_tools.zeroui.state.withItemScope
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.matches
import com.zero.zero_tools.zeroui.value.resolve
import com.zero.zero_tools.zeroui.value.toColor
import com.zero.zero_tools.zeroui.value.toContainerColor
import com.zero.zero_tools.zeroui.value.toModifier
import com.zero.zero_tools.zeroui.value.toTextStyle

@Composable
internal fun RenderColumnNode(
    node: Node.Column,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.then(node.layout.toModifier()),
        verticalArrangement = Arrangement.spacedBy(node.spacing.dp)
    ) {
        node.children.forEach { child ->
            ZeroUiRenderer(
                node = child,
                state = state,
                onInteraction = onInteraction
            )
        }
    }
}

@Composable
internal fun RenderRowNode(
    node: Node.Row,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(node.spacing.dp)
    ) {
        node.children.forEach { child ->
            ZeroUiRenderer(
                node = child,
                state = state,
                onInteraction = onInteraction
            )
        }
    }
}

@Composable
internal fun RenderConditionNode(
    node: Node.Condition,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    if (node.condition.matches(state)) {
        ZeroUiRenderer(
            node = node.child,
            state = state,
            onInteraction = onInteraction,
            modifier = modifier
        )
    }
}

@Composable
internal fun RenderTextNode(
    node: Node.Text,
    state: State,
    modifier: Modifier
) {
    Text(
        text = node.text.resolve(state),
        style = node.style.toTextStyle(),
        color = node.tone.toColor(),
        modifier = modifier.then(node.layout.toModifier())
    )
}

@Composable
internal fun RenderTextFieldNode(
    node: Node.TextField,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    OutlinedTextField(
        value = node.value.resolve(state),
        onValueChange = { value ->
            onInteraction(node.onValueChange, Value.Text(value))
        },
        label = { Text(text = node.label) },
        singleLine = true,
        modifier = modifier.then(node.layout.toModifier())
    )
}

@Composable
internal fun RenderSwitchNode(
    node: Node.Switch,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Switch(
            checked = state.getBoolean(node.checkedKey),
            onCheckedChange = { checked ->
                onInteraction(node.onCheckedChange, Value.Bool(checked))
            }
        )
        Text(
            text = node.text,
            style = LocalZeroStyleResolver.current.bodyTextStyle,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
internal fun RenderButtonNode(
    node: Node.Button,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val content = @Composable {
        Text(text = node.text)
    }
    val tokens = LocalZeroStyleResolver.current.buttonTokens(node.variant)
    val colors = ButtonDefaults.buttonColors(
        containerColor = tokens.colors.container,
        contentColor = tokens.colors.content,
        disabledContainerColor = tokens.colors.disabledContainer,
        disabledContentColor = tokens.colors.disabledContent
    )
    val buttonModifier = modifier
        .then(node.layout.toModifier())
        .heightIn(min = tokens.minHeight)
    val contentPadding = PaddingValues(horizontal = tokens.horizontalPadding)
    val shape = RoundedCornerShape(tokens.cornerRadius)

    if (node.variant == ButtonVariant.Primary) {
        Button(
            onClick = { onInteraction(node.onClick, null) },
            modifier = buttonModifier,
            colors = colors,
            shape = shape,
            contentPadding = contentPadding,
            content = { content() }
        )
    } else {
        OutlinedButton(
            onClick = { onInteraction(node.onClick, null) },
            modifier = buttonModifier,
            colors = colors,
            shape = shape,
            contentPadding = contentPadding,
            content = { content() }
        )
    }
}

@Composable
internal fun RenderChipGroupNode(
    node: Node.ChipGroup,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(node.spacing.dp)
    ) {
        node.options.forEach { option ->
            FilterChip(
                selected = state.getText(node.selectedKey) == option.value,
                onClick = {
                    onInteraction(node.onSelected, Value.Text(option.value))
                },
                label = { Text(text = option.label) }
            )
        }
    }
}

@Composable
internal fun RenderCardNode(
    node: Node.Card,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val skin = LocalZeroSkin.current

    Card(
        modifier = modifier.then(node.layout.toModifier()),
        colors = CardDefaults.cardColors(
            containerColor = node.tone.toContainerColor()
        ),
        shape = RoundedCornerShape(skin.shapes.cardCornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(node.padding.dp),
            verticalArrangement = Arrangement.spacedBy(node.spacing.dp)
        ) {
            node.children.forEach { child ->
                ZeroUiRenderer(
                    node = child,
                    state = state,
                    onInteraction = onInteraction
                )
            }
        }
    }
}

@Composable
internal fun RenderSpacerNode(
    node: Node.Spacer,
    modifier: Modifier
) {
    Spacer(
        modifier = modifier
            .height(node.height.dp)
            .width(node.width.dp)
    )
}

@Composable
internal fun RenderForEachNode(
    node: Node.ForEach,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val items = state.getList(node.itemsKey)
    Column(
        modifier = modifier.then(node.layout.toModifier()),
        verticalArrangement = Arrangement.spacedBy(node.spacing.dp)
    ) {
        items.forEachIndexed { index, item ->
            val scopedState = remember(state, item, index) {
                state.withItemScope(item, index)
            }
            ZeroUiRenderer(
                node = node.child,
                state = scopedState,
                onInteraction = onInteraction
            )
        }
    }
}

private const val UnknownNodeLogTag = "ZeroUiUnknownNode"

@Composable
internal fun RenderUnknownNode(
    node: Node.Unknown,
    modifier: Modifier
) {
    val skin = LocalZeroSkin.current
    val resolver = LocalZeroStyleResolver.current

    // Non-fatal: emit a warning once per recomposition so the type name surfaces in logs,
    // and render a visible placeholder so designers/QA notice in-app.
    Log.w(UnknownNodeLogTag, "Unsupported ZeroUI node type: ${node.typeName}")

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = resolver.unknownContainerColor
        ),
        shape = RoundedCornerShape(skin.shapes.cardCornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(skin.spacing.unknownNodePadding),
            verticalArrangement = Arrangement.spacedBy(skin.spacing.unknownNodeSpacing)
        ) {
            Text(
                text = "未知组件: ${node.typeName}",
                style = resolver.labelTextStyle,
                color = resolver.errorContentColor
            )
            Text(
                text = "客户端不支持该 type，已用占位兜底",
                style = resolver.supportTextStyle,
                color = resolver.mutedContentColor
            )
        }
    }
}
