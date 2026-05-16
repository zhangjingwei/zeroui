package com.zero.zero_tools

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.core.parseZeroUiPage
import com.zero.zero_tools.zeroui.effect.Effect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Page-level lifecycle hook (Wave 5): the `onMount` field at page top-level is parsed
 * as an Interaction. Host triggers it once per stack push / start composition.
 */
class ZeroUiOnMountParserTest {

    @Test
    fun parsesOnMountWithActionsAndEffects() {
        val page = parseZeroUiPage(
            """
              {
                "onMount": {
                  "actions": [
                    {
                      "type": "setState",
                      "key": "loading",
                      "value": { "type": "literal", "value": true }
                    }
                  ],
                  "effects": [
                    {
                      "type": "http",
                      "method": "GET",
                      "url": { "type": "literal", "value": "https://x.test" }
                    }
                  ]
                },
                "root": { "type": "column", "children": [] }
              }
            """.trimIndent()
        )

        assertEquals(1, page.onMount.actions.size)
        assertTrue(page.onMount.actions.single() is Action.SetState)
        assertEquals(1, page.onMount.effects.size)
        assertTrue(page.onMount.effects.single() is Effect.Http)
    }

    @Test
    fun missingOnMountDefaultsToEmptyInteraction() {
        val page = parseZeroUiPage(
            """{ "root": { "type": "column", "children": [] } }"""
        )
        assertTrue(page.onMount.actions.isEmpty())
        assertTrue(page.onMount.effects.isEmpty())
    }

    @Test
    fun emptyOnMountObjectParsesAsEmptyInteraction() {
        val page = parseZeroUiPage(
            """
              {
                "onMount": {},
                "root": { "type": "column", "children": [] }
              }
            """.trimIndent()
        )
        // Empty Interaction (no actions/effects keys) is the "fire onMount but do nothing"
        // shape. The host's fireOnMountIfAny treats this as a no-op.
        assertTrue(page.onMount.actions.isEmpty())
        assertTrue(page.onMount.effects.isEmpty())
    }
}
