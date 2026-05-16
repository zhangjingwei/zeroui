package com.zero.zero_tools.zeroui.action

import com.zero.zero_tools.zeroui.value.ValueSource

sealed interface Action {
    data class SetState(
        val key: String,
        val value: ValueSource
    ) : Action

    data class IncrementState(
        val key: String,
        val step: Int = 1
    ) : Action

    data class ToggleState(
        val key: String
    ) : Action

    data class Batch(
        val actions: List<Action>
    ) : Action
}
