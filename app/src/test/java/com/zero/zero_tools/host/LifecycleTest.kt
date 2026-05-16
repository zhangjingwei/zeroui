package com.zero.zero_tools.host

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecycleTest {

    private val nonEmptyAction = Interaction(
        actions = listOf(Action.SetState(key = "loading", value = ValueSource.Literal(Value.Bool(true))))
    )
    private val nonEmptyEffect = Interaction(
        effects = listOf(Effect.Log(message = ValueSource.Literal(Value.Text("hi"))))
    )
    private val empty = Interaction()

    @Test
    fun firesWhenInteractionHasActions() {
        var seen: Pair<Interaction, Value?>? = null
        val handle: (Interaction, Value?) -> Unit = { i, v -> seen = i to v }

        val fired = fireOnMountIfAny(nonEmptyAction, handle)

        assertTrue(fired)
        assertEquals(nonEmptyAction, seen?.first)
        assertNull(seen?.second) // onMount has no event value
    }

    @Test
    fun firesWhenInteractionHasOnlyEffects() {
        var fireCount = 0
        val handle: (Interaction, Value?) -> Unit = { _, _ -> fireCount++ }

        val fired = fireOnMountIfAny(nonEmptyEffect, handle)

        assertTrue(fired)
        assertEquals(1, fireCount)
    }

    @Test
    fun skipsEmptyInteractionWithoutCallingHandle() {
        var called = false
        val handle: (Interaction, Value?) -> Unit = { _, _ -> called = true }

        val fired = fireOnMountIfAny(empty, handle)

        assertFalse(fired)
        assertFalse("handler must not be called for empty onMount", called)
    }

    @Test
    fun nullHandleIsSafeAndReturnsFalse() {
        // Defensive: during the brief composition window before handlerRef is populated,
        // a navigator.navigate() could in principle race in. The helper must not NPE.
        val fired = fireOnMountIfAny(nonEmptyAction, handle = null)
        assertFalse(fired)
    }
}
