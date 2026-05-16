package com.zero.zero_tools.zeroui.effect

import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.ValueSource

sealed interface Effect {
    data class Toast(
        val message: ValueSource
    ) : Effect

    data class Log(
        val message: ValueSource
    ) : Effect

    /**
     * Push a new page onto the host's navigation stack.
     * `target` resolves at execute time to a [com.zero.zero_tools.zeroui.value.Value.Text],
     * which the host's [com.zero.zero_tools.zeroui.navigation.Navigator] interprets
     * (asset name, route id, URL, ...).
     */
    data class Navigate(
        val target: ValueSource
    ) : Effect

    /** Pop the top page from the host's navigation stack. */
    data object Back : Effect

    /**
     * Emit a tracking event. Params are resolved against the current state/event scope at
     * execute time and handed to the host-supplied [com.zero.zero_tools.zeroui.tracking.Tracker].
     */
    data class Track(
        val event: String,
        val params: Map<String, ValueSource> = emptyMap()
    ) : Effect

    /**
     * Fires an HTTP request via the host-supplied [com.zero.zero_tools.zeroui.http.HttpClient].
     *
     * The response Value (parsed JSON object/array or fallback Text) becomes the `eventValue`
     * of [onSuccess] or [onError] — meaning `{"type":"event"}` inside those interactions
     * yields the whole response, and dotted-path bindings (`data.list`) walk into Records.
     *
     * Routing:
     * - 2xx, no transport error → [onSuccess]
     * - 4xx/5xx OR transport error → [onError]
     */
    data class Http(
        val method: String,
        val url: ValueSource,
        val headers: Map<String, ValueSource> = emptyMap(),
        val body: ValueSource? = null,
        val onSuccess: Interaction = Interaction(),
        val onError: Interaction = Interaction()
    ) : Effect
}
