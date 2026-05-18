package com.zero.zero_tools.zeroui.http

import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.effect.HttpCachePolicy
import com.zero.zero_tools.zeroui.effect.HttpMapMode
import com.zero.zero_tools.zeroui.effect.HttpResponseMode
import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.asText
import com.zero.zero_tools.zeroui.state.resolveValueSource
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Resolves an [Effect.Http]'s ValueSource fields against the current state/event, then
 * hands the materialised request to [client]. When [client] reports a response, routes
 * it to `onSuccess` or `onError` by invoking [onFollowUp] with the response Value as the
 * new event value.
 *
 * Carved out of [com.zero.zero_tools.zeroui.effect.executeEffects] so it can be unit-tested
 * without an Android Context (which `Toast` / `Log` paths require).
 */
public fun dispatchHttp(
    state: State,
    effect: Effect.Http,
    eventValue: Value?,
    client: HttpClient,
    cache: HttpResponseCache? = null,
    shouldAcceptResponse: () -> Boolean = { true },
    onFollowUp: (Interaction, Value?) -> Unit
): Cancelable {
    val url = buildUrl(
        baseUrl = resolveValueSource(state, effect.url, eventValue).asText(),
        params = effect.params.mapValues { (_, source) ->
            resolveValueSource(state, source, eventValue).asText()
        }
    )
    val headers = effect.headers.mapValues { (_, source) ->
        resolveValueSource(state, source, eventValue).asText()
    }
    val body = effect.body?.let { resolveValueSource(state, it, eventValue).asText() }

    val request = HttpRequest(
        method = effect.method.uppercase(),
        url = url,
        headers = headers,
        body = body,
        timeoutMs = effect.timeoutMs,
        retryCount = effect.retryCount,
        retryDelayMs = effect.retryDelayMs
    )
    val cacheKey = request.cacheKey()
    if (effect.cachePolicy == HttpCachePolicy.CacheFirst) {
        cache?.get(cacheKey)?.let { cached ->
            cached.dispatch(effect, onFollowUp)
            return Cancelable.Noop
        }
    }

    val cancelable = RetiringCancelable()
    dispatchLifecycle(effect.onStart, effect.startStateInteraction(), onFollowUp)
    val clientCancelable = client.request(
        request = request
    ) { response ->
        try {
            if (!shouldAcceptResponse()) return@request
            if (response.isSuccess && effect.cachePolicy != HttpCachePolicy.NetworkOnly) {
                cache?.set(cacheKey, response)
            }
            response.dispatch(effect, onFollowUp)
        } finally {
            cancelable.retire()
        }
    }
    cancelable.attach(clientCancelable)
    return cancelable
}

private fun HttpRequest.cacheKey(): String {
    return listOf(
        method,
        url,
        headers.entries.sortedBy { it.key }.joinToString("&") { "${it.key}:${it.value}" },
        body.orEmpty()
    ).joinToString("|")
}

private fun HttpResponse.dispatch(
    effect: Effect.Http,
    onFollowUp: (Interaction, Value?) -> Unit
) {
    val (interaction, payload) = routeFor(effect)
    val mappedValues = effect.map.mapNotNull { mapping ->
        readPath(envelope(), body, mapping.path)?.let { mapping to it }
    }
    dispatchLifecycle(
        interaction,
        effect.resultStateInteraction(this, mappedValues.firstOrNull()?.second),
        onFollowUp,
        payload
    )
    mappedValues.forEach { (mapping, value) ->
        val action = when (mapping.mode) {
            HttpMapMode.Replace -> Action.SetState(
                key = mapping.key,
                value = ValueSource.Literal(value)
            )
            HttpMapMode.Append -> Action.AppendState(
                key = mapping.key,
                value = ValueSource.Literal(value)
            )
        }
        onFollowUp(Interaction(actions = listOf(action)), payload)
    }
    dispatchLifecycle(effect.onFinally, Interaction(), onFollowUp, payload)
}

/**
 * For success: yields [HttpResponse.body] (parsed JSON).
 * For error: yields a Value.Record describing `{statusCode, error, body}` so the
 * onError interaction can extract whichever it cares about via dotted-path bindings.
 */
private fun HttpResponse.routeFor(effect: Effect.Http): Pair<Interaction, Value> {
    return if (isSuccess) {
        effect.onSuccess to successPayload(effect)
    } else {
        effect.onError to envelope()
    }
}

private fun HttpResponse.successPayload(effect: Effect.Http): Value {
    return when (effect.responseMode) {
        HttpResponseMode.Body -> body
        HttpResponseMode.Full -> envelope()
    }
}

private fun HttpResponse.envelope(): Value.Record {
    return Value.Record(
        fields = mapOf(
            "ok" to Value.Bool(isSuccess),
            "statusCode" to Value.Number(statusCode),
            "headers" to headers.toValueRecord(),
            "error" to Value.Text(errorMessage ?: ""),
            "body" to body,
            "empty" to Value.Bool(body.isEmptyValue())
        )
    )
}

private fun Map<String, String>.toValueRecord(): Value.Record {
    return Value.Record(mapValues { (_, value) -> Value.Text(value) })
}

private fun Value.isEmptyValue(): Boolean {
    return when (this) {
        is Value.Text -> value.isBlank()
        is Value.List -> items.isEmpty()
        is Value.Record -> fields.isEmpty()
        is Value.Bool -> false
        is Value.Number -> false
    }
}

