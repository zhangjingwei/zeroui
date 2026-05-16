package com.zero.zero_tools

import com.zero.zero_tools.zeroui.core.parseZeroUiPage
import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.node.Node
import com.zero.zero_tools.zeroui.value.ValueSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers Navigate / Back / Track parsing — the Effect families that EffectExecutor
 * now dispatches through Navigator and Tracker.
 */
class ZeroUiEffectParserTest {

    @Test
    fun parsesNavigateEffectWithLiteralTarget() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "Go",
                  "onClick": {
                    "actions": [],
                    "effects": [
                      { "type": "navigate", "target": { "type": "literal", "value": "detail" } }
                    ]
                  }
                }
              }
            """.trimIndent()
        )

        val nav = (page.root as Node.Button).onClick.effects.single() as Effect.Navigate
        assertEquals(ValueSource.Literal(com.zero.zero_tools.zeroui.value.Value.Text("detail")), nav.target)
    }

    @Test
    fun parsesNavigateWithStateTarget() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "Go",
                  "onClick": {
                    "effects": [
                      { "type": "navigate", "target": { "type": "state", "key": "nextPage" } }
                    ]
                  }
                }
              }
            """.trimIndent()
        )
        val nav = (page.root as Node.Button).onClick.effects.single() as Effect.Navigate
        assertEquals(ValueSource.StateValue("nextPage"), nav.target)
    }

    @Test
    fun parsesBackEffect() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "Back",
                  "onClick": { "effects": [ { "type": "back" } ] }
                }
              }
            """.trimIndent()
        )
        val effect = (page.root as Node.Button).onClick.effects.single()
        assertTrue(effect === Effect.Back)
    }

    @Test
    fun parsesTrackEffectWithoutParams() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "T",
                  "onClick": { "effects": [ { "type": "track", "event": "tap" } ] }
                }
              }
            """.trimIndent()
        )
        val track = (page.root as Node.Button).onClick.effects.single() as Effect.Track
        assertEquals("tap", track.event)
        assertTrue(track.params.isEmpty())
    }

    @Test
    fun parsesTrackEffectWithMixedParams() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "T",
                  "onClick": {
                    "effects": [
                      {
                        "type": "track",
                        "event": "purchase",
                        "params": {
                          "sku": { "type": "state", "key": "selectedSku" },
                          "qty": { "type": "literal", "value": 1 },
                          "topic": { "type": "event" }
                        }
                      }
                    ]
                  }
                }
              }
            """.trimIndent()
        )
        val track = (page.root as Node.Button).onClick.effects.single() as Effect.Track
        assertEquals("purchase", track.event)
        assertEquals(3, track.params.size)
        assertEquals(ValueSource.StateValue("selectedSku"), track.params["sku"])
        assertEquals(ValueSource.Literal(com.zero.zero_tools.zeroui.value.Value.Number(1)), track.params["qty"])
        assertEquals(ValueSource.EventValue, track.params["topic"])
    }

    @Test
    fun parsesMultipleEffectsInOrder() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "Submit",
                  "onClick": {
                    "effects": [
                      { "type": "track", "event": "submit_clicked" },
                      { "type": "toast", "message": { "type": "literal", "value": "done" } },
                      { "type": "navigate", "target": { "type": "literal", "value": "thanks" } }
                    ]
                  }
                }
              }
            """.trimIndent()
        )
        val effects = (page.root as Node.Button).onClick.effects
        assertEquals(3, effects.size)
        assertTrue(effects[0] is Effect.Track)
        assertTrue(effects[1] is Effect.Toast)
        assertTrue(effects[2] is Effect.Navigate)
    }
}
