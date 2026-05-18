package com.zero.zero_tools.zeroui.node

import com.zero.zero_tools.zeroui.condition.Condition
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.page.Layout
import com.zero.zero_tools.zeroui.text.Text
import com.zero.zero_tools.zeroui.text.TextStyle
import com.zero.zero_tools.zeroui.text.Tone

public sealed interface Node {
    public data class Column(
        val spacing: Int = 0,
        val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.Start,
        val layout: Layout = Layout(),
        val children: List<Node>
    ) : Node

    public data class Row(
        val spacing: Int = 0,
        val verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
        val arrangement: RowArrangement? = null,
        val layout: Layout = Layout(),
        val children: List<Node>
    ) : Node

    public data class LazyColumn(
        val spacing: Int = 0,
        val layout: Layout = Layout(),
        val children: List<Node> = emptyList(),
        val itemsKey: String? = null,
        val item: Node? = null,
        val emptyChild: Node? = null
    ) : Node

    public data class LazyRow(
        val spacing: Int = 0,
        val verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
        val layout: Layout = Layout(),
        val children: List<Node> = emptyList(),
        val itemsKey: String? = null,
        val item: Node? = null,
        val emptyChild: Node? = null
    ) : Node

    public data class Text(
        val text: com.zero.zero_tools.zeroui.text.Text,
        val style: TextStyle = TextStyle.Body,
        val tone: Tone? = null,
        val surfaceTone: Tone? = null,
        val layout: Layout = Layout(),
        val onClick: Interaction? = null
    ) : Node

    public data class Image(
        val source: ImageSource,
        val contentDescription: String? = null,
        val contentScale: ImageContentScale = ImageContentScale.Fit,
        val aspectRatio: Float? = null,
        val cornerRadius: Int = 0,
        val layout: Layout = Layout(fillMaxWidth = true),
        val onClick: Interaction? = null
    ) : Node

    public data class Icon(
        val source: IconSource,
        val contentDescription: String? = null,
        val tone: Tone = Tone.Default,
        val size: Int = 24,
        val tint: Boolean = true,
        val layout: Layout = Layout(),
        val onClick: Interaction? = null
    ) : Node

    public data class TextField(
        val label: String,
        val value: Text.Binding,
        val onValueChange: Interaction,
        val enabledKey: String? = null,
        val layout: Layout = Layout(fillMaxWidth = true)
    ) : Node

    public data class Switch(
        val text: String,
        val checkedKey: String,
        val onCheckedChange: Interaction,
        val enabledKey: String? = null,
        val layout: Layout = Layout()
    ) : Node

    public data class Button(
        val text: String,
        val onClick: Interaction,
        val variant: ButtonVariant = ButtonVariant.Primary,
        val icon: IconSource? = null,
        val enabledKey: String? = null,
        val layout: Layout = Layout()
    ) : Node

    public data class ChipGroup(
        val selectedKey: String,
        val options: List<ChipOption>,
        val onSelected: Interaction,
        val spacing: Int = 8,
        val enabledKey: String? = null,
        val layout: Layout = Layout()
    ) : Node

    public data class Card(
        val tone: Tone = Tone.Default,
        val padding: Int = 16,
        val spacing: Int = 8,
        val layout: Layout = Layout(fillMaxWidth = true),
        val onClick: Interaction? = null,
        val children: List<Node>
    ) : Node

    public data class Spacer(
        val height: Int = 0,
        val width: Int = 0
    ) : Node

    public data class Condition(
        val condition: com.zero.zero_tools.zeroui.condition.Condition,
        val child: Node
    ) : Node

    public data class ForEach(
        val itemsKey: String,
        val child: Node,
        val spacing: Int = 8,
        val layout: Layout = Layout()
    ) : Node

    public data class Dialog(
        val visibleKey: String,
        val onDismiss: Interaction = Interaction(),
        val title: com.zero.zero_tools.zeroui.text.Text? = null,
        val spacing: Int = 8,
        val padding: Int = 20,
        val children: List<Node>
    ) : Node

    public data class Box(
        val contentAlignment: BoxAlignment = BoxAlignment.TopStart,
        val layout: Layout = Layout(),
        val onClick: Interaction? = null,
        val children: List<Node>
    ) : Node

    public data class Divider(
        val thickness: Int = 1,
        val tone: Tone? = null,
        val layout: Layout = Layout(fillMaxWidth = true)
    ) : Node

    public data class Checkbox(
        val text: String,
        val checkedKey: String,
        val onCheckedChange: Interaction,
        val enabledKey: String? = null,
        val layout: Layout = Layout()
    ) : Node

    public data class RadioGroup(
        val selectedKey: String,
        val options: List<RadioOption>,
        val onSelected: Interaction,
        val spacing: Int = 8,
        val enabledKey: String? = null,
        val layout: Layout = Layout()
    ) : Node

    public data class Progress(
        val variant: ProgressVariant = ProgressVariant.Linear,
        val progressKey: String? = null,
        val tone: Tone = Tone.Primary,
        val layout: Layout = Layout()
    ) : Node

    public data class Slider(
        val valueKey: String,
        val onValueChange: Interaction,
        val min: Float = 0f,
        val max: Float = 1f,
        val steps: Int = 0,
        val enabledKey: String? = null,
        val layout: Layout = Layout(fillMaxWidth = true)
    ) : Node

    public data class Select(
        val selectedKey: String,
        val options: List<SelectOption>,
        val onSelected: Interaction,
        val label: String? = null,
        val enabledKey: String? = null,
        val layout: Layout = Layout(fillMaxWidth = true)
    ) : Node

    public data class Snackbar(
        val visibleKey: String,
        val message: com.zero.zero_tools.zeroui.text.Text,
        val actionLabel: String? = null,
        val onAction: Interaction? = null,
        val onDismiss: Interaction = Interaction()
    ) : Node

    public data class BottomSheet(
        val visibleKey: String,
        val onDismiss: Interaction = Interaction(),
        val title: com.zero.zero_tools.zeroui.text.Text? = null,
        val spacing: Int = 8,
        val padding: Int = 20,
        val children: List<Node>
    ) : Node

    /**
     * Fallback node returned when [parseNode] encounters an unsupported `type`.
     * Renderers should display a non-fatal placeholder so the rest of the page keeps rendering.
     */
    public data class Unknown(
        val typeName: String,
        val raw: String
    ) : Node
}

public enum class ButtonVariant {
    Primary,
    Secondary
}

public enum class HorizontalAlignment {
    Start,
    Center,
    End
}

public enum class VerticalAlignment {
    Top,
    Center,
    Bottom,
    Baseline
}

public enum class RowArrangement {
    Start,
    Center,
    End,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

public sealed interface ImageSource {
    public data class Url(val value: String) : ImageSource
    public data class Resource(val name: String) : ImageSource
    public data class Binding(val key: String, val fallback: String = "") : ImageSource
}

public sealed interface IconSource {
    public data class Url(val value: String) : IconSource
    public data class Resource(val name: String) : IconSource
    public data class Binding(val key: String, val fallback: String = "") : IconSource
}

public enum class ImageContentScale {
    Fit,
    Crop,
    FillWidth
}

public data class ChipOption(
    val label: String,
    val value: String,
    val icon: IconSource? = null
)

public enum class BoxAlignment {
    TopStart, TopCenter, TopEnd,
    CenterStart, Center, CenterEnd,
    BottomStart, BottomCenter, BottomEnd
}

public enum class ProgressVariant {
    Linear,
    Circular
}

public data class RadioOption(
    val label: String,
    val value: String
)

public data class SelectOption(
    val label: String,
    val value: String
)
