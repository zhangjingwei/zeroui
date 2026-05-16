package com.zero.zero_tools.host

import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.Value

/**
 * Wave 5 helper: dispatch a page's onMount [Interaction] if it has anything to run.
 *
 * Kept as a pure function so the "did we trigger?" decision can be unit-tested without
 * a Compose runtime. The host is responsible for calling this at the right moments —
 * once per stack push and once when the host first composes for the start page.
 *
 * `handle` is nullable to accommodate the forward-reference holder pattern in
 * `ZeroUiHost`: at the moment the navigator is constructed via `remember { ... }`,
 * `handleInteraction` is not yet defined; the navigator captures a holder whose
 * `value` is filled in later in the same composition.
 */
internal fun fireOnMountIfAny(
    onMount: Interaction,
    handle: ((Interaction, Value?) -> Unit)?
): Boolean {
    if (handle == null) return false
    if (onMount.actions.isEmpty() && onMount.effects.isEmpty()) return false
    handle(onMount, null)
    return true
}
