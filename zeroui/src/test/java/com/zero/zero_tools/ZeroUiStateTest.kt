package com.zero.zero_tools

import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.StateEntry
import com.zero.zero_tools.zeroui.state.StateOwner
import com.zero.zero_tools.zeroui.state.getBoolean
import com.zero.zero_tools.zeroui.state.getList
import com.zero.zero_tools.zeroui.state.getNumber
import com.zero.zero_tools.zeroui.state.getOwner
import com.zero.zero_tools.zeroui.state.getText
import com.zero.zero_tools.zeroui.state.getValue
import com.zero.zero_tools.zeroui.state.withItemScope
import com.zero.zero_tools.zeroui.value.Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the read-side of State: typed accessors, dotted-path lookup into Records,
 * and the item-scope overlay used by forEach.
 */
class ZeroUiStateTest {

    @Test
    fun typedAccessorsCoerceAcrossPrimitiveValues() {
        val state = State(
            values = mapOf(
                "asNum" to StateEntry(Value.Text("42")),
                "asBool" to StateEntry(Value.Number(0)),
                "asText" to StateEntry(Value.Bool(true))
            )
        )

        assertEquals(42, state.getNumber("asNum"))
        assertFalse(state.getBoolean("asBool"))
        assertEquals("true", state.getText("asText"))
    }

    @Test
    fun missingKeyReturnsTypedDefaults() {
        val empty = State()
        assertNull(empty.getValue("nope"))
        assertEquals("", empty.getText("nope"))
        assertEquals(0, empty.getNumber("nope"))
        assertFalse(empty.getBoolean("nope"))
        assertTrue(empty.getList("nope").isEmpty())
    }

    @Test
    fun dottedPathReadsIntoRecord() {
        val state = State(
            values = mapOf(
                "user" to StateEntry(
                    Value.Record(
                        fields = mapOf(
                            "name" to Value.Text("Alice"),
                            "age" to Value.Number(30)
                        )
                    )
                )
            )
        )

        assertEquals(Value.Text("Alice"), state.getValue("user.name"))
        assertEquals("Alice", state.getText("user.name"))
        assertEquals(30, state.getNumber("user.age"))
    }

    @Test
    fun dottedPathReturnsNullWhenNotARecord() {
        val state = State(values = mapOf("user" to StateEntry(Value.Text("Alice"))))
        // user is a Text, not a Record — `.name` must not pretend to resolve.
        assertNull(state.getValue("user.name"))
    }

    @Test
    fun dottedPathReturnsNullForMissingField() {
        val state = State(
            values = mapOf(
                "user" to StateEntry(Value.Record(mapOf("name" to Value.Text("Alice"))))
            )
        )
        assertNull(state.getValue("user.missing"))
    }

    @Test
    fun withItemScopeOverlaysItemAndIndex() {
        val state = State(values = mapOf("title" to StateEntry(Value.Text("page"))))

        val item = Value.Record(
            fields = mapOf(
                "title" to Value.Text("buy milk"),
                "done" to Value.Bool(false)
            )
        )
        val scoped = state.withItemScope(item, index = 1)

        // Original state keys preserved.
        assertEquals("page", scoped.getText("title"))
        // item / item.field / index are now reachable.
        assertEquals("buy milk", scoped.getText("item.title"))
        assertFalse(scoped.getBoolean("item.done"))
        assertEquals(1, scoped.getNumber("index"))
    }

    @Test
    fun withItemScopeSupportsPrimitiveItems() {
        val state = State()
        val scoped = state.withItemScope(Value.Text("hello"), index = 0)
        assertEquals("hello", scoped.getText("item"))
        assertEquals(0, scoped.getNumber("index"))
    }

    @Test
    fun getListReturnsItemsAndCountsForBooleanCoercion() {
        val state = State(
            values = mapOf(
                "todos" to StateEntry(
                    Value.List(items = listOf(Value.Text("a"), Value.Text("b")))
                ),
                "empty" to StateEntry(Value.List(emptyList()))
            )
        )

        assertEquals(2, state.getList("todos").size)
        assertEquals(2, state.getNumber("todos"))          // List → size
        assertTrue(state.getBoolean("todos"))               // List → isNotEmpty
        assertFalse(state.getBoolean("empty"))
    }

    @Test
    fun ownerLookupReturnsRegisteredOwner() {
        val state = State(
            values = mapOf(
                "x" to StateEntry(Value.Text("v"), owner = StateOwner.Server)
            )
        )
        assertEquals(StateOwner.Server, state.getOwner("x"))
        assertNull(state.getOwner("missing"))
    }
}
