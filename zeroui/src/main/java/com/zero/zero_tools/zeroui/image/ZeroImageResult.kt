package com.zero.zero_tools.zeroui.image

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Result returned by a [ZeroImageLoader].
 *
 * Loaders MUST always invoke their `onResult` callback exactly once (either
 * [Success] or [Unavailable]) — unless the [com.zero.zero_tools.zeroui.http.Cancelable]
 * has been cancelled, in which case `onResult` should not fire.
 */
public sealed interface ZeroImageResult {
    public data class Success(public val bitmap: ImageBitmap) : ZeroImageResult
    public data object Unavailable : ZeroImageResult
}
