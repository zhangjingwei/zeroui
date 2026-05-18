package com.zero.zero_tools.zeroui.state

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.expression.evaluate
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.value.matches
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource

public fun reduceState(
    state: State,
    action: Action,
    eventValue: Value? = null,
    initialState: State = State()
): State {
    return when (action) {
        is Action.SetState -> state.set(
            key = action.key,
            value = action.value.resolve(state, eventValue)
        )

        is Action.AppendState -> state.append(
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
            reduceState(currentState, childAction, eventValue, initialState)
        }

        is Action.ClearState -> action.keys.fold(state) { currentState, key ->
            currentState.clear(key)
        }

        is Action.ResetState -> action.keys.fold(state) { currentState, key ->
            currentState.reset(key, initialState)
        }

        is Action.Validate -> {
            val message = if (action.condition.matches(state)) {
                Value.Text("")
            } else {
                action.message.resolve(state, eventValue)
            }
            state.set(action.errorKey, message)
        }
    }
}

public fun reduceState(
    state: State,
    interaction: Interaction,
    eventValue: Value? = null,
    initialState: State = State()
): State {
    return interaction.actions.fold(state) { currentState, action ->
        reduceState(currentState, action, eventValue, initialState)
    }
}

public fun resolveValueSource(
    state: State,
    source: ValueSource,
    eventValue: Value? = null
): Value {
    return source.resolve(state, eventValue)
}

private fun State.append(key: String, value: Value): State {
    val current = getValue(key)
    val nextItems = when (current) {
        is Value.List -> current.items
        null -> emptyList()
        else -> listOf(current)
    } + when (value) {
        is Value.List -> value.items
        else -> listOf(value)
    }
    return set(key, Value.List(nextItems))
}

private fun State.set(key: String, value: Value): State {
    if (key.hasPath()) return setPath(key, value)
    val owner = getOwner(key) ?: StateOwner.Client
    return copy(values = values + (key to StateEntry(value = value, owner = owner)))
}

private fun State.clear(key: String): State {
    if (!key.hasPath()) return copy(values = values - key)
    val segments = key.pathSegments()
    val top = segments.firstOrNull() ?: return this
    val topKey = top.name
    val current = values[topKey] ?: return this
    val childSegments = if (top.index != null) {
        listOf(PathSegment(name = "", index = top.index)) + segments.drop(1)
    } else {
        segments.drop(1)
    }
    val nextValue = current.value.updatePath(childSegments, null)
    return if (nextValue == null) {
        copy(values = values - topKey)
    } else {
        copy(values = values + (topKey to current.copy(value = nextValue)))
    }
}

private fun State.reset(key: String, initialState: State): State {
    val initialValue = initialState.getValue(key)
    return if (initialValue == null) clear(key) else set(key, initialValue)
}

private fun State.setPath(key: String, value: Value): State {
    val segments = key.pathSegments()
    val top = segments.firstOrNull() ?: return this
    val current = values[top.name]
    val childSegments = if (top.index != null) {
        listOf(PathSegment(name = "", index = top.index)) + segments.drop(1)
    } else {
        segments.drop(1)
    }
    val nextValue = (current?.value ?: Value.Record(emptyMap()))
        .updatePath(childSegments, value)
        ?: value
    val owner = current?.owner ?: StateOwner.Client
    return copy(values = values + (top.name to StateEntry(value = nextValue, owner = owner)))
}

private data class PathSegment(
    val name: String,
    val index: Int? = null
)

private fun String.hasPath(): Boolean = contains('.') || contains('[')

private fun String.pathSegments(): List<PathSegment> {
    return split('.').filter { it.isNotBlank() }.map { raw ->
        val bracketStart = raw.indexOf('[')
        if (bracketStart < 0) {
            PathSegment(name = raw)
        } else {
            val name = raw.substring(0, bracketStart)
            val index = raw.substringAfter('[').substringBefore(']').toIntOrNull()
            PathSegment(name = name, index = index)
        }
    }
}

private fun Value.updatePath(segments: List<PathSegment>, value: Value?): Value? {
    if (segments.isEmpty()) return value
    val segment = segments.first()
    val rest = segments.drop(1)
    return when {
        segment.index != null && segment.name.isNotBlank() -> updateRecordListPath(segment, rest, value)
        segment.index != null -> updateListIndexPath(segment.index, rest, value)
        else -> updateRecordPath(segment.name, rest, value)
    }
}

private fun Value.updateRecordPath(
    name: String,
    rest: List<PathSegment>,
    value: Value?
): Value {
    val fields = (this as? Value.Record)?.fields.orEmpty()
    val current = fields[name] ?: Value.Record(emptyMap())
    val next = current.updatePath(rest, value)
    val nextFields = if (next == null) fields - name else fields + (name to next)
    return Value.Record(nextFields)
}

private fun Value.updateListPath(
    segment: PathSegment,
    rest: List<PathSegment>,
    value: Value?
): Value {
    return updateRecordListPath(segment, rest, value)
}

private fun Value.updateRecordListPath(
    segment: PathSegment,
    rest: List<PathSegment>,
    value: Value?
): Value {
    val index = segment.index ?: return this
    val fields = (this as? Value.Record)?.fields.orEmpty()
    val listValue = fields[segment.name] ?: Value.List(emptyList())
    val nextList = listValue.updateListIndexPath(index, rest, value)
    return Value.Record(fields + (segment.name to nextList))
}

private fun Value.updateListIndexPath(
    index: Int,
    rest: List<PathSegment>,
    value: Value?
): Value {
    val existingItems = (this as? Value.List)?.items.orEmpty()
    val size = maxOf(existingItems.size, index + 1)
    val items = MutableList<Value>(size) { existingItems.getOrNull(it) ?: Value.Record(emptyMap()) }
    val current = items[index]
    val next = current.updatePath(rest, value) ?: Value.Record(emptyMap())
    items[index] = next
    return Value.List(items)
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
