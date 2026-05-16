package com.zero.zero_tools.zeroui.state

import com.zero.zero_tools.zeroui.value.Value

data class State(
    val values: Map<String, StateEntry> = emptyMap()
)

data class StateEntry(
    val value: Value,
    val owner: StateOwner = StateOwner.Client
)

enum class StateOwner {
    Server,
    Client,
    Shared
}
