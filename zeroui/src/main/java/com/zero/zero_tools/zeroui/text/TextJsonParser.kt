package com.zero.zero_tools.zeroui.text

import org.json.JSONObject

internal fun parseText(json: JSONObject): Text {
    return when (val type = json.getString("type")) {
        "value" -> Text.Value(json.getString("value"))
        "binding" -> Text.Binding(
            key = json.getString("key"),
            fallback = json.optString("fallback", ""),
            format = json.optString("format").ifBlank { null }
        )

        else -> error("Unsupported ZeroUI text type: $type")
    }
}

internal fun JSONObject.optTextStyle(): TextStyle {
    return optString("style", "body").toTextStyle()
}

internal fun JSONObject.optTone(): Tone {
    return optString("tone", "default").toTone()
}

private fun String.toTextStyle(): TextStyle {
    return when (this) {
        "title" -> TextStyle.Title
        "sectionTitle" -> TextStyle.SectionTitle
        "body" -> TextStyle.Body
        "label" -> TextStyle.Label
        "support" -> TextStyle.Support
        else -> TextStyle.Body
    }
}

private fun String.toTone(): Tone {
    return when (this) {
        "default" -> Tone.Default
        "muted" -> Tone.Muted
        "primary" -> Tone.Primary
        "success" -> Tone.Success
        "error" -> Tone.Error
        "warning" -> Tone.Warning
        "inverse" -> Tone.Inverse
        else -> error("Unsupported ZeroUI tone: $this")
    }
}
