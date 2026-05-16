package com.zero.zero_tools.zeroui.value

import com.zero.zero_tools.zeroui.expression.parseExpression
import org.json.JSONArray
import org.json.JSONObject

internal fun parseValueSource(json: JSONObject): ValueSource {
    return when (val type = json.getString("type")) {
        "literal" -> ValueSource.Literal(parseRawValue(json.get("value")))
        "event" -> ValueSource.EventValue
        "state" -> ValueSource.StateValue(key = json.getString("key"))
        "template" -> ValueSource.Template(json.getString("value"))
        "expr" -> ValueSource.Expr(parseExpression(json.getJSONObject("expression")))
        else -> error("Unsupported ZeroUI value source type: $type")
    }
}

/**
 * Wraps any JSON-shaped value into a [Value]:
 * - JSONObject → [Value.Record]
 * - JSONArray  → [Value.List]
 * - Boolean / Number / everything else → corresponding primitive (text as fallback).
 *
 * Public so hosts can map HTTP / WebSocket payloads into the same Value shape the
 * reducer / renderer consumes.
 */
fun parseRawValue(value: Any): Value {
    return when (value) {
        is Boolean -> Value.Bool(value)
        is Int -> Value.Number(value)
        is Number -> Value.Number(value.toInt())
        is JSONArray -> Value.List(
            items = List(value.length()) { index -> parseRawValue(value.get(index)) }
        )
        is JSONObject -> Value.Record(
            fields = value.keys().asSequence().associateWith { key ->
                parseRawValue(value.get(key))
            }
        )
        else -> Value.Text(value.toString())
    }
}
