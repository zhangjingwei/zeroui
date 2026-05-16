package com.zero.zero_tools.zeroui.tracking

import com.zero.zero_tools.zeroui.value.Value

/**
 * Host-supplied tracking sink for ZeroUI [com.zero.zero_tools.zeroui.effect.Effect.Track].
 *
 * The library does not depend on any analytics SDK. The host implements `track` to forward
 * events into whatever platform it uses (Firebase, Amplitude, an internal pipeline, ...).
 *
 * Params are already resolved [Value]s — the host typically flattens them via `asText()` or
 * branches on the Value subtype.
 */
public fun interface Tracker {
    public fun track(event: String, params: Map<String, Value>)

    public companion object {
        /** Drops all events. Useful for previews and tests. */
        public val Noop: Tracker = Tracker { _, _ -> }
    }
}
