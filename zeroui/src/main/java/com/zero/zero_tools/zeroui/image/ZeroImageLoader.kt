package com.zero.zero_tools.zeroui.image

import com.zero.zero_tools.zeroui.http.Cancelable

/**
 * Host-supplied image transport for the ZeroUI `image` node.
 *
 * The library does NOT depend on any image-loading SDK — the host may plug in
 * Coil, Glide, Fresco, or rely on the bundled default
 * ([rememberDefaultZeroImageLoader]). Contract:
 *
 * - [load] MAY run async; it MUST NOT block the caller's thread.
 * - [onResult] MUST be invoked on the Android main thread so renderer state
 *   updates are safe.
 * - The returned [Cancelable] is registered with the host scope (per-page
 *   cancellation); implementations that cannot abort the underlying I/O
 *   should still return a [Cancelable] that suppresses [onResult].
 * - Loaders SHOULD cache by [ZeroImageRequest.source] so repeated rendering
 *   (recomposition, list scrolling) does not re-fetch.
 */
public fun interface ZeroImageLoader {
    public fun load(
        request: ZeroImageRequest,
        onResult: (ZeroImageResult) -> Unit
    ): Cancelable

    public companion object {
        /** Reports every request as [ZeroImageResult.Unavailable]. Useful for previews and tests. */
        public val Noop: ZeroImageLoader = ZeroImageLoader { _, onResult ->
            onResult(ZeroImageResult.Unavailable)
            Cancelable.Noop
        }
    }
}
