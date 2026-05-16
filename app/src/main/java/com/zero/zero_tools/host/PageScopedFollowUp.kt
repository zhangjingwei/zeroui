package com.zero.zero_tools.host

import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.Value

/**
 * Builds an [onFollowUp] callback that only fires when the page entry that issued the
 * original effect is still the top of the navigation stack at response time.
 *
 * Wave 3.5 semantics: an HTTP follow-up may write back only to the page that started
 * the request. If the user navigated away (or popped back past it), the late response
 * is discarded — [onDrop] is invoked instead so the host can log / surface it.
 *
 * Kept as a pure function so the page-stack interaction can be unit-tested without
 * pulling in Compose or Android lifecycle.
 *
 * @param originEntryId the id of the page entry that dispatched the original effect.
 * @param currentTopId resolved at follow-up time — usually `{ stack.last().id }`.
 *                     Returning null means there is no longer any top entry; treat as drop.
 * @param onAllow invoked when [currentTopId] matches [originEntryId].
 * @param onDrop invoked otherwise; receives (origin, current) for observability.
 */
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
