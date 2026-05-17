package com.zero.zero_tools.zeroui.text

public sealed interface Text {
    public data class Value(val value: String) : Text

    public data class Binding(
        val key: String,
        val fallback: String,
        val format: String? = null
    ) : Text
}

public enum class TextStyle {
    Title,
    SectionTitle,
    Body,
    Label,
    Support
}

public enum class Tone {
    Default,
    Muted,
    Primary,
    Success,
    Error,
    Warning,
    Inverse
}