private fun Effect.Http.startStateInteraction(): Interaction {
    val key = stateKey ?: return Interaction()
    return Interaction(
        actions = listOf(
            Action.SetState("$key.loading", ValueSource.Literal(Value.Bool(true))),
            Action.SetState("$key.error", ValueSource.Literal(Value.Text(""))),
            Action.SetState("$key.statusCode", ValueSource.Literal(Value.Number(0)))
        )
    )
}

private fun Effect.Http.resultStateInteraction(response: HttpResponse, mappedValue: Value?): Interaction {
    val key = stateKey ?: return Interaction()
    val envelope = response.envelope()
    val emptyValue = mappedValue ?: response.body
    val actions = mutableListOf<Action>(
        Action.SetState("$key.loading", ValueSource.Literal(Value.Bool(false))),
        Action.SetState("$key.statusCode", ValueSource.Literal(Value.Number(response.statusCode))),
        Action.SetState("$key.headers", ValueSource.Literal(response.headers.toValueRecord())),
        Action.SetState("$key.empty", ValueSource.Literal(Value.Bool(emptyValue.isEmptyValue())))
    )
    if (response.isSuccess) {
        actions += Action.SetState("$key.data", ValueSource.Literal(response.body))
        actions += Action.SetState("$key.error", ValueSource.Literal(Value.Text("")))
    } else {
        actions += Action.SetState("$key.error", ValueSource.Literal(envelope))
    }
    return Interaction(actions = actions)
}

private fun dispatchLifecycle(
    userInteraction: Interaction,
    stateInteraction: Interaction,
    onFollowUp: (Interaction, Value?) -> Unit,
    eventValue: Value? = null
) {
    if (stateInteraction.actions.isNotEmpty() || stateInteraction.effects.isNotEmpty()) {
        onFollowUp(stateInteraction, eventValue)
    }
    if (userInteraction.actions.isNotEmpty() || userInteraction.effects.isNotEmpty()) {
        onFollowUp(userInteraction, eventValue)
    }
}

private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
    if (params.isEmpty()) return baseUrl
    val fragmentIndex = baseUrl.indexOf('#')
    val beforeFragment = if (fragmentIndex >= 0) baseUrl.substring(0, fragmentIndex) else baseUrl
    val fragment = if (fragmentIndex >= 0) baseUrl.substring(fragmentIndex) else ""
    val separator = when {
        beforeFragment.endsWith("?") || beforeFragment.endsWith("&") -> ""
        beforeFragment.contains("?") -> "&"
        else -> "?"
    }
    val query = params.entries.joinToString("&") { (key, value) ->
        "${encodeQueryPart(key)}=${encodeQueryPart(value)}"
    }
    return beforeFragment + separator + query + fragment
}

private fun encodeQueryPart(value: String): String =
    java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

private fun readPath(envelope: Value.Record, body: Value, path: String): Value? {
    val trimmed = path.trim().ifBlank { "body" }
    return when {
        trimmed == "body" -> body
        trimmed.startsWith("body.") -> body.readDotted(trimmed.removePrefix("body."))
        trimmed.startsWith("response.") -> envelope.readDotted(trimmed.removePrefix("response."))
        else -> envelope.readDotted(trimmed) ?: body.readDotted(trimmed)
    }
}

private fun Value.readDotted(path: String): Value? {
    if (path.isBlank()) return this
    var current: Value? = this
    path.split('.').forEach { segment ->
        val name = segment.substringBefore('[')
        if (name.isNotBlank()) {
            current = (current as? Value.Record)?.fields?.get(name)
        }
        if (segment.contains('[')) {
            val index = segment.substringAfter('[').substringBefore(']').toIntOrNull()
            current = index?.let { (current as? Value.List)?.items?.getOrNull(it) }
        }
    }
    return current
}

internal interface RetirableCancelable : Cancelable {
    val isRetired: Boolean
    fun invokeOnRetired(callback: () -> Unit)
}

private class RetiringCancelable : RetirableCancelable {
    private val delegate = AtomicReference<Cancelable?>(null)
    private val cancelRequested = AtomicBoolean(false)
    private val retired = AtomicBoolean(false)
    private val callbacks = mutableListOf<() -> Unit>()

    override val isRetired: Boolean
        get() = retired.get()

    fun attach(cancelable: Cancelable) {
        delegate.set(cancelable)
        if (isRetired) {
            val attached = delegate.getAndSet(null)
            if (cancelRequested.get()) {
                attached?.cancel()
            }
        }
    }

    override fun cancel() {
        cancelRequested.set(true)
        delegate.getAndSet(null)?.cancel()
        retire()
    }

    override fun invokeOnRetired(callback: () -> Unit) {
        val invokeNow = synchronized(callbacks) {
            if (isRetired) {
                true
            } else {
                callbacks.add(callback)
                false
            }
        }
        if (invokeNow) {
            callback()
        }
    }

    fun retire() {
        if (!retired.compareAndSet(false, true)) return
        delegate.set(null)
        val callbacksToInvoke = synchronized(callbacks) {
            callbacks.toList().also { callbacks.clear() }
        }
        callbacksToInvoke.forEach { it() }
    }
}
