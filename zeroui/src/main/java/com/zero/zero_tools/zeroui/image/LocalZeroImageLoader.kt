package com.zero.zero_tools.zeroui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

/**
 * CompositionLocal carrying the active [ZeroImageLoader].
 *
 * [com.zero.zero_tools.zeroui.host.ZeroUiHost] installs a value when its
 * `imageLoader` parameter is bound; renderers retrieve it via
 * `LocalZeroImageLoader.current`. If no loader is installed the default
 * is [ZeroImageLoader.Noop], so renderers never crash and instead show the
 * unavailable placeholder.
 */
public val LocalZeroImageLoader: ProvidableCompositionLocal<ZeroImageLoader> = compositionLocalOf {
    ZeroImageLoader.Noop
}

/**
 * Remembers a process-wide default image loader bound to the current Compose scope.
 *
 * The default implementation:
 * - Decodes drawable resources via `BitmapFactory.decodeResource` with a two-pass
 *   `inJustDecodeBounds` + `inSampleSize` to cap memory.
 * - Fetches HTTP/HTTPS URLs via `HttpURLConnection`. Other URL schemes
 *   (`file`, `content`, `data`, …) resolve to [ZeroImageResult.Unavailable]
 *   — this is an SDK-side allowlist, not user-configurable.
 * - Holds a small in-memory LRU cache (32 entries) keyed by source.
 * - Decodes on `Dispatchers.IO` and posts results on `Dispatchers.Main`.
 *
 * For production traffic patterns (lazy lists with hundreds of remote images,
 * GIF/SVG support, disk caching, request prioritisation) hosts should plug in
 * a Coil- or Glide-backed [ZeroImageLoader] via the `imageLoader` parameter
 * of [com.zero.zero_tools.zeroui.host.ZeroUiHost].
 */
@Composable
public fun rememberDefaultZeroImageLoader(): ZeroImageLoader {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        DefaultZeroImageLoader(context = context, scope = scope)
    }
}
