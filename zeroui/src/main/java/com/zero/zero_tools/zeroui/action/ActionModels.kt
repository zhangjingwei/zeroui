package com.zero.zero_tools.zeroui.action

import com.zero.zero_tools.zeroui.value.ValueSource

public sealed interface Action {
    public data class SetState(
        val key: String,
        val value: ValueSource
    ) : Action

    public data class IncrementState(
        val key: String,
        val step: Int = 1
    ) : Action

    public data class ToggleState(
        val key: String
    ) : Action

    public data class Batch(
        val actions: List<Action>
    ) : Action
}
