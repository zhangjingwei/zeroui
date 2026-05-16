package com.zero.zero_tools.zeroui.image

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zero.zero_tools.zeroui.http.Cancelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Default in-process image loader.
 *
 * Resources: looked up by name via `Resources.getIdentifier(name, "drawable", packageName)`.
 *
 * URLs: fetched via [HttpURLConnection]. Only `http` and `https` schemes are allowed —
 * `file://`, `content://`, `data:`, etc. are rejected to keep server-driven payloads
 * from reading the host process's private storage.
 *
 * Both paths use the standard `inJustDecodeBounds` + `inSampleSize` two-pass to keep
 * bitmaps under [maxDimensionPx]. Results are cached in a [LruCache] of [cacheMaxEntries]
 * entries keyed by source identity.
 *
 * Not production-grade: no disk cache, no request coalescing, no animated formats, no
 * priority ordering. Hosts running a heavy image workload should plug in Coil or Glide
 * via the [ZeroImageLoader] hook on `ZeroUiHost`.
 */
internal class DefaultZeroImageLoader(
    private val context: Context,
    private val scope: CoroutineScope,
    private val maxDimensionPx: Int = 2048,
    cacheMaxEntries: Int = 32,
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 5_000
) : ZeroImageLoader {

    private val cache: LruCache<String, ImageBitmap> = LruCache(cacheMaxEntries)

    override fun load(
        request: ZeroImageRequest,
        onResult: (ZeroImageResult) -> Unit
    ): Cancelable {
        val key = request.cacheKey()
        cache.get(key)?.let { cached ->
            onResult(ZeroImageResult.Success(cached))
            return Cancelable.Noop
        }

        val job = scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { decode(request) }.getOrNull()
            }
            if (!isActive) return@launch
            withContext(Dispatchers.Main) {
                if (bitmap == null) {
                    onResult(ZeroImageResult.Unavailable)
                } else {
                    cache.put(key, bitmap)
                    onResult(ZeroImageResult.Success(bitmap))
                }
            }
        }
        return Cancelable { job.cancel() }
    }

    private fun decode(request: ZeroImageRequest): ImageBitmap? {
        val cap = listOfNotNull(
            request.targetWidthPx,
            request.targetHeightPx,
            maxDimensionPx
        ).min().coerceAtLeast(1)

        return when (val src = request.source) {
            is ZeroImageSource.Resource -> decodeResource(src.name, cap)
            is ZeroImageSource.Url -> decodeUrl(src.value, cap)
        }
    }

    private fun decodeResource(name: String, maxDim: Int): ImageBitmap? {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, id, bounds)

        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        }
        return BitmapFactory.decodeResource(context.resources, id, opts)?.asImageBitmap()
    }

    private fun decodeUrl(url: String, maxDim: Int): ImageBitmap? {
        val parsed = runCatching { URL(url) }.getOrNull() ?: return null
        if (parsed.protocol.lowercase() !in AllowedSchemes) return null

        val bytes = downloadBytes(parsed) ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
    }

    private fun downloadBytes(url: URL): ByteArray? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                useCaches = true
            }
            connection.inputStream.use { it.readBytes() }
        } catch (_: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun calculateSampleSize(rawWidth: Int, rawHeight: Int, maxDim: Int): Int {
        if (rawWidth <= 0 || rawHeight <= 0) return 1
        var sample = 1
        while (rawWidth / sample > maxDim || rawHeight / sample > maxDim) {
            sample *= 2
        }
        return sample
    }

    private fun ZeroImageRequest.cacheKey(): String {
        return when (val s = source) {
            is ZeroImageSource.Resource -> "res:${s.name}"
            is ZeroImageSource.Url -> "url:${s.value}"
        }
    }

    private companion object {
        private val AllowedSchemes: Set<String> = setOf("http", "https")
    }
}
