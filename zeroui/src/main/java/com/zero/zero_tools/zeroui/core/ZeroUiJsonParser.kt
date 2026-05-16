package com.zero.zero_tools.zeroui.core

import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.interaction.parseInteraction
import com.zero.zero_tools.zeroui.node.parseNode
import com.zero.zero_tools.zeroui.page.Page
import com.zero.zero_tools.zeroui.page.ZeroUiSchemaVersion
import com.zero.zero_tools.zeroui.state.parseState
import org.json.JSONObject

public fun parseZeroUiPage(json: String): Page {
    val page = JSONObject(json)

    return Page(
        schemaVersion = page.optInt("schemaVersion", ZeroUiSchemaVersion.Current),
        initialState = parseState(page.optJSONObject("initialState")),
        root = parseNode(page.getJSONObject("root")),
        onMount = page.optJSONObject("onMount")?.let(::parseInteraction) ?: Interaction()
    )
}
