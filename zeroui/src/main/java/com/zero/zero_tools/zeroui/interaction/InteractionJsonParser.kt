package com.zero.zero_tools.zeroui.interaction

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.value.parseValueSource
import org.json.JSONArray
import org.json.JSONObject

internal fun parseInteraction(json: JSONObject): Interaction {
    return Interaction(
        actions = json.optJSONArray("actions")?.mapObjects(::parseAction) ?: emptyList(),
        effects = json.optJSONArray("effects")?.mapObjects(::parseEffect) ?: emptyList()
    )
}

private fun parseAction(json: JSONObject): Action {
    return when (val type = json.getString("type")) {
        "setState" -> Action.SetState(
            key = json.getString("key"),
            value = parseValueSource(json.getJSONObject("value"))
        )

        "incrementState" -> Action.IncrementState(
            key = json.getString("key"),
            step = json.optInt("step", 1)
        )

        "toggleState" -> Action.ToggleState(
            key = json.getString("key")
        )

        "batch" -> Action.Batch(
            actions = json.getJSONArray("actions").mapObjects(::parseAction)
        )

        else -> error("Unsupported ZeroUI action type: $type")
    }
}

private fun parseEffect(json: JSONObject): Effect {
    return when (val type = json.getString("type")) {
        "toast" -> Effect.Toast(message = parseValueSource(json.getJSONObject("message")))
        "log" -> Effect.Log(message = parseValueSource(json.getJSONObject("message")))
        "navigate" -> Effect.Navigate(target = parseValueSource(json.getJSONObject("target")))
        "back" -> Effect.Back
        "track" -> Effect.Track(
            event = json.getString("event"),
            params = json.optJSONObject("params")?.let { paramsJson ->
                paramsJson.keys().asSequence().associateWith { key ->
                    parseValueSource(paramsJson.getJSONObject(key))
                }
            } ?: emptyMap()
        )
        "http" -> Effect.Http(
            method = json.optString("method", "GET"),
            url = parseValueSource(json.getJSONObject("url")),
            headers = json.optJSONObject("headers")?.let { headersJson ->
                headersJson.keys().asSequence().associateWith { key ->
                    parseValueSource(headersJson.getJSONObject(key))
                }
            } ?: emptyMap(),
            body = json.optJSONObject("body")?.let(::parseValueSource),
            onSuccess = json.optJSONObject("onSuccess")?.let(::parseInteraction) ?: Interaction(),
            onError = json.optJSONObject("onError")?.let(::parseInteraction) ?: Interaction()
        )
        else -> error("Unsupported ZeroUI effect type: $type")
    }
}

internal fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    return List(length()) { index -> transform(getJSONObject(index)) }
}
