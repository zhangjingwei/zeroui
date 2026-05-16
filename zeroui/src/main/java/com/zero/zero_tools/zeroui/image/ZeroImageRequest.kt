package com.zero.zero_tools.zeroui.image

/**
 * A single image-load intent handed to a [ZeroImageLoader].
 *
 * `targetWidthPx` / `targetHeightPx` are advisory hints — loaders may use them to
 * downsample, but are not required to honour them exactly. ZeroUI's default
 * [com.zero.zero_tools.zeroui.image.rememberDefaultZeroImageLoader] leaves both
 * `null` and caps the largest dimension at an internal heuristic; hosts that
 * plug in Coil/Glide should pass through these hints to the underlying loader
 * for precise sizing.
 */
public data class ZeroImageRequest(
    public val source: ZeroImageSource,
    public val targetWidthPx: Int? = null,
    public val targetHeightPx: Int? = null
)
