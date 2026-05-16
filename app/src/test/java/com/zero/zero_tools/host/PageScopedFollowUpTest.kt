package com.zero.zero_tools.host

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageScopedFollowUpTest {

    private val sampleInteraction = Interaction(
        actions = listOf(
            Action.SetState(key = "x", value = ValueSource.Literal(Value.Number(1)))
        )
    )

    @Test
    fun firesOnAllowWhenTopMatchesOrigin() {
        var allowed: Pair<Interaction, Value?>? = null
        var dropped: Pair<Long, Long?>? = null

        val callback = pageScopedFollowUp(
            originEntryId = 42L,
            currentTopId = { 42L },
            onAllow = { i, v -> allowed = i to v },
            onDrop = { o, c -> dropped = o to c }
        )
        callback(sampleInteraction, Value.Text("evt"))

        assertEquals(sampleInteraction, allowed?.first)
        assertEquals(Value.Text("evt"), allowed?.second)
        assertNull(dropped)
    }

    @Test
    fun dropsWhenTopChanged() {
        var allowed = false
        var dropped: Pair<Long, Long?>? = null

        val callback = pageScopedFollowUp(
            originEntryId = 1L,
            currentTopId = { 2L },
            onAllow = { _, _ -> allowed = true },
            onDrop = { o, c -> dropped = o to c }
        )
        callback(sampleInteraction, null)

        assertTrue("onAllow must NOT fire when current top changed", !allowed)
        assertEquals(1L to 2L, dropped)
    }

    @Test
    fun dropsWhenStackEmptied() {
        var allowed = false
        var dropped: Pair<Long, Long?>? = null

        val callback = pageScopedFollowUp(
            originEntryId = 7L,
            currentTopId = { null },           // stack popped beyond origin
            onAllow = { _, _ -> allowed = true },
            onDrop = { o, c -> dropped = o to c }
        )
        callback(sampleInteraction, null)

        assertTrue(!allowed)
        assertEquals(7L, dropped?.first)
        assertNull(dropped?.second)
    }

    @Test
    fun readsCurrentTopFreshlyOnEachInvocation() {
        // Simulates: callback created when top=1; first invocation while still 1,
        // then user navigates so top=2, second invocation must now drop.
        var currentTop = 1L
        var allowCount = 0
        var dropCount = 0

        val callback = pageScopedFollowUp(
            originEntryId = 1L,
            currentTopId = { currentTop },
            onAllow = { _, _ -> allowCount++ },
            onDrop = { _, _ -> dropCount++ }
        )

        callback(sampleInteraction, null) // still on entry 1
        currentTop = 2L
        callback(sampleInteraction, null) // navigated away

        assertEquals(1, allowCount)
        assertEquals(1, dropCount)
    }

    @Test
    fun returningToOriginEntryReallowsFollowUp() {
        // Real-world case: user on entry 2, fires http, navigates to 3, navigates back to 2;
        // by the time the response lands, top is 2 again and follow-up should apply.
        var currentTop = 2L
        var allowCount = 0
        var dropCount = 0

        val callback = pageScopedFollowUp(
            originEntryId = 2L,
            currentTopId = { currentTop },
            onAllow = { _, _ -> allowCount++ },
            onDrop = { _, _ -> dropCount++ }
        )

        currentTop = 3L
        callback(sampleInteraction, null) // dropped while away
        currentTop = 2L
        callback(sampleInteraction, null) // back on origin → allowed

        assertEquals(1, allowCount)
        assertEquals(1, dropCount)
    }
}
