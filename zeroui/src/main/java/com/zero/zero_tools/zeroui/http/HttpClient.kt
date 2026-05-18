package com.zero.zero_tools.zeroui.http

import com.zero.zero_tools.zeroui.value.Value

/**
 * Host-supplied HTTP transport.
 *
 * The library does NOT depend on any HTTP client implementation — the host plugs in
 * HttpURLConnection, OkHttp, Ktor, or anything else. The contract:
 *
 * - [request] may run async; it MUST NOT block the caller's thread.
 * - [onResponse] SHOULD be invoked on the main thread (so reducer state writes are safe).
 *   The default [UrlConnectionHttpClient] guarantees this by switching to
 *   [kotlinx.coroutines.Dispatchers.Main] before invoking the callback. Custom
 *   implementations should match that behavior. Implementations should also honour
 *   cancellation: if [Cancelable.cancel] was invoked before the response landed,
 *   [onResponse] should NOT fire.
 * - Implementations SHOULD restrict URL schemes they accept. The default
 *   [UrlConnectionHttpClient] short-circuits anything outside `http`/`https` to a
 *   transport-error response without opening a connection — server-driven JSON is
 *   not trusted input, and letting it specify `file://` / `content://` exposes
 *   host private storage. Custom implementations that need additional schemes
 *   should make the allowlist explicit at construction time.
 * - The body in [HttpResponse] should already be parsed via
 *   [com.zero.zero_tools.zeroui.value.parseRawValue] — the library does not inspect it further.
 * - The returned [Cancelable] is given to the host (via `onCancelable` in
 *   [com.zero.zero_tools.zeroui.effect.executeEffects]) so the host can stop the request
 *   when the issuing page leaves the stack. Implementations that cannot truly abort
 *   the underlying I/O should still return a Cancelable that suppresses [onResponse]
 *   so the page-scope guard layer stays effective.
 */
public fun interface HttpClient {
    public fun request(
        request: HttpRequest,
        onResponse: (HttpResponse) -> Unit
    ): Cancelable

    public companion object {
        /** Drops every request without ever invoking the callback. Useful in tests/previews. */
        public val Noop: HttpClient = HttpClient { _, _ -> Cancelable.Noop }
    }
}

public data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val timeoutMs: Int? = null,
    val retryCount: Int = 0,
    val retryDelayMs: Int = 250
)

public class HttpResponseCache(
    private val maxEntries: Int = 64
) {
    private val entries = object : LinkedHashMap<String, HttpResponse>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, HttpResponse>?): Boolean {
            return size > maxEntries
        }
    }

    public operator fun get(key: String): HttpResponse? = synchronized(entries) {
        entries[key]
    }

    public operator fun set(key: String, response: HttpResponse) {
        synchronized(entries) {
            entries[key] = response
        }
    }

    public fun clear() {
        synchronized(entries) {
            entries.clear()
        }
    }
}

/**
 * Parsed HTTP response — what gets fed back into the reducer as the event value
 * of `onSuccess` / `onError`.
 *
 * Status code 0 + non-null [errorMessage] indicates a transport-level failure
 * (no connection, timeout, malformed URL). 4xx/5xx with a parsed body is still
 * considered an HTTP "result" — the library routes it to `onError` because the
 * status code is not in 200..299, but the body is preserved.
 */
public data class HttpResponse(
    val statusCode: Int,
    val body: Value,
    val headers: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = statusCode in 200..299 && errorMessage == null
}
