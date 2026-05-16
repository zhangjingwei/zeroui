package com.zero.zero_tools.zeroui.skin

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import com.zero.zero_tools.zeroui.node.ButtonVariant
import com.zero.zero_tools.zeroui.text.TextStyle
import com.zero.zero_tools.zeroui.text.Tone

@Stable
class ZeroStyleResolver(
    private val skin: ZeroSkin
) {
    fun textStyle(style: TextStyle): ComposeTextStyle = when (style) {
        TextStyle.Title -> skin.typography.title
        TextStyle.SectionTitle -> skin.typography.sectionTitle
        TextStyle.Body -> skin.typography.body
        TextStyle.Label -> skin.typography.label
    }

    fun contentColor(tone: Tone): Color = when (tone) {
        Tone.Default -> skin.palette.content
        Tone.Muted -> skin.palette.mutedContent
        Tone.Primary -> skin.palette.primaryContent
        Tone.Success -> skin.palette.successContent
        Tone.Error -> skin.palette.errorContent
        Tone.Warning -> skin.palette.warningContent
    }

    fun containerColor(tone: Tone): Color = when (tone) {
        Tone.Default -> skin.palette.container
        Tone.Muted -> skin.palette.mutedContainer
        Tone.Primary -> skin.palette.primaryContainer
        Tone.Success -> skin.palette.successContainer
        Tone.Error -> skin.palette.errorContainer
        Tone.Warning -> skin.palette.warningContainer
    }

    fun buttonTokens(variant: ButtonVariant): ZeroButtonVariantTokens = when (variant) {
        ButtonVariant.Primary -> skin.components.button.primary
        ButtonVariant.Secondary -> skin.components.button.secondary
    }

    val bodyTextStyle: ComposeTextStyle
        get() = skin.typography.body

    val labelTextStyle: ComposeTextStyle
        get() = skin.typography.label

    val supportTextStyle: ComposeTextStyle
        get() = skin.typography.support

    val errorContentColor: Color
        get() = skin.palette.errorContent

    val mutedContentColor: Color
        get() = skin.palette.mutedContent

    val unknownContainerColor: Color
        get() = skin.palette.unknownContainer
}
