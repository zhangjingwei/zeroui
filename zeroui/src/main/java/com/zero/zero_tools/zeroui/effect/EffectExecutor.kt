package com.zero.zero_tools.zeroui.effect

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.zero.zero_tools.zeroui.http.Cancelable
import com.zero.zero_tools.zeroui.http.HttpClient
import com.zero.zero_tools.zeroui.http.HttpResponseCache
import com.zero.zero_tools.zeroui.http.dispatchHttp
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.navigation.Navigator
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.asText
import com.zero.zero_tools.zeroui.state.resolveValueSource
import com.zero.zero_tools.zeroui.tracking.Tracker
import com.zero.zero_tools.zeroui.value.Value

private const val ZeroUiLogTag = "ZeroToolsZeroUi"

public fun executeEffects(
    context: Context,
    state: State,
    effects: List<Effect>,
    eventValue: Value? = null,
    navigator: Navigator = Navigator.Noop,
    tracker: Tracker = Tracker.Noop,
    httpClient: HttpClient = HttpClient.Noop,
    httpCache: HttpResponseCache? = null,
    onFollowUp: (Interaction, Value?) -> Unit = { _, _ -> },
    onHttpStart: (Effect.Http) -> Long = { 0L },
    shouldAcceptHttpResponse: (Effect.Http, Long) -> Boolean = { _, _ -> true },
    onCancelable: (Cancelable, Effect.Http, Long) -> Unit = { _, _, _ -> }
) {
    effects.forEach { effect ->
        when (effect) {
            is Effect.Toast -> Toast.makeText(
                context,
                resolveValueSource(state, effect.message, eventValue).asText(),
                Toast.LENGTH_SHORT
            ).show()

            is Effect.Log -> Log.d(
                ZeroUiLogTag,
                resolveValueSource(state, effect.message, eventValue).asText()
            )

            is Effect.Navigate -> navigator.navigate(
                target = resolveValueSource(state, effect.target, eventValue).asText(),
                kind = effect.targetKind
            )

            Effect.Back -> navigator.back()

            is Effect.Track -> tracker.track(
                event = effect.event,
                params = effect.params.mapValues { (_, source) ->
                    resolveValueSource(state, source, eventValue)
                }
            )

            is Effect.Http -> {
                val requestGeneration = onHttpStart(effect)
                val cancelable = dispatchHttp(
                    state = state,
                    effect = effect,
                    eventValue = eventValue,
                    client = httpClient,
                    cache = httpCache,
                    shouldAcceptResponse = {
                        shouldAcceptHttpResponse(effect, requestGeneration)
                    },
                    onFollowUp = onFollowUp
                )
                if (cancelable !== Cancelable.Noop) {
                    onCancelable(cancelable, effect, requestGeneration)
                }
            }
        }
    }
}
