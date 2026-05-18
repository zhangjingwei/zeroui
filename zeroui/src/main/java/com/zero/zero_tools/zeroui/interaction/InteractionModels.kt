package com.zero.zero_tools.zeroui.interaction

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.effect.Effect

public data class Interaction(
    val id: String? = null,
    val debounceMillis: Int = 0,
    val throttleMillis: Int = 0,
    val actions: List<Action> = emptyList(),
    val effects: List<Effect> = emptyList()
)
