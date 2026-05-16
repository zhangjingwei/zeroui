package com.zero.zero_tools.zeroui.host

import android.util.Log
import com.zero.zero_tools.zeroui.state.asText
import com.zero.zero_tools.zeroui.tracking.Tracker

private const val TrackerLogTag = "ZeroUiTrack"

public val LogcatTracker: Tracker = Tracker { event, params ->
    if (params.isEmpty()) {
        Log.i(TrackerLogTag, event)
    } else {
        val rendered = params.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "$key=${value.asText()}"
        }
        Log.i(TrackerLogTag, "$event $rendered")
    }
}
