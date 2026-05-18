package com.zero.zero_tools.zeroui.node

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
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
import com.zero.zero_tools.zeroui.state.getNumber
import com.zero.zero_tools.zeroui.state.getText
import com.zero.zero_tools.zeroui.state.withItemScope
import com.zero.zero_tools.zeroui.text.TextStyle
import com.zero.zero_tools.zeroui.text.Tone
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
        verticalArrangement = Arrangement.spacedBy(node.spacing.dp),
        horizontalAlignment = node.horizontalAlignment.toComposeAlignment()
    ) {
        node.children.forEach { child ->
            ZeroUiRenderer(
                node = child,
                state = state,
                onInteraction = onInteraction,
                modifier = parentWeightModifier(child)
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
        horizontalArrangement = node.arrangement?.toComposeArrangement() ?: Arrangement.spacedBy(node.spacing.dp),
        verticalAlignment = node.verticalAlignment.toComposeAlignment()
    ) {
        node.children.forEach { child ->
            ZeroUiRenderer(
                node = child,
                state = state,
                onInteraction = onInteraction,
                modifier = parentRowChildModifier(child, node.verticalAlignment)
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

    val emptyNode = node.emptyChild
    if (emptyNode != null && items.isEmpty() && node.children.isEmpty()) {
        ZeroUiRenderer(node = emptyNode, state = state, onInteraction = onInteraction, modifier = modifier)
        return
    }

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
            if (items.isEmpty() && emptyNode != null) {
                item {
                    ZeroUiRenderer(node = emptyNode, state = state, onInteraction = onInteraction)
                }
            } else {
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
}

@Composable
internal fun RenderLazyRowNode(
    node: Node.LazyRow,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val items = node.itemsKey?.let(state::getList).orEmpty()

    val emptyNode = node.emptyChild
    if (emptyNode != null && items.isEmpty() && node.children.isEmpty()) {
        ZeroUiRenderer(node = emptyNode, state = state, onInteraction = onInteraction, modifier = modifier)
        return
    }

    LazyRow(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(node.spacing.dp),
        verticalAlignment = node.verticalAlignment.toComposeAlignment()
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
            if (items.isEmpty() && emptyNode != null) {
                item {
                    ZeroUiRenderer(node = emptyNode, state = state, onInteraction = onInteraction)
                }
            } else {
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
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val surfaceTone = node.surfaceTone
    val contentTone = node.tone ?: surfaceTone ?: Tone.Default
    val surfaceModifier = if (surfaceTone != null) {
        Modifier
            .background(
                color = surfaceTone.toContainerColor(),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    } else {
        Modifier
    }

    Text(
        text = node.text.resolve(state),
        style = node.style.toTextStyle(),
        color = contentTone.toColor(),
        modifier = modifier
            .then(node.layout.toModifier())
            .then(surfaceModifier)
            .then(node.onClick?.let { Modifier.clickable { onInteraction(it, null) } } ?: Modifier)
    )
}

@Composable
internal fun RenderImageNode(
    node: Node.Image,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
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
        .then(node.onClick?.let { Modifier.clickable { onInteraction(it, null) } } ?: Modifier)

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
internal fun RenderIconNode(
    node: Node.Icon,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
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

    val iconModifier = modifier
        .then(node.layout.toModifier())
        .size(node.size.dp)
        .then(node.onClick?.let { Modifier.clickable { onInteraction(it, null) } } ?: Modifier)

    when (val current = result) {
        is ZeroImageResult.Success -> Image(
            bitmap = current.bitmap,
            contentDescription = node.contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = if (node.tint) ColorFilter.tint(node.tone.toColor()) else null,
            modifier = iconModifier
        )
        ZeroImageResult.Unavailable,
        null -> Box(
            modifier = iconModifier.background(resolver.unknownContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "",
                style = resolver.supportTextStyle,
                color = resolver.mutedContentColor
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
    val enabled = state.isEnabled(node.enabledKey)
    OutlinedTextField(
        value = node.value.resolve(state),
        onValueChange = { value ->
            onInteraction(node.onValueChange, Value.Text(value))
        },
        enabled = enabled,
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
    val enabled = state.isEnabled(node.enabledKey)
    Row(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Switch(
            checked = state.getBoolean(node.checkedKey),
            enabled = enabled,
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
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val enabled = state.isEnabled(node.enabledKey)
    val content = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = node.text)
            node.icon?.let { icon ->
                Spacer(modifier = Modifier.width(4.dp))
                RenderInlineIcon(
                    source = icon,
                    size = 15.dp,
                    tone = Tone.Primary,
                    state = state
                )
            }
        }
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
            enabled = enabled,
            modifier = buttonModifier,
            colors = colors,
            shape = shape,
            contentPadding = contentPadding,
            content = { content() }
        )
    } else {
        OutlinedButton(
            onClick = { onInteraction(node.onClick, null) },
            enabled = enabled,
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
    val enabled = state.isEnabled(node.enabledKey)
    Row(
        modifier = modifier
            .then(node.layout.toModifier())
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(node.spacing.dp)
    ) {
        node.options.forEach { option ->
            val selected = state.getText(node.selectedKey) == option.value
            val tokens = resolver.chipTokens(selected)
            FilterChip(
                selected = selected,
                enabled = enabled,
                onClick = {
                    onInteraction(node.onSelected, Value.Text(option.value))
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        option.icon?.let { source ->
                            RenderInlineIcon(
                                source = source,
                                size = 18.dp,
                                tone = if (selected) Tone.Primary else Tone.Muted,
                                state = state
                            )
                        }
                        Text(text = option.label)
                    }
                },
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
                    enabled = enabled,
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
private fun RenderInlineIcon(
    source: IconSource,
    size: Dp,
    tone: Tone,
    state: State
) {
    val resolver = LocalZeroStyleResolver.current
    val loader = LocalZeroImageLoader.current
    val resolvedSource = source.toZeroImageSource(state)

    var result by remember(resolvedSource) { mutableStateOf<ZeroImageResult?>(null) }

    DisposableEffect(resolvedSource, loader) {
        result = null
        val cancelable = loader.load(ZeroImageRequest(source = resolvedSource)) { outcome ->
            result = outcome
        }
        onDispose { cancelable.cancel() }
    }

    when (val current = result) {
        is ZeroImageResult.Success -> Image(
            bitmap = current.bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tone.toColor()),
            modifier = Modifier.size(size)
        )
        ZeroImageResult.Unavailable,
        null -> Box(
            modifier = Modifier
                .size(size)
                .background(resolver.unknownContainerColor)
        )
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
        modifier = modifier
            .then(node.layout.toModifier())
            .then(node.onClick?.let { Modifier.clickable { onInteraction(it, null) } } ?: Modifier),
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

@Composable
internal fun RenderBoxNode(
    node: Node.Box,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .then(node.layout.toModifier())
            .then(node.onClick?.let { Modifier.clickable { onInteraction(it, null) } } ?: Modifier),
        contentAlignment = node.contentAlignment.toComposeAlignment()
    ) {
        node.children.forEach { child ->
            ZeroUiRenderer(node = child, state = state, onInteraction = onInteraction)
        }
    }
}

@Composable
internal fun RenderDividerNode(
    node: Node.Divider,
    modifier: Modifier
) {
    HorizontalDivider(
        modifier = modifier.then(node.layout.toModifier()),
        thickness = node.thickness.dp,
        color = (node.tone ?: Tone.Muted).toColor()
    )
}

@Composable
internal fun RenderCheckboxNode(
    node: Node.Checkbox,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val tokens = LocalZeroStyleResolver.current.checkboxTokens()
    val enabled = state.isEnabled(node.enabledKey)
    Row(
        modifier = modifier.then(node.layout.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = state.getBoolean(node.checkedKey),
            enabled = enabled,
            onCheckedChange = { checked ->
                onInteraction(node.onCheckedChange, Value.Bool(checked))
            },
            colors = CheckboxDefaults.colors(
                checkedColor = tokens.checked,
                uncheckedColor = tokens.unchecked,
                checkmarkColor = tokens.checkmark,
                disabledCheckedColor = tokens.disabledChecked,
                disabledUncheckedColor = tokens.disabledUnchecked
            )
        )
        Text(
            text = node.text,
            style = LocalZeroStyleResolver.current.bodyTextStyle
        )
    }
}

@Composable
internal fun RenderRadioGroupNode(
    node: Node.RadioGroup,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val tokens = LocalZeroStyleResolver.current.checkboxTokens()
    val enabled = state.isEnabled(node.enabledKey)
    val selectedValue = state.getText(node.selectedKey)
    Column(
        modifier = modifier.then(node.layout.toModifier()),
        verticalArrangement = Arrangement.spacedBy(node.spacing.dp)
    ) {
        node.options.forEach { option ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = enabled) {
                    onInteraction(node.onSelected, Value.Text(option.value))
                }
            ) {
                RadioButton(
                    selected = selectedValue == option.value,
                    enabled = enabled,
                    onClick = { onInteraction(node.onSelected, Value.Text(option.value)) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = tokens.checked,
                        unselectedColor = tokens.unchecked,
                        disabledSelectedColor = tokens.disabledChecked,
                        disabledUnselectedColor = tokens.disabledUnchecked
                    )
                )
                Text(
                    text = option.label,
                    style = LocalZeroStyleResolver.current.bodyTextStyle
                )
            }
        }
    }
}

@Composable
internal fun RenderProgressNode(
    node: Node.Progress,
    state: State,
    modifier: Modifier
) {
    val progress = node.progressKey?.let { state.getNumber(it).coerceIn(0, 100) / 100f }
    val color = node.tone.toColor()
    val trackColor = Tone.Muted.toContainerColor()
    when (node.variant) {
        ProgressVariant.Linear -> {
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = modifier.then(node.layout.toModifier()),
                    color = color,
                    trackColor = trackColor
                )
            } else {
                LinearProgressIndicator(
                    modifier = modifier.then(node.layout.toModifier()),
                    color = color,
                    trackColor = trackColor
                )
            }
        }
        ProgressVariant.Circular -> {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = modifier.then(node.layout.toModifier()),
                    color = color,
                    trackColor = trackColor
                )
            } else {
                CircularProgressIndicator(
                    modifier = modifier.then(node.layout.toModifier()),
                    color = color,
                    trackColor = trackColor
                )
            }
        }
    }
}

@Composable
internal fun RenderSliderNode(
    node: Node.Slider,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    val tokens = LocalZeroStyleResolver.current.sliderTokens()
    val enabled = state.isEnabled(node.enabledKey)
    val min = node.min
    val max = node.max.coerceAtLeast(min)
    val value = state.getNumber(node.valueKey).toFloat().coerceIn(min, max)
    Slider(
        value = value,
        onValueChange = { next -> onInteraction(node.onValueChange, Value.Number(next.toInt())) },
        enabled = enabled,
        valueRange = min..max,
        steps = node.steps,
        modifier = modifier.then(node.layout.toModifier()),
        colors = SliderDefaults.colors(
            thumbColor = tokens.thumb,
            activeTrackColor = tokens.activeTrack,
            inactiveTrackColor = tokens.inactiveTrack,
            disabledThumbColor = tokens.disabledThumb,
            disabledActiveTrackColor = tokens.disabledActiveTrack,
            disabledInactiveTrackColor = tokens.disabledInactiveTrack
        )
    )
}

@Composable
internal fun RenderSelectNode(
    node: Node.Select,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = state.isEnabled(node.enabledKey)
    val selectedValue = state.getText(node.selectedKey)
    val selectedLabel = node.options.firstOrNull { it.value == selectedValue }?.label ?: selectedValue
    Box(modifier = modifier.then(node.layout.toModifier())) {
        OutlinedButton(
            enabled = enabled,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = node.label?.let { "$it: $selectedLabel" } ?: selectedLabel.ifBlank { "Select" })
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            node.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onInteraction(node.onSelected, Value.Text(option.value))
                    }
                )
            }
        }
    }
}

@Composable
internal fun RenderSnackbarNode(
    node: Node.Snackbar,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier
) {
    if (!state.getBoolean(node.visibleKey)) return

    val tokens = LocalZeroStyleResolver.current.snackbarTokens()
    Snackbar(
        modifier = modifier.fillMaxWidth(),
        containerColor = tokens.container,
        contentColor = tokens.content,
        action = node.actionLabel?.let { label ->
            {
                TextButton(onClick = { node.onAction?.let { onInteraction(it, null) } }) {
                    Text(text = label, color = tokens.actionContent)
                }
            }
        },
        dismissAction = {
            TextButton(onClick = { onInteraction(node.onDismiss, null) }) {
                Text(text = "Dismiss", color = tokens.actionContent)
            }
        }
    ) {
        Text(text = node.message.resolve(state))
    }
}

@Composable
internal fun RenderBottomSheetNode(
    node: Node.BottomSheet,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit
) {
    if (!state.getBoolean(node.visibleKey)) return

    Dialog(onDismissRequest = { onInteraction(node.onDismiss, null) }) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
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
}

private fun ImageSource.toZeroImageSource(state: State): ZeroImageSource {
    return when (this) {
        is ImageSource.Resource -> ZeroImageSource.Resource(name)
        is ImageSource.Url -> ZeroImageSource.Url(value)
        is ImageSource.Binding -> ZeroImageSource.Url(state.getText(key).ifBlank { fallback })
    }
}

private fun IconSource.toZeroImageSource(state: State): ZeroImageSource {
    return when (this) {
        is IconSource.Resource -> ZeroImageSource.Resource(name)
        is IconSource.Url -> ZeroImageSource.Url(value)
        is IconSource.Binding -> ZeroImageSource.Url(state.getText(key).ifBlank { fallback })
    }
}

private fun Node.layoutWeight(): Float {
    return when (this) {
        is Node.Box -> layout.weight
        is Node.Card -> layout.weight
        is Node.ChipGroup -> layout.weight
        is Node.Checkbox -> layout.weight
        is Node.Column -> layout.weight
        is Node.Divider -> layout.weight
        is Node.ForEach -> layout.weight
        is Node.Icon -> layout.weight
        is Node.Image -> layout.weight
        is Node.LazyColumn -> layout.weight
        is Node.LazyRow -> layout.weight
        is Node.Progress -> layout.weight
        is Node.RadioGroup -> layout.weight
        is Node.Row -> layout.weight
        is Node.Select -> layout.weight
        is Node.Slider -> layout.weight
        is Node.Switch -> layout.weight
        is Node.Text -> layout.weight
        is Node.TextField -> layout.weight
        is Node.Button -> layout.weight
        is Node.BottomSheet,
        is Node.Condition,
        is Node.Dialog,
        is Node.Snackbar,
        is Node.Spacer,
        is Node.Unknown -> 0f
    }
}

private fun State.isEnabled(enabledKey: String?): Boolean {
    return enabledKey?.let(::getBoolean) ?: true
}

private fun ColumnScope.parentWeightModifier(node: Node): Modifier {
    val weight = node.layoutWeight()
    return if (weight > 0f) Modifier.weight(weight) else Modifier
}

private fun RowScope.parentWeightModifier(node: Node): Modifier {
    val weight = node.layoutWeight()
    return if (weight > 0f) Modifier.weight(weight) else Modifier
}

private fun RowScope.parentRowChildModifier(
    node: Node,
    verticalAlignment: VerticalAlignment
): Modifier {
    val base = parentWeightModifier(node)
    return if (verticalAlignment == VerticalAlignment.Baseline) base.alignByBaseline() else base
}

private fun BoxAlignment.toComposeAlignment(): Alignment = when (this) {
    BoxAlignment.TopStart -> Alignment.TopStart
    BoxAlignment.TopCenter -> Alignment.TopCenter
    BoxAlignment.TopEnd -> Alignment.TopEnd
    BoxAlignment.CenterStart -> Alignment.CenterStart
    BoxAlignment.Center -> Alignment.Center
    BoxAlignment.CenterEnd -> Alignment.CenterEnd
    BoxAlignment.BottomStart -> Alignment.BottomStart
    BoxAlignment.BottomCenter -> Alignment.BottomCenter
    BoxAlignment.BottomEnd -> Alignment.BottomEnd
}

private fun HorizontalAlignment.toComposeAlignment(): Alignment.Horizontal {
    return when (this) {
        HorizontalAlignment.Start -> Alignment.Start
        HorizontalAlignment.Center -> Alignment.CenterHorizontally
        HorizontalAlignment.End -> Alignment.End
    }
}

private fun VerticalAlignment.toComposeAlignment(): Alignment.Vertical {
    return when (this) {
        VerticalAlignment.Top -> Alignment.Top
        VerticalAlignment.Center -> Alignment.CenterVertically
        VerticalAlignment.Bottom -> Alignment.Bottom
        VerticalAlignment.Baseline -> Alignment.Top
    }
}

private fun RowArrangement.toComposeArrangement(): Arrangement.Horizontal {
    return when (this) {
        RowArrangement.Start -> Arrangement.Start
        RowArrangement.Center -> Arrangement.Center
        RowArrangement.End -> Arrangement.End
        RowArrangement.SpaceBetween -> Arrangement.SpaceBetween
        RowArrangement.SpaceAround -> Arrangement.SpaceAround
        RowArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
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

    val handler = LocalZeroUiUnknownNodeHandler.current
    LaunchedEffect(node.typeName, node.raw) {
        Log.w(UnknownNodeLogTag, "Unsupported ZeroUI node type: ${node.typeName}")
        handler?.invoke(node.typeName, node.raw)
    }

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
