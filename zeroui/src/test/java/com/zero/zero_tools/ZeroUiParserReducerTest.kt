package com.zero.zero_tools

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.condition.Condition
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.StateEntry
import com.zero.zero_tools.zeroui.state.StateOwner
import com.zero.zero_tools.zeroui.state.getBoolean
import com.zero.zero_tools.zeroui.state.getNumber
import com.zero.zero_tools.zeroui.state.getOwner
import com.zero.zero_tools.zeroui.state.getText
import com.zero.zero_tools.zeroui.state.getValue
import com.zero.zero_tools.zeroui.state.reduceState
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeroUiParserReducerTest {
    @Test
    fun reduceInteractionUsesEventValueAndPreservesOwner() {
        val state = State(
            values = mapOf(
                "inputText" to StateEntry(
                    value = Value.Text("before"),
                    owner = StateOwner.Shared
                )
            )
        )
        val interaction = Interaction(
            actions = listOf(
                Action.SetState(
                    key = "inputText",
                    value = ValueSource.EventValue
                )
            )
        )

        val reduced = reduceState(
            state = state,
            interaction = interaction,
            eventValue = Value.Text("after")
        )

        assertEquals("after", reduced.getText("inputText"))
        assertEquals(StateOwner.Shared, reduced.getOwner("inputText"))
    }

    @Test
    fun reduceBatchAppliesActionsInOrder() {
        val state = State(
            values = mapOf(
                "clickCount" to StateEntry(Value.Number(1)),
                "isDetailVisible" to StateEntry(Value.Bool(false))
            )
        )
        val batch = Action.Batch(
            actions = listOf(
                Action.IncrementState(key = "clickCount", step = 2),
                Action.ToggleState(key = "isDetailVisible")
            )
        )

        val reduced = reduceState(state, batch)

        assertEquals(3, reduced.getNumber("clickCount"))
        assertTrue(reduced.getBoolean("isDetailVisible"))
    }

    @Test
    fun setStateWithLiteralWritesLiteralValue() {
        val state = State(values = mapOf("x" to StateEntry(Value.Number(0))))
        val reduced = reduceState(
            state,
            Action.SetState(key = "x", value = ValueSource.Literal(Value.Number(99)))
        )
        assertEquals(99, reduced.getNumber("x"))
    }

    @Test
    fun setStateWithStateValueCopiesAnotherKey() {
        val state = State(
            values = mapOf(
                "src" to StateEntry(Value.Text("hello")),
                "dst" to StateEntry(Value.Text(""))
            )
        )
        val reduced = reduceState(
            state,
            Action.SetState(key = "dst", value = ValueSource.StateValue("src"))
        )
        assertEquals("hello", reduced.getText("dst"))
    }

    @Test
    fun setStateWithTemplateInterpolatesEventValue() {
        val state = State(values = mapOf("greeting" to StateEntry(Value.Text(""))))
        val reduced = reduceState(
            state = state,
            action = Action.SetState(
                key = "greeting",
                value = ValueSource.Template("hi {event}")
            ),
            eventValue = Value.Text("Alice")
        )
        assertEquals("hi Alice", reduced.getText("greeting"))
    }

    @Test
    fun appendStateAddsItemsToExistingList() {
        val state = State(
            values = mapOf(
                "items" to StateEntry(Value.List(listOf(Value.Text("a"))))
            )
        )
        val reduced = reduceState(
            state,
            Action.AppendState(
                key = "items",
                value = ValueSource.Literal(Value.List(listOf(Value.Text("b"), Value.Text("c"))))
            )
        )

        assertEquals(
            listOf<Value>(Value.Text("a"), Value.Text("b"), Value.Text("c")),
            reduced.getValue("items")?.let { (it as Value.List).items }
        )
    }

    @Test
    fun incrementStateUsesCustomStep() {
        val state = State(values = mapOf("n" to StateEntry(Value.Number(5))))
        val reduced = reduceState(state, Action.IncrementState(key = "n", step = -3))
        assertEquals(2, reduced.getNumber("n"))
    }

    @Test
    fun incrementStateCreatesEntryWhenMissing() {
        val reduced = reduceState(State(), Action.IncrementState(key = "fresh", step = 1))
        assertEquals(1, reduced.getNumber("fresh"))
        // Newly created entries default to Client ownership.
        assertEquals(StateOwner.Client, reduced.getOwner("fresh"))
    }

    @Test
    fun toggleStateDoubleToggleRestoresOriginal() {
        val state = State(values = mapOf("on" to StateEntry(Value.Bool(true))))
        val once = reduceState(state, Action.ToggleState(key = "on"))
        val twice = reduceState(once, Action.ToggleState(key = "on"))
        assertEquals(true, twice.getBoolean("on"))
        assertEquals(false, once.getBoolean("on"))
    }

    @Test
    fun setStatePreservesShadowedOwner() {
        // Shared ownership must survive a SetState — server/shared keys should not
        // silently degrade to Client just because reducer touched them.
        val state = State(
            values = mapOf(
                "remote" to StateEntry(Value.Text("a"), owner = StateOwner.Server)
            )
        )
        val reduced = reduceState(
            state,
            Action.SetState(key = "remote", value = ValueSource.Literal(Value.Text("b")))
        )
        assertEquals("b", reduced.getText("remote"))
        assertEquals(StateOwner.Server, reduced.getOwner("remote"))
    }

    @Test
    fun setStateUpdatesDottedRecordPath() {
        val state = State(
            values = mapOf(
                "form" to StateEntry(
                    Value.Record(
                        mapOf(
                            "name" to Value.Text("before"),
                            "age" to Value.Number(3)
                        )
                    )
                )
            )
        )

        val reduced = reduceState(
            state,
            Action.SetState("form.name", ValueSource.Literal(Value.Text("after")))
        )

        assertEquals("after", reduced.getText("form.name"))
        assertEquals(3, reduced.getNumber("form.age"))
    }

    @Test
    fun setStateUpdatesListItemPath() {
        val state = State(
            values = mapOf(
                "items" to StateEntry(
                    Value.List(
                        listOf(
                            Value.Record(mapOf("title" to Value.Text("first"))),
                            Value.Record(mapOf("title" to Value.Text("second")))
                        )
                    )
                )
            )
        )

        val reduced = reduceState(
            state,
            Action.SetState("items[1].title", ValueSource.Literal(Value.Text("changed")))
        )

        assertEquals("first", reduced.getText("items[0].title"))
        assertEquals("changed", reduced.getText("items[1].title"))
    }

    @Test
    fun clearAndResetStateSupportKeysAndInitialState() {
        val initial = State(
            values = mapOf(
                "name" to StateEntry(Value.Text("initial")),
                "form" to StateEntry(Value.Record(mapOf("error" to Value.Text(""))))
            )
        )
        val state = State(
            values = mapOf(
                "name" to StateEntry(Value.Text("changed")),
                "form" to StateEntry(Value.Record(mapOf("error" to Value.Text("bad")))),
                "temp" to StateEntry(Value.Text("remove"))
            )
        )

        val cleared = reduceState(state, Action.ClearState(listOf("temp", "form.error")))
        assertEquals("", cleared.getText("temp"))
        assertEquals("", cleared.getText("form.error"))

        val reset = reduceState(
            state = state,
            action = Action.ResetState(listOf("name", "form.error")),
            initialState = initial
        )
        assertEquals("initial", reset.getText("name"))
        assertEquals("", reset.getText("form.error"))
    }

    @Test
    fun validateWritesMessageOnlyWhenConditionFails() {
        val state = State(values = mapOf("name" to StateEntry(Value.Text(""))))
        val invalid = reduceState(
            state,
            Action.Validate(
                condition = Condition.NotBlank("name"),
                errorKey = "nameError",
                message = ValueSource.Literal(Value.Text("Required"))
            )
        )
        assertEquals("Required", invalid.getText("nameError"))

        val valid = reduceState(
            state = invalid.copy(values = invalid.values + ("name" to StateEntry(Value.Text("Ada")))),
            action = Action.Validate(
                condition = Condition.NotBlank("name"),
                errorKey = "nameError",
                message = ValueSource.Literal(Value.Text("Required"))
            )
        )
        assertEquals("", valid.getText("nameError"))
    }
}
