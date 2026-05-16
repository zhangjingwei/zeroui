package com.zero.zero_tools.http

import com.zero.zero_tools.zeroui.http.Cancelable
import com.zero.zero_tools.zeroui.http.HttpClient
import com.zero.zero_tools.zeroui.http.HttpResponse
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.parseRawValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

/**
 * Zero-dependency [HttpClient] implementation. Uses [HttpURLConnection] on
 * [Dispatchers.IO] and delivers [HttpResponse] back on the dispatcher of [scope]
 * (which the host should bind to the main thread — typically `lifecycleScope`).
 *
 * Cancellation (Wave 6): the returned [Cancelable] both cancels the coroutine `Job`
 * AND calls `disconnect()` on the live `HttpURLConnection`. On Android / OpenJDK,
 * `disconnect()` of a connection in the middle of a blocking read/connect causes
 * the in-flight I/O to throw `IOException`, ending the IO call promptly and freeing
 * the socket. Without this, [Job.cancel] alone would only flip `isActive` to false —
 * the coroutine would keep blocking on `responseCode` / `inputStream.read` until
 * the server replied or the read timeout elapsed.
 *
 * Response bodies are parsed into [Value] via [parseRawValue]:
 * JSON objects → [Value.Record], JSON arrays → [Value.List], otherwise → [Value.Text].
 */
class UrlConnectionHttpClient(
    private val scope: CoroutineScope,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 15_000,
    /**
     * Test seam: lets tests inject a fake [HttpURLConnection] without going through
     * real DNS / socket. Production code uses the JDK default.
     */
    private val openConnection: (String) -> HttpURLConnection = { spec ->
        URL(spec).openConnection() as HttpURLConnection
    }
) : HttpClient {

    override fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
        onResponse: (HttpResponse) -> Unit
    ): Cancelable {
        // One AtomicReference per request. Holds the live HttpURLConnection between
        // "open" (inside runRequest) and "finally" (back inside runRequest) — or
        // "cancel" (from the host on the main thread).
        val connectionRef = AtomicReference<HttpURLConnection?>(null)

        val job = scope.launch {
            val response = withContext(Dispatchers.IO) {
                runRequest(method, url, headers, body, connectionRef)
            }
            // Wave 4 first-line defense: if the host cancelled this request while the
            // IO call was in flight, suppress onResponse so the late body does not
            // reach the reducer. The page-scope guard would also drop it, but failing
            // earlier saves work and is observable in tests.
            if (!isActive) return@launch
            onResponse(response)
        }

        return buildHttpCancelable(job, connectionRef)
    }

    private fun runRequest(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
        connectionRef: AtomicReference<HttpURLConnection?>
    ): HttpResponse {
        var connection: HttpURLConnection? = null
        return try {
            connection = openConnection(url).apply {
                requestMethod = method
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                useCaches = false
                doInput = true
                headers.forEach { (name, value) -> setRequestProperty(name, value) }
                if (body != null) {
                    doOutput = true
                    if (getRequestProperty("Content-Type") == null) {
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            // Publish the live connection so a concurrent cancel() can disconnect it.
            connectionRef.set(connection)

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val rawBody = stream?.use { it.toText() }.orEmpty()
            HttpResponse(
                statusCode = statusCode,
                body = rawBody.parseJsonValueOrText()
            )
        } catch (t: Throwable) {
            HttpResponse(
                statusCode = 0,
                body = Value.Text(""),
                errorMessage = t.message ?: t::class.simpleName.orEmpty()
            )
        } finally {
            // Only disconnect if WE still own the ref. If cancel() already swapped it
            // to null (and called disconnect itself), don't double-disconnect.
            // compareAndSet is the atomic "claim and clear" we need here.
            val live = connection
            if (live != null && connectionRef.compareAndSet(live, null)) {
                live.disconnect()
            }
        }
    }
}

/**
 * Builds the [Cancelable] returned to the host. Extracted so the disconnect routing
 * is unit-testable without spinning up the coroutine pipeline.
 *
 * Semantics:
 * - `job.cancel()` flips the coroutine's `isActive` to false; the suppression in
 *   [UrlConnectionHttpClient.request] then prevents `onResponse` from firing.
 * - `connectionRef.getAndSet(null)?.disconnect()` atomically takes the connection
 *   away from the IO thread's finally block (so it won't double-disconnect) and
 *   forcibly closes the socket — this is what makes a blocking `responseCode` /
 *   `read` actually return, instead of waiting up to `readTimeoutMs`.
 */
internal fun buildHttpCancelable(
    job: Job,
    connectionRef: AtomicReference<HttpURLConnection?>
): Cancelable = Cancelable {
    job.cancel()
    connectionRef.getAndSet(null)?.disconnect()
}

private fun java.io.InputStream.toText(): String =
    BufferedReader(InputStreamReader(this, Charsets.UTF_8)).use(BufferedReader::readText)

/**
 * Tries JSONObject first, then JSONArray; if both fail, returns the raw string as
 * [Value.Text]. Empty body becomes empty Text.
 */
private fun String.parseJsonValueOrText(): Value {
    if (isBlank()) return Value.Text("")
    return try {
        parseRawValue(JSONObject(this))
    } catch (_: JSONException) {
        try {
            parseRawValue(JSONArray(this))
        } catch (_: JSONException) {
            Value.Text(this)
        }
    }
}
