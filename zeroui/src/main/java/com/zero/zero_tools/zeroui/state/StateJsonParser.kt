package com.zero.zero_tools.zeroui.state

import com.zero.zero_tools.zeroui.value.parseRawValue
import org.json.JSONObject

internal fun parseState(json: JSONObject?): State {
    if (json == null) {
        return State()
    }

    return State(
        values = json.keys().asSequence().associateWith { key ->
            parseStateEntry(json.get(key))
        }
    )
}

private fun parseStateEntry(value: Any): StateEntry {
    if (value is JSONObject && value.has("value")) {
        return StateEntry(
            value = parseRawValue(value.get("value")),
            owner = value.optString("owner", "client").toStateOwner()
        )
    }

    return StateEntry(
        value = parseRawValue(value),
        owner = StateOwner.Client
    )
}

private fun String.toStateOwner(): StateOwner {
    return when (this) {
        "server" -> StateOwner.Server
        "client" -> StateOwner.Client
        "shared" -> StateOwner.Shared
        else -> error("Unsupported ZeroUI state owner: $this")
    }
}
