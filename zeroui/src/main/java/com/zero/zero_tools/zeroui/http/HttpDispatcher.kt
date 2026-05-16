package com.zero.zero_tools.zeroui.http

import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.asText
import com.zero.zero_tools.zeroui.state.resolveValueSource
import com.zero.zero_tools.zeroui.value.Value

/**
 * Resolves an [Effect.Http]'s ValueSource fields against the current state/event, then
 * hands the materialised request to [client]. When [client] reports a response, routes
 * it to `onSuccess` or `onError` by invoking [onFollowUp] with the response Value as the
 * new event value.
 *
 * Carved out of [com.zero.zero_tools.zeroui.effect.executeEffects] so it can be unit-tested
 * without an Android Context (which `Toast` / `Log` paths require).
 */
fun dispatchHttp(
    state: State,
    effect: Effect.Http,
    eventValue: Value?,
    client: HttpClient,
    onFollowUp: (Interaction, Value?) -> Unit
): Cancelable {
    val url = resolveValueSource(state, effect.url, eventValue).asText()
    val headers = effect.headers.mapValues { (_, source) ->
        resolveValueSource(state, source, eventValue).asText()
    }
    val body = effect.body?.let { resolveValueSource(state, it, eventValue).asText() }

    return client.request(
        method = effect.method.uppercase(),
        url = url,
        headers = headers,
        body = body
    ) { response ->
        val (interaction, payload) = response.routeFor(effect)
        onFollowUp(interaction, payload)
    }
}

/**
 * For success: yields [HttpResponse.body] (parsed JSON).
 * For error: yields a Value.Record describing `{statusCode, error, body}` so the
 * onError interaction can extract whichever it cares about via dotted-path bindings.
 */
private fun HttpResponse.routeFor(effect: Effect.Http): Pair<Interaction, Value> {
    return if (isSuccess) {
        effect.onSuccess to body
    } else {
        val errorRecord = Value.Record(
            fields = mapOf(
                "statusCode" to Value.Number(statusCode),
                "error" to Value.Text(errorMessage ?: ""),
                "body" to body
            )
        )
        effect.onError to errorRecord
    }
}
