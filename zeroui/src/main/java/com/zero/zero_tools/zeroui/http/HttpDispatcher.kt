package com.zero.zero_tools.zeroui.http

import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.asText
import com.zero.zero_tools.zeroui.state.resolveValueSource
import com.zero.zero_tools.zeroui.value.Value
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
    onFollowUp: (Interaction, Value?) -> Unit
): Cancelable {
    val url = resolveValueSource(state, effect.url, eventValue).asText()
    val headers = effect.headers.mapValues { (_, source) ->
        resolveValueSource(state, source, eventValue).asText()
    }
    val body = effect.body?.let { resolveValueSource(state, it, eventValue).asText() }

    val cancelable = RetiringCancelable()
    val clientCancelable = client.request(
        method = effect.method.uppercase(),
        url = url,
        headers = headers,
        body = body
    ) { response ->
        try {
            val (interaction, payload) = response.routeFor(effect)
            onFollowUp(interaction, payload)
        } finally {
            cancelable.retire()
        }
    }
    cancelable.attach(clientCancelable)
    return cancelable
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
