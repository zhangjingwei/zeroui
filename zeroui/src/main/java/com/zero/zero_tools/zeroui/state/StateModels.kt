package com.zero.zero_tools.zeroui.state

import com.zero.zero_tools.zeroui.value.Value

public data class State(
    val values: Map<String, StateEntry> = emptyMap()
)

public data class StateEntry(
    val value: Value,
    val owner: StateOwner = StateOwner.Client
)

public enum class StateOwner {
    Server,
    Client,
    Shared
}
