package com.zero.zero_tools.zeroui.state

import com.zero.zero_tools.zeroui.value.Value

/**
 * Resolves [key] against the current [State].
 *
 * Supports dotted-path access into [Value.Record] entries — e.g. `item.title`
 * walks into the `item` record's `title` field. Used by `forEach` item scopes.
 */
public fun State.getValue(key: String): Value? {
    val parts = key.split('.').filter { it.isNotBlank() }
    if (parts.isEmpty()) return null

    var current: Value? = values[parts.first().substringBefore('[')]?.value
    current = current.resolveBracket(parts.first())
    for (segment in parts.drop(1)) {
        current = current.readSegment(segment)
    }
    return current
}

private fun Value?.readSegment(segment: String): Value? {
    val name = segment.substringBefore('[')
    val fromRecord = if (name.isBlank()) this else (this as? Value.Record)?.fields?.get(name)
    return fromRecord.resolveBracket(segment)
}

private fun Value?.resolveBracket(segment: String): Value? {
    if (!segment.contains('[')) return this
    val index = segment.substringAfter('[').substringBefore(']').toIntOrNull() ?: return null
    return (this as? Value.List)?.items?.getOrNull(index)
}

public fun State.getOwner(key: String): StateOwner? = values[key]?.owner

public fun State.getText(key: String): String = getValue(key).asText()

public fun State.getNumber(key: String): Int = getValue(key).asNumber()

public fun State.getBoolean(key: String): Boolean = getValue(key).asBoolean()

public fun State.getList(key: String): List<Value> {
    return when (val value = getValue(key)) {
        is Value.List -> value.items
        else -> emptyList()
    }
}

/**
 * Returns a derived [State] with `item` (and `index`) keys overlaid for use
 * inside `forEach` child rendering. Existing top-level keys are preserved;
 * `item` / `index` are shadowed if they happen to collide.
 */
public fun State.withItemScope(item: Value, index: Int): State {
    return copy(
        values = values + mapOf(
            "item" to StateEntry(value = item, owner = StateOwner.Client),
            "index" to StateEntry(value = Value.Number(index), owner = StateOwner.Client)
        )
    )
}

public fun Value?.asText(): String {
    return when (this) {
        is Value.Text -> value
        is Value.Number -> value.toString()
        is Value.Bool -> value.toString()
        is Value.List -> ""
        is Value.Record -> ""
        null -> ""
    }
}

internal fun Value?.asNumber(): Int {
    return when (this) {
        is Value.Number -> value
        is Value.Text -> value.toIntOrNull() ?: 0
        is Value.Bool -> if (value) 1 else 0
        is Value.List -> items.size
        is Value.Record -> 0
        null -> 0
    }
}

internal fun Value?.asBoolean(): Boolean {
    return when (this) {
        is Value.Bool -> value
        is Value.Number -> value != 0
        is Value.Text -> value.isNotBlank() && value != "false"
        is Value.List -> items.isNotEmpty()
        is Value.Record -> fields.isNotEmpty()
        null -> false
    }
}
