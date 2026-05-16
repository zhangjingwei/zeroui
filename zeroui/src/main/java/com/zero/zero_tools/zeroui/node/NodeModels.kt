package com.zero.zero_tools.zeroui.node

import com.zero.zero_tools.zeroui.condition.Condition
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.page.Layout
import com.zero.zero_tools.zeroui.text.Text
import com.zero.zero_tools.zeroui.text.TextStyle
import com.zero.zero_tools.zeroui.text.Tone

sealed interface Node {
    data class Column(
        val spacing: Int = 0,
        val layout: Layout = Layout(),
        val children: List<Node>
    ) : Node

    data class Row(
        val spacing: Int = 0,
        val layout: Layout = Layout(),
        val children: List<Node>
    ) : Node

    data class Text(
        val text: com.zero.zero_tools.zeroui.text.Text,
        val style: TextStyle = TextStyle.Body,
        val tone: Tone = Tone.Default,
        val layout: Layout = Layout()
    ) : Node

    data class TextField(
        val label: String,
        val value: Text.Binding,
        val onValueChange: Interaction,
        val layout: Layout = Layout(fillMaxWidth = true)
    ) : Node

    data class Switch(
        val text: String,
        val checkedKey: String,
        val onCheckedChange: Interaction,
        val layout: Layout = Layout()
    ) : Node

    data class Button(
        val text: String,
        val onClick: Interaction,
        val variant: ButtonVariant = ButtonVariant.Primary,
        val layout: Layout = Layout()
    ) : Node

    data class ChipGroup(
        val selectedKey: String,
        val options: List<ChipOption>,
        val onSelected: Interaction,
        val spacing: Int = 8,
        val layout: Layout = Layout()
    ) : Node

    data class Card(
        val tone: Tone = Tone.Default,
        val padding: Int = 16,
        val spacing: Int = 8,
        val layout: Layout = Layout(fillMaxWidth = true),
        val children: List<Node>
    ) : Node

    data class Spacer(
        val height: Int = 0,
        val width: Int = 0
    ) : Node

    data class Condition(
        val condition: com.zero.zero_tools.zeroui.condition.Condition,
        val child: Node
    ) : Node

    data class ForEach(
        val itemsKey: String,
        val child: Node,
        val spacing: Int = 8,
        val layout: Layout = Layout()
    ) : Node

    /**
     * Fallback node returned when [parseNode] encounters an unsupported `type`.
     * Renderers should display a non-fatal placeholder so the rest of the page keeps rendering.
     */
    data class Unknown(
        val typeName: String,
        val raw: String
    ) : Node
}

enum class ButtonVariant {
    Primary,
    Secondary
}

data class ChipOption(
    val label: String,
    val value: String
)
