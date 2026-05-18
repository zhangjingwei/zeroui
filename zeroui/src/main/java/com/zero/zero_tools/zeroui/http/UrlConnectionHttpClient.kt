package com.zero.zero_tools.zeroui.http

import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.parseRawValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 * Default [HttpClient] implementation backed by [HttpURLConnection].
 *
 * Scheme allowlist: only `http` and `https` URLs are dispatched. Other schemes
 * (`file`, `content`, `data`, `ftp`, custom schemes) short-circuit to an
 * `errorMessage` response without ever opening a connection — this is an SDK-side
 * guard against server-driven JSON pointing the client at the host process's
 * private storage. Hosts that need additional schemes should implement [HttpClient]
 * directly.
 *
 * Threading: `onResponse` is invoked on `Dispatchers.Main`, satisfying the
 * [HttpClient] contract.
 */
public class UrlConnectionHttpClient(
    private val scope: CoroutineScope,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 15_000,
    private val openConnection: (String) -> HttpURLConnection = { spec ->
        URL(spec).openConnection() as HttpURLConnection
    }
) : HttpClient {

    override fun request(
        request: HttpRequest,
        onResponse: (HttpResponse) -> Unit
    ): Cancelable {
        val connectionRef = AtomicReference<HttpURLConnection?>(null)

        val job = scope.launch {
            var response: HttpResponse
            var attempt = 0
            var shouldRetry: Boolean
            do {
                response = withContext(Dispatchers.IO) {
                    runRequest(request, connectionRef)
                }
                shouldRetry = response.shouldRetry && attempt < request.retryCount && isActive
                if (shouldRetry && request.retryDelayMs > 0) {
                    delay(request.retryDelayMs.toLong())
                }
                attempt++
            } while (shouldRetry)
            if (!isActive) return@launch
            withContext(Dispatchers.Main) {
                if (isActive) {
                    onResponse(response)
                }
            }
        }

        return buildHttpCancelable(job, connectionRef)
    }

    private fun runRequest(
        request: HttpRequest,
        connectionRef: AtomicReference<HttpURLConnection?>
    ): HttpResponse {
        val url = request.url
        val parsed = runCatching { URL(url) }.getOrNull()
        if (parsed == null) {
            return transportError("Malformed URL: $url")
        }
        if (parsed.protocol.lowercase() !in AllowedSchemes) {
            return transportError("Disallowed URL scheme: ${parsed.protocol}")
        }

        var connection: HttpURLConnection? = null
        return try {
            connection = openConnection(url).apply {
                requestMethod = request.method
                connectTimeout = request.timeoutMs ?: connectTimeoutMs
                readTimeout = request.timeoutMs ?: readTimeoutMs
                useCaches = false
                doInput = true
                request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
                if (request.body != null) {
                    doOutput = true
                    if (getRequestProperty("Content-Type") == null) {
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                }
            }
            connectionRef.set(connection)
            if (request.body != null) {
                connection.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val rawBody = stream?.use { it.toText() }.orEmpty()
            HttpResponse(
                statusCode = statusCode,
                body = rawBody.parseJsonValueOrText(),
                headers = connection.headerFields.toFlatHeaders()
            )
        } catch (t: Throwable) {
            HttpResponse(
                statusCode = 0,
                body = Value.Text(""),
                errorMessage = t.message ?: t::class.simpleName.orEmpty()
            )
        } finally {
            val live = connection
            if (live != null && connectionRef.compareAndSet(live, null)) {
                live.disconnect()
            }
        }
    }

    private companion object {
        private val AllowedSchemes: Set<String> = setOf("http", "https")

        private fun transportError(message: String): HttpResponse = HttpResponse(
            statusCode = 0,
            body = Value.Text(""),
            errorMessage = message
        )
    }
}

private val HttpResponse.shouldRetry: Boolean
    get() = errorMessage != null || statusCode == 408 || statusCode == 429 || statusCode >= 500

private fun Map<String?, List<String>>.toFlatHeaders(): Map<String, String> {
    return entries.mapNotNull { (name, values) ->
        if (name == null) null else name to values.joinToString(",")
    }.toMap()
}

internal fun buildHttpCancelable(
    job: Job,
    connectionRef: AtomicReference<HttpURLConnection?>
): Cancelable = Cancelable {
    job.cancel()
    connectionRef.getAndSet(null)?.disconnect()
}

private fun java.io.InputStream.toText(): String =
    BufferedReader(InputStreamReader(this, Charsets.UTF_8)).use(BufferedReader::readText)

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
