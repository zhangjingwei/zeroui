package com.zero.zero_tools.zeroui.state

import com.zero.zero_tools.zeroui.value.Value

/**
 * Resolves [key] against the current [State].
 *
 * Supports dotted-path access into [Value.Record] entries — e.g. `item.title`
 * walks into the `item` record's `title` field. Used by `forEach` item scopes.
 */
fun State.getValue(key: String): Value? {
    if (!key.contains('.')) return values[key]?.value

    val parts = key.split('.')
    var current: Value? = values[parts.first()]?.value
    for (segment in parts.drop(1)) {
        val record = current as? Value.Record ?: return null
        current = record.fields[segment]
    }
    return current
}

fun State.getOwner(key: String): StateOwner? = values[key]?.owner

fun State.getText(key: String): String = getValue(key).asText()

fun State.getNumber(key: String): Int = getValue(key).asNumber()

fun State.getBoolean(key: String): Boolean = getValue(key).asBoolean()

fun State.getList(key: String): List<Value> {
    return when (val value = getValue(key)) {
        is Value.List -> value.items
        null -> emptyList()
        else -> emptyList()
    }
}

/**
 * Returns a derived [State] with `item` (and `index`) keys overlaid for use
 * inside `forEach` child rendering. Existing top-level keys are preserved;
 * `item` / `index` are shadowed if they happen to collide.
 */
fun State.withItemScope(item: Value, index: Int): State {
    return copy(
        values = values + mapOf(
            "item" to StateEntry(value = item, owner = StateOwner.Client),
            "index" to StateEntry(value = Value.Number(index), owner = StateOwner.Client)
        )
    )
}

fun Value?.asText(): String {
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
