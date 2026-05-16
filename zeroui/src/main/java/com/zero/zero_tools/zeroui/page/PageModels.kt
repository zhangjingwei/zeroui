package com.zero.zero_tools.zeroui.page

import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.node.Node
import com.zero.zero_tools.zeroui.state.State

data class Page(
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

data class Layout(
    val fillMaxWidth: Boolean = false,
    val padding: Int = 0
)
