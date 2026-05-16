package com.zero.zero_tools.zeroui.image

/**
 * Already-resolved image reference handed to a [ZeroImageLoader].
 *
 * The `Binding` variant of [com.zero.zero_tools.zeroui.node.ImageSource] is resolved
 * against the current [com.zero.zero_tools.zeroui.state.State] before the loader is
 * called, so loader implementations never need to know about state or expressions —
 * they only see a flat URL or resource name.
 */
public sealed interface ZeroImageSource {
    /** Remote image. Default loader requires `http` or `https` scheme; other schemes resolve to [ZeroImageResult.Unavailable]. */
    public data class Url(public val value: String) : ZeroImageSource

    /** Drawable resource lookup by name; resolved against the host app's `R.drawable` table. */
    public data class Resource(public val name: String) : ZeroImageSource
}
