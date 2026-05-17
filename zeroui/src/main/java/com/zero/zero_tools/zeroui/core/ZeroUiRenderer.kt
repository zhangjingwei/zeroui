package com.zero.zero_tools.zeroui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.node.Node
import com.zero.zero_tools.zeroui.node.RenderButtonNode
import com.zero.zero_tools.zeroui.node.RenderCardNode
import com.zero.zero_tools.zeroui.node.RenderChipGroupNode
import com.zero.zero_tools.zeroui.node.RenderColumnNode
import com.zero.zero_tools.zeroui.node.RenderConditionNode
import com.zero.zero_tools.zeroui.node.RenderDialogNode
import com.zero.zero_tools.zeroui.node.RenderForEachNode
import com.zero.zero_tools.zeroui.node.RenderIconNode
import com.zero.zero_tools.zeroui.node.RenderImageNode
import com.zero.zero_tools.zeroui.node.RenderLazyColumnNode
import com.zero.zero_tools.zeroui.node.RenderLazyRowNode
import com.zero.zero_tools.zeroui.node.RenderRowNode
import com.zero.zero_tools.zeroui.node.RenderSpacerNode
import com.zero.zero_tools.zeroui.node.RenderSwitchNode
import com.zero.zero_tools.zeroui.node.RenderTextFieldNode
import com.zero.zero_tools.zeroui.node.RenderTextNode
import com.zero.zero_tools.zeroui.node.RenderUnknownNode
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.value.Value

public val LocalZeroUiUnknownNodeHandler: androidx.compose.runtime.ProvidableCompositionLocal<((typeName: String, raw: String) -> Unit)?> = compositionLocalOf {
    null
}

@Composable
public fun ZeroUiRenderer(
    node: Node,
    state: State,
    onInteraction: (Interaction, Value?) -> Unit,
    modifier: Modifier = Modifier
) {
    when (node) {
        is Node.Column -> RenderColumnNode(node, state, onInteraction, modifier)
        is Node.Row -> RenderRowNode(node, state, onInteraction, modifier)
        is Node.LazyColumn -> RenderLazyColumnNode(node, state, onInteraction, modifier)
        is Node.LazyRow -> RenderLazyRowNode(node, state, onInteraction, modifier)
        is Node.Condition -> RenderConditionNode(node, state, onInteraction, modifier)
        is Node.Text -> RenderTextNode(node, state, onInteraction, modifier)
        is Node.Image -> RenderImageNode(node, state, onInteraction, modifier)
        is Node.Icon -> RenderIconNode(node, state, onInteraction, modifier)
        is Node.TextField -> RenderTextFieldNode(node, state, onInteraction, modifier)
        is Node.Switch -> RenderSwitchNode(node, state, onInteraction, modifier)
        is Node.Button -> RenderButtonNode(node, state, onInteraction, modifier)
        is Node.ChipGroup -> RenderChipGroupNode(node, state, onInteraction, modifier)
        is Node.Card -> RenderCardNode(node, state, onInteraction, modifier)
        is Node.Spacer -> RenderSpacerNode(node, modifier)
        is Node.ForEach -> RenderForEachNode(node, state, onInteraction, modifier)
        is Node.Dialog -> RenderDialogNode(node, state, onInteraction)
        is Node.Unknown -> RenderUnknownNode(node, modifier)
    }
}
