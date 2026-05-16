package com.zero.zero_tools.zeroui.host

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

        assertTrue(!allowed)
        assertEquals(1L to 2L, dropped)
    }

    @Test
    fun dropsWhenStackEmptied() {
        var allowed = false
        var dropped: Pair<Long, Long?>? = null

        val callback = pageScopedFollowUp(
            originEntryId = 7L,
            currentTopId = { null },
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
        var currentTop = 1L
        var allowCount = 0
        var dropCount = 0

        val callback = pageScopedFollowUp(
            originEntryId = 1L,
            currentTopId = { currentTop },
            onAllow = { _, _ -> allowCount++ },
            onDrop = { _, _ -> dropCount++ }
        )

        callback(sampleInteraction, null)
        currentTop = 2L
        callback(sampleInteraction, null)

        assertEquals(1, allowCount)
        assertEquals(1, dropCount)
    }

    @Test
    fun returningToOriginEntryReallowsFollowUp() {
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
        callback(sampleInteraction, null)
        currentTop = 2L
        callback(sampleInteraction, null)

        assertEquals(1, allowCount)
        assertEquals(1, dropCount)
    }
}
