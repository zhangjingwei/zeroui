package com.zero.zero_tools.zeroui.skin

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import com.zero.zero_tools.zeroui.node.ButtonVariant
import com.zero.zero_tools.zeroui.text.TextStyle
import com.zero.zero_tools.zeroui.text.Tone

@Stable
public class ZeroStyleResolver(
    private val skin: ZeroSkin
) {
    public fun textStyle(style: TextStyle): ComposeTextStyle = when (style) {
        TextStyle.Display -> skin.typography.display
        TextStyle.Title -> skin.typography.title
        TextStyle.SectionTitle -> skin.typography.sectionTitle
        TextStyle.Body -> skin.typography.body
        TextStyle.Label -> skin.typography.label
        TextStyle.Support -> skin.typography.support
    }

    public fun contentColor(tone: Tone): Color = when (tone) {
        Tone.Default -> skin.palette.content
        Tone.Muted -> skin.palette.mutedContent
        Tone.Primary -> skin.palette.primaryContent
        Tone.Success -> skin.palette.successContent
        Tone.Error -> skin.palette.errorContent
        Tone.Warning -> skin.palette.warningContent
        Tone.Inverse -> skin.palette.inverseContent
    }

    public fun containerColor(tone: Tone): Color = when (tone) {
        Tone.Default -> skin.palette.container
        Tone.Muted -> skin.palette.mutedContainer
        Tone.Primary -> skin.palette.primaryContainer
        Tone.Success -> skin.palette.successContainer
        Tone.Error -> skin.palette.errorContainer
        Tone.Warning -> skin.palette.warningContainer
        Tone.Inverse -> skin.palette.inverseContainer
    }

    public fun buttonTokens(variant: ButtonVariant): ZeroButtonVariantTokens = when (variant) {
        ButtonVariant.Primary -> skin.components.button.primary
        ButtonVariant.Secondary -> skin.components.button.secondary
    }

    public fun fieldTokens(): ZeroFieldTokens = skin.components.field

    public fun chipTokens(selected: Boolean): ZeroChipTokens = skin.components.chip

    public fun switchTokens(): ZeroSwitchTokens = skin.components.switch

    public fun cardTokens(tone: Tone): ZeroCardResolvedTokens {
        val toneTokens = when (tone) {
            Tone.Default -> skin.components.card.default
            Tone.Muted -> skin.components.card.muted
            Tone.Primary -> skin.components.card.primary
            Tone.Success -> skin.components.card.success
            Tone.Error -> skin.components.card.error
            Tone.Warning -> skin.components.card.warning
            Tone.Inverse -> skin.components.card.inverse
        }
        return ZeroCardResolvedTokens(
            colors = toneTokens,
            cornerRadius = skin.components.card.cornerRadius
        )
    }

    public val bodyTextStyle: ComposeTextStyle
        get() = skin.typography.body

    public val labelTextStyle: ComposeTextStyle
        get() = skin.typography.label

    public val supportTextStyle: ComposeTextStyle
        get() = skin.typography.support

    public val errorContentColor: Color
        get() = skin.palette.errorContent

    public val mutedContentColor: Color
        get() = skin.palette.mutedContent

    public val unknownContainerColor: Color
        get() = skin.palette.unknownContainer
}
