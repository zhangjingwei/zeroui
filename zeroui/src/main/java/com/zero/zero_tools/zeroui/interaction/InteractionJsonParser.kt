package com.zero.zero_tools.zeroui.interaction

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.effect.NavigationTargetKind
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
        "navigate" -> parseNavigateEffect(json)
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

private fun parseNavigateEffect(json: JSONObject): Effect.Navigate {
    val targetJson = json.getJSONObject("target")
    val targetType = targetJson.optString("type", "")
    return when (targetType) {
        "page",
        "route",
        "url",
        "external" -> Effect.Navigate(
            target = targetJson.optJSONObject("value")?.let(::parseValueSource)
                ?: targetJson.optJSONObject("name")?.let(::parseValueSource)
                ?: error("ZeroUI navigate target '$targetType' requires value or name"),
            targetKind = targetType.toNavigationTargetKind()
        )
        else -> Effect.Navigate(
            target = parseValueSource(targetJson),
            targetKind = json.optString("targetKind", "page").toNavigationTargetKind()
        )
    }
}

private fun String.toNavigationTargetKind(): NavigationTargetKind {
    return when (this) {
        "page" -> NavigationTargetKind.Page
        "route" -> NavigationTargetKind.Route
        "url" -> NavigationTargetKind.Url
        "external" -> NavigationTargetKind.External
        else -> error("Unsupported ZeroUI navigation target kind: $this")
    }
}

internal fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    return List(length()) { index -> transform(getJSONObject(index)) }
}
