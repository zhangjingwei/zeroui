package com.zero.zero_tools.zeroui.host

import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.Value

internal fun pageScopedFollowUp(
    originEntryId: Long,
    currentTopId: () -> Long?,
    onAllow: (Interaction, Value?) -> Unit,
    onDrop: (origin: Long, current: Long?) -> Unit = { _, _ -> }
): (Interaction, Value?) -> Unit = { interaction, eventValue ->
    val current = currentTopId()
    if (current == originEntryId) {
        onAllow(interaction, eventValue)
    } else {
        onDrop(originEntryId, current)
    }
}
