package com.zero.zero_tools

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.core.parseZeroUiPage
import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.http.Cancelable
import com.zero.zero_tools.zeroui.http.HttpClient
import com.zero.zero_tools.zeroui.http.HttpResponse
import com.zero.zero_tools.zeroui.http.dispatchHttp
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.node.Node
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.StateEntry
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers:
 * - JSON parsing of the `http` effect (method / url / headers / body / onSuccess / onError)
 * - dispatchHttp routing: 2xx → onSuccess, 4xx/5xx → onError, transport failure → onError
 * - ValueSource resolution for url / headers / body
 * - Response Value wired into the onFollowUp call as eventValue
 */
class ZeroUiHttpTest {

    // ---------- parser ----------

    @Test
    fun parsesHttpEffectWithFullShape() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "Fetch",
                  "onClick": {
                    "effects": [
                      {
                        "type": "http",
                        "method": "POST",
                        "url": { "type": "literal", "value": "https://x.test/api" },
                        "headers": {
                          "Authorization": { "type": "state", "key": "token" }
                        },
                        "body": { "type": "literal", "value": "{\"a\":1}" },
                        "onSuccess": {
                          "actions": [
                            { "type": "setState", "key": "data", "value": { "type": "event" } }
                          ]
                        },
                        "onError": {
                          "actions": [
                            { "type": "setState", "key": "errorMsg", "value": { "type": "event" } }
                          ]
                        }
                      }
                    ]
                  }
                }
              }
            """.trimIndent()
        )

        val http = (page.root as Node.Button).onClick.effects.single() as Effect.Http
        assertEquals("POST", http.method)
        assertEquals(ValueSource.Literal(Value.Text("https://x.test/api")), http.url)
        assertEquals(ValueSource.StateValue("token"), http.headers["Authorization"])
        assertEquals(ValueSource.Literal(Value.Text("{\"a\":1}")), http.body)
        assertEquals(1, http.onSuccess.actions.size)
        assertEquals(1, http.onError.actions.size)
    }

    @Test
    fun parsesHttpEffectWithMinimalShape() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "Fetch",
                  "onClick": {
                    "effects": [
                      { "type": "http", "url": { "type": "literal", "value": "https://x.test" } }
                    ]
                  }
                }
              }
            """.trimIndent()
        )
        val http = (page.root as Node.Button).onClick.effects.single() as Effect.Http
        assertEquals("GET", http.method) // default
        assertTrue(http.headers.isEmpty())
        assertNull(http.body)
        assertTrue(http.onSuccess.actions.isEmpty())
        assertTrue(http.onError.actions.isEmpty())
    }

    // ---------- dispatch ----------

    private fun successEffect(): Effect.Http = Effect.Http(
        method = "GET",
        url = ValueSource.Literal(Value.Text("https://x.test/users")),
        onSuccess = Interaction(
            actions = listOf(Action.SetState(key = "data", value = ValueSource.EventValue))
        ),
        onError = Interaction(
            actions = listOf(Action.SetState(key = "errorMsg", value = ValueSource.EventValue))
        )
    )

    @Test
    fun dispatch200RoutesToOnSuccessWithBody() {
        val responseBody = Value.Record(fields = mapOf("id" to Value.Number(7)))
        val client = HttpClient { _, _, _, _, onResponse ->
            onResponse(HttpResponse(statusCode = 200, body = responseBody))
            Cancelable.Noop
        }

        var captured: Pair<Interaction, Value?>? = null
        dispatchHttp(
            state = State(),
            effect = successEffect(),
            eventValue = null,
            client = client
        ) { interaction, eventValue ->
            captured = interaction to eventValue
        }

        assertEquals(successEffect().onSuccess, captured?.first)
        assertEquals(responseBody, captured?.second)
    }

    @Test
    fun dispatch500RoutesToOnErrorWithRecord() {
        val client = HttpClient { _, _, _, _, onResponse ->
            onResponse(HttpResponse(statusCode = 500, body = Value.Text("server fail")))
            Cancelable.Noop
        }

        var captured: Pair<Interaction, Value?>? = null
        dispatchHttp(State(), successEffect(), null, client) { interaction, eventValue ->
            captured = interaction to eventValue
        }

        assertEquals(successEffect().onError, captured?.first)
        val record = captured?.second as Value.Record
        assertEquals(Value.Number(500), record.fields["statusCode"])
        assertEquals(Value.Text(""), record.fields["error"])
        assertEquals(Value.Text("server fail"), record.fields["body"])
    }

    @Test
    fun dispatchTransportFailureRoutesToOnErrorWithMessage() {
        val client = HttpClient { _, _, _, _, onResponse ->
            onResponse(
                HttpResponse(
                    statusCode = 0,
                    body = Value.Text(""),
                    errorMessage = "Unable to resolve host"
                )
            )
            Cancelable.Noop
        }

        var captured: Pair<Interaction, Value?>? = null
        dispatchHttp(State(), successEffect(), null, client) { interaction, eventValue ->
            captured = interaction to eventValue
        }

        assertEquals(successEffect().onError, captured?.first)
        val record = captured?.second as Value.Record
        assertEquals(Value.Number(0), record.fields["statusCode"])
        assertEquals(Value.Text("Unable to resolve host"), record.fields["error"])
    }

    @Test
    fun dispatchResolvesUrlHeadersAndBodyFromState() {
        val effect = Effect.Http(
            method = "POST",
            url = ValueSource.StateValue("endpoint"),
            headers = mapOf(
                "Authorization" to ValueSource.Template("Bearer {event}")
            ),
            body = ValueSource.StateValue("payload"),
            onSuccess = Interaction(),
            onError = Interaction()
        )
        val state = State(
            values = mapOf(
                "endpoint" to StateEntry(Value.Text("https://api.test/v1/users")),
                "payload" to StateEntry(Value.Text("{\"x\":1}"))
            )
        )

        var seenMethod: String? = null
        var seenUrl: String? = null
        var seenHeaders: Map<String, String>? = null
        var seenBody: String? = null
        val client = HttpClient { method, url, headers, body, onResponse ->
            seenMethod = method
            seenUrl = url
            seenHeaders = headers
            seenBody = body
            onResponse(HttpResponse(200, Value.Text("")))
            Cancelable.Noop
        }

        dispatchHttp(state, effect, eventValue = Value.Text("ABC"), client = client) { _, _ -> }

        assertEquals("POST", seenMethod)
        assertEquals("https://api.test/v1/users", seenUrl)
        assertEquals("Bearer ABC", seenHeaders?.get("Authorization"))
        assertEquals("{\"x\":1}", seenBody)
    }

    @Test
    fun dispatchReturnsCancelableFromClient() {
        // The whole point of Wave 4: the host gets a handle so it can cancel
        // the request when the issuing page leaves the stack. dispatchHttp
        // must propagate the client's Cancelable verbatim.
        var cancelled = false
        val sentinelCancelable = Cancelable { cancelled = true }
        val client = HttpClient { _, _, _, _, _ ->
            // Don't fire onResponse — simulate an in-flight request the host wants to cancel.
            sentinelCancelable
        }

        val handle = dispatchHttp(
            state = State(),
            effect = successEffect(),
            eventValue = null,
            client = client
        ) { _, _ -> /* no follow-up should fire */ }

        // Host invokes cancel via the registry — verify it routes back to client's Cancelable.
        handle.cancel()
        assertTrue(cancelled)
    }

    @Test
    fun dispatchPassesEventValueToOnFollowUp() {
        // Permissive equality should make the eventValue propagation chain visible.
        val effect = successEffect()
        val state = State()
        val responseBody = Value.List(items = listOf(Value.Number(1), Value.Number(2)))
        val client = HttpClient { _, _, _, _, onResponse ->
            onResponse(HttpResponse(204, responseBody))
            Cancelable.Noop
        }

        var sawListInCallback = false
        dispatchHttp(state, effect, null, client) { _, eventValue ->
            sawListInCallback = eventValue == responseBody
        }
        // 204 is in 200..299 so we should route success and pass the body verbatim.
        assertTrue(sawListInCallback)
    }
}
