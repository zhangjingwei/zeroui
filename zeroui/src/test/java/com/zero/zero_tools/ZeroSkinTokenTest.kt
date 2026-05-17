package com.zero.zero_tools

import androidx.compose.ui.graphics.Color
import com.zero.zero_tools.zeroui.skin.ZeroButtonTokens
import com.zero.zero_tools.zeroui.skin.ZeroChipTokens
import com.zero.zero_tools.zeroui.skin.ZeroDensity
import com.zero.zero_tools.zeroui.skin.ZeroPalette
import org.junit.Assert.assertEquals
import org.junit.Test

class ZeroSkinTokenTest {

    @Test
    fun derivedChipTokensUseMutedUnselectedAndInverseSelected() {
        val tokens = ZeroChipTokens.fromPalette(TestPalette, ZeroDensity.Comfortable)

        assertEquals(TestPalette.inverseContainer, tokens.selected.container)
        assertEquals(TestPalette.inverseContent, tokens.selected.content)
        assertEquals(TestPalette.inverseContainer, tokens.selected.outline)

        assertEquals(TestPalette.mutedContainer, tokens.unselected.container)
        assertEquals(TestPalette.content, tokens.unselected.content)
        assertEquals(Color.Transparent, tokens.unselected.outline)
    }

    @Test
    fun derivedSecondaryButtonUsesNeutralContent() {
        val tokens = ZeroButtonTokens.fromPalette(TestPalette, ZeroDensity.Comfortable)

        assertEquals(Color.Transparent, tokens.secondary.colors.container)
        assertEquals(TestPalette.content, tokens.secondary.colors.content)
        assertEquals(Color.Transparent, tokens.secondary.colors.disabledContainer)
        assertEquals(TestPalette.content.copy(alpha = 0.38f), tokens.secondary.colors.disabledContent)
    }

    private companion object {
        private val TestPalette = ZeroPalette(
            content = Color(0xFF101010),
            mutedContent = Color(0xFF606060),
            primaryContent = Color(0xFF3355CC),
            successContent = Color(0xFF117744),
            errorContent = Color(0xFFCC3333),
            warningContent = Color(0xFF996600),
            container = Color(0xFFF8F8F8),
            mutedContainer = Color(0xFFECECEC),
            primaryContainer = Color(0xFFDDE5FF),
            successContainer = Color(0xFFDDF5EC),
            errorContainer = Color(0xFFFFE2E0),
            warningContainer = Color(0xFFFFF1C7),
            outline = Color(0xFFCCCCCC),
            mutedOutline = Color(0xFFDDDDDD),
            focusedOutline = Color(0xFF3355CC),
            errorOutline = Color(0xFFCC3333),
            unknownContainer = Color(0x11000000),
            inverseContent = Color(0xFFFFFFFF),
            inverseContainer = Color(0xFF101010)
        )
    }
}
