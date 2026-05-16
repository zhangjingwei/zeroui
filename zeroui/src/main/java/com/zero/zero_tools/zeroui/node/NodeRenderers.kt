package com.zero.zero_tools.zeroui.node

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zero.zero_tools.zeroui.R
import com.zero.zero_tools.zeroui.core.LocalZeroUiUnknownNodeHandler
import com.zero.zero_tools.zeroui.core.ZeroUiRenderer
import com.zero.zero_tools.zeroui.image.LocalZeroImageLoader
import com.zero.zero_tools.zeroui.image.ZeroImageRequest
import com.zero.zero_tools.zeroui.image.ZeroImageResult
import com.zero.zero_tools.zeroui.image.ZeroImageSource
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.skin.LocalZeroSkin
import com.zero.zero_tools.zeroui.skin.LocalZeroStyleResolver
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.getBoolean
import com.zero.zero_tools.zeroui.state.getList
import com.zero.zero_tools.zeroui.state.getText
import com.zero.zero_tools.zeroui.state.withItemScope
import com.zero.zero_tools.zeroui.text.TextStyle
import com.zero.zero_tools.zeroui.text.Tone
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.matches
import com.zero.zero_tools.zeroui.value.resolve
import com.zero.zero_tools.zeroui.value.toColor
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
@OptIn(ExperimentalLayoutApi::class)
internal fun RenderRowNode(
    node: Node.Row,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    FlowRow(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(node.spacing.dp),
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
internal fun RenderLazyColumnNode(
    node: Node.LazyColumn,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val items = node.itemsKey?.let(state::getList).orEmpty()

    LazyColumn(
        modifier = modifier.then(node.layout.toModifier()),
        verticalArrangement = Arrangement.spacedBy(node.spacing.dp)
    ) {
        node.children.forEach { child ->
            item {
                ZeroUiRenderer(
                    node = child,
                    state = state,
                    onInteraction = onInteraction
                )
            }
        }

        val itemNode = node.item
        if (itemNode != null) {
            itemsIndexed(items) { index, item ->
                val scopedState = state.withItemScope(item, index)
                ZeroUiRenderer(
                    node = itemNode,
                    state = scopedState,
                    onInteraction = onInteraction
                )
            }
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
internal fun RenderImageNode(
    node: Node.Image,
    state: State,
    modifier: Modifier
) {
    val resolver = LocalZeroStyleResolver.current
    val loader = LocalZeroImageLoader.current
    val source = node.source.toZeroImageSource(state)

    var result by remember(source) { mutableStateOf<ZeroImageResult?>(null) }

    DisposableEffect(source, loader) {
        result = null
        val cancelable = loader.load(ZeroImageRequest(source = source)) { outcome ->
            result = outcome
        }
        onDispose { cancelable.cancel() }
    }

    val shape = RoundedCornerShape(node.cornerRadius.dp)
    val imageModifier = modifier
        .then(node.layout.toModifier())
        .then(node.aspectRatio?.let { Modifier.aspectRatio(it) } ?: Modifier)
        .clip(shape)

    when (val current = result) {
        is ZeroImageResult.Success -> Image(
            bitmap = current.bitmap,
            contentDescription = node.contentDescription,
            contentScale = node.contentScale.toComposeContentScale(),
            modifier = imageModifier
        )
        ZeroImageResult.Unavailable,
        null -> Box(
            modifier = imageModifier.background(resolver.unknownContainerColor),
            contentAlignment = Alignment.Center
        ) {
            val placeholderText = if (current == null) {
                stringResource(R.string.zero_ui_image_loading)
            } else {
                stringResource(R.string.zero_ui_image_unavailable)
            }
            Text(
                text = placeholderText,
                style = resolver.supportTextStyle,
                color = resolver.mutedContentColor,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
internal fun RenderTextFieldNode(
    node: Node.TextField,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val tokens = LocalZeroStyleResolver.current.fieldTokens()
    OutlinedTextField(
        value = node.value.resolve(state),
        onValueChange = { value ->
            onInteraction(node.onValueChange, Value.Text(value))
        },
        label = { Text(text = node.label) },
        singleLine = true,
        modifier = modifier
            .then(node.layout.toModifier())
            .heightIn(min = tokens.minHeight),
        shape = RoundedCornerShape(tokens.cornerRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = tokens.colors.content,
            unfocusedTextColor = tokens.colors.content,
            disabledTextColor = tokens.colors.disabledContent,
            focusedContainerColor = tokens.colors.container,
            unfocusedContainerColor = tokens.colors.container,
            disabledContainerColor = tokens.colors.container,
            cursorColor = tokens.colors.cursor,
            focusedBorderColor = tokens.colors.focusedOutline,
            unfocusedBorderColor = tokens.colors.unfocusedOutline,
            disabledBorderColor = tokens.colors.disabledOutline,
            errorBorderColor = tokens.colors.errorOutline,
            focusedLabelColor = tokens.colors.focusedOutline,
            unfocusedLabelColor = tokens.colors.label,
            disabledLabelColor = tokens.colors.disabledContent,
            errorLabelColor = tokens.colors.errorOutline
        )
    )
}

@Composable
internal fun RenderSwitchNode(
    node: Node.Switch,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val tokens = LocalZeroStyleResolver.current.switchTokens()
    Row(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Switch(
            checked = state.getBoolean(node.checkedKey),
            onCheckedChange = { checked ->
                onInteraction(node.onCheckedChange, Value.Bool(checked))
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.checked.thumb,
                checkedTrackColor = tokens.checked.track,
                checkedBorderColor = tokens.checked.border,
                uncheckedThumbColor = tokens.unchecked.thumb,
                uncheckedTrackColor = tokens.unchecked.track,
                uncheckedBorderColor = tokens.unchecked.border,
                disabledCheckedThumbColor = tokens.checked.disabledThumb,
                disabledCheckedTrackColor = tokens.checked.disabledTrack,
                disabledCheckedBorderColor = tokens.checked.disabledBorder,
                disabledUncheckedThumbColor = tokens.unchecked.disabledThumb,
                disabledUncheckedTrackColor = tokens.unchecked.disabledTrack,
                disabledUncheckedBorderColor = tokens.unchecked.disabledBorder
            )
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
    val resolver = LocalZeroStyleResolver.current
    Row(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(node.spacing.dp)
    ) {
        node.options.forEach { option ->
            val selected = state.getText(node.selectedKey) == option.value
            val tokens = resolver.chipTokens(selected)
            FilterChip(
                selected = selected,
                onClick = {
                    onInteraction(node.onSelected, Value.Text(option.value))
                },
                label = { Text(text = option.label) },
                modifier = Modifier.heightIn(min = tokens.minHeight),
                shape = RoundedCornerShape(tokens.cornerRadius),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = tokens.unselected.container,
                    labelColor = tokens.unselected.content,
                    disabledContainerColor = tokens.unselected.disabledContainer,
                    disabledLabelColor = tokens.unselected.disabledContent,
                    selectedContainerColor = tokens.selected.container,
                    selectedLabelColor = tokens.selected.content,
                    disabledSelectedContainerColor = tokens.selected.disabledContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = tokens.unselected.outline,
                    selectedBorderColor = tokens.selected.outline,
                    disabledBorderColor = tokens.unselected.disabledOutline,
                    disabledSelectedBorderColor = tokens.selected.disabledOutline
                )
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
    val tokens = LocalZeroStyleResolver.current.cardTokens(node.tone)

    Card(
        modifier = modifier.then(node.layout.toModifier()),
        colors = CardDefaults.cardColors(
            containerColor = tokens.colors.container,
            contentColor = tokens.colors.content
        ),
        shape = RoundedCornerShape(tokens.cornerRadius)
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
internal fun RenderDialogNode(
    node: Node.Dialog,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit
) {
    if (!state.getBoolean(node.visibleKey)) return

    Dialog(onDismissRequest = { onInteraction(node.onDismiss, null) }) {
        val resolver = LocalZeroStyleResolver.current
        val tokens = resolver.cardTokens(Tone.Default)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = tokens.colors.container,
                contentColor = tokens.colors.content
            ),
            shape = RoundedCornerShape(tokens.cornerRadius)
        ) {
            Column(
                modifier = Modifier.padding(node.padding.dp),
                verticalArrangement = Arrangement.spacedBy(node.spacing.dp)
            ) {
                node.title?.let { title ->
                    Text(
                        text = title.resolve(state),
                        style = resolver.textStyle(TextStyle.SectionTitle)
                    )
                }
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

private fun ImageSource.toZeroImageSource(state: State): ZeroImageSource {
    return when (this) {
        is ImageSource.Resource -> ZeroImageSource.Resource(name)
        is ImageSource.Url -> ZeroImageSource.Url(value)
        is ImageSource.Binding -> ZeroImageSource.Url(state.getText(key).ifBlank { fallback })
    }
}

private fun ImageContentScale.toComposeContentScale(): ContentScale {
    return when (this) {
        ImageContentScale.Fit -> ContentScale.Fit
        ImageContentScale.Crop -> ContentScale.Crop
        ImageContentScale.FillWidth -> ContentScale.FillWidth
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
    LocalZeroUiUnknownNodeHandler.current?.invoke(node.typeName, node.raw)

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
                text = stringResource(R.string.zero_ui_unknown_node_title, node.typeName),
                style = resolver.labelTextStyle,
                color = resolver.errorContentColor
            )
            Text(
                text = stringResource(R.string.zero_ui_unknown_node_support),
                style = resolver.supportTextStyle,
                color = resolver.mutedContentColor
            )
        }
    }
}
