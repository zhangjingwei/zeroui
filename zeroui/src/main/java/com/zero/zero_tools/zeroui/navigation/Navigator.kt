package com.zero.zero_tools.zeroui.navigation

/**
 * Host-supplied navigation contract for ZeroUI pages.
 *
 * The library does not know whether a "target" is an asset name, a deeplink, a URL,
 * or a route in some Compose Navigation graph — the host implements those semantics.
 *
 * Effects [com.zero.zero_tools.zeroui.effect.Effect.Navigate] / [com.zero.zero_tools.zeroui.effect.Effect.Back]
 * are translated into calls on this interface by [com.zero.zero_tools.zeroui.effect.executeEffects].
 */
public fun interface Navigator {
    public fun navigate(target: String)

    public fun back() {
        // default: no-op. Hosts that maintain a stack should override.
    }

    public companion object {
        /** Discards all navigation requests. Useful for previews and tests. */
        public val Noop: Navigator = object : Navigator {
            override fun navigate(target: String) = Unit
            override fun back() = Unit
        }
    }
}
