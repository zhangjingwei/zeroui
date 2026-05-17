package com.zero.zero_tools.zeroui.page

import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.node.Node
import com.zero.zero_tools.zeroui.state.State

public data class Page(
    /**
     * Server-side protocol version for this page schema.
     *
     * Missing versions default to 1 so existing pages remain compatible. Hosts can inspect
     * this value for telemetry or preflight checks before rendering.
     */
    val schemaVersion: Int = ZeroUiSchemaVersion.Current,
    val initialState: State,
    val root: Node,
    /**
     * Lifecycle hook fired exactly once per page entry — when the host loads/pushes
     * this page onto its navigation stack. Used for "enter screen and auto-fetch"
     * patterns; combines naturally with Wave 4 page-scoped cancellation so a leaving
     * page's onMount-driven request gets cancelled.
     *
     * Returning to an existing entry (back navigation) does NOT re-fire onMount —
     * the entry's state is preserved and the original mount counts as still-active.
     * Re-navigating to the same page by name creates a new entry and therefore
     * does re-fire.
     */
    val onMount: Interaction = Interaction()
)

public object ZeroUiSchemaVersion {
    public const val Current: Int = 1
}

public data class Layout(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val weight: Float = 0f,
    val padding: Int = 0,
    val paddingStart: Int? = null,
    val paddingTop: Int? = null,
    val paddingEnd: Int? = null,
    val paddingBottom: Int? = null,
    val width: Int = 0,
    val height: Int = 0,
    val minWidth: Int = 0,
    val minHeight: Int = 0,
    val maxWidth: Int = 0,
    val maxHeight: Int = 0
)
