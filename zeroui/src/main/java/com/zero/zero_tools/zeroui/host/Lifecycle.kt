package com.zero.zero_tools.zeroui.host

import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.Value

internal fun fireOnMountIfAny(
    onMount: Interaction,
    handle: ((Interaction, Value?) -> Unit)?
): Boolean {
    if (handle == null) return false
    if (onMount.actions.isEmpty() && onMount.effects.isEmpty()) return false
    handle(onMount, null)
    return true
}
