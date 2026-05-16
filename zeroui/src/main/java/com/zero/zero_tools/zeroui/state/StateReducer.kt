package com.zero.zero_tools.zeroui.state

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.expression.evaluate
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource

fun reduceState(
    state: State,
    action: Action,
    eventValue: Value? = null
): State {
    return when (action) {
        is Action.SetState -> state.set(
            key = action.key,
            value = action.value.resolve(state, eventValue)
        )

        is Action.IncrementState -> state.set(
            key = action.key,
            value = Value.Number(state.getNumber(action.key) + action.step)
        )

        is Action.ToggleState -> state.set(
            key = action.key,
            value = Value.Bool(!state.getBoolean(action.key))
        )

        is Action.Batch -> action.actions.fold(state) { currentState, childAction ->
            reduceState(currentState, childAction, eventValue)
        }
    }
}

fun reduceState(
    state: State,
    interaction: Interaction,
    eventValue: Value? = null
): State {
    return interaction.actions.fold(state) { currentState, action ->
        reduceState(currentState, action, eventValue)
    }
}

fun resolveValueSource(
    state: State,
    source: ValueSource,
    eventValue: Value? = null
): Value {
    return source.resolve(state, eventValue)
}

private fun State.set(key: String, value: Value): State {
    val owner = getOwner(key) ?: StateOwner.Client
    return copy(values = values + (key to StateEntry(value = value, owner = owner)))
}

private fun ValueSource.resolve(state: State, eventValue: Value?): Value {
    return when (this) {
        is ValueSource.Literal -> value
        ValueSource.EventValue -> eventValue ?: Value.Text("")
        is ValueSource.StateValue -> state.getValue(key) ?: Value.Text("")
        is ValueSource.Template -> Value.Text(
            value.replace("{event}", eventValue.asText())
        )
        is ValueSource.Expr -> expression.evaluate(state, eventValue)
    }
}
