package com.zero.zero_tools.zeroui.text

sealed interface Text {
    data class Value(val value: String) : Text

    data class Binding(
        val key: String,
        val fallback: String,
        val format: String? = null
    ) : Text
}

enum class TextStyle {
    Title,
    SectionTitle,
    Body,
    Label
}

enum class Tone {
    Default,
    Muted,
    Primary,
    Success,
    Error,
    Warning
}
