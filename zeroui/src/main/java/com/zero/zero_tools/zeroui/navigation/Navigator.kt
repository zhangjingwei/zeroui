package com.zero.zero_tools.zeroui.navigation

import com.zero.zero_tools.zeroui.effect.NavigationTargetKind

/**
 * Host-supplied navigation contract for ZeroUI pages.
 *
 * ZeroUI supplies target semantics through [NavigationTargetKind] rather than private
 * string schemes. The default [com.zero.zero_tools.zeroui.host.ZeroUiHost] consumes
 * [NavigationTargetKind.Page] targets as page names in its own stack.
 *
 * Effects [com.zero.zero_tools.zeroui.effect.Effect.Navigate] / [com.zero.zero_tools.zeroui.effect.Effect.Back]
 * are translated into calls on this interface by [com.zero.zero_tools.zeroui.effect.executeEffects].
 */
public fun interface Navigator {
    public fun navigate(target: String)

    public fun navigate(target: String, kind: NavigationTargetKind) {
        navigate(target)
    }

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
