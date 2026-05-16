package com.zero.zero_tools.zeroui.condition

import com.zero.zero_tools.zeroui.expression.parseExpression
import org.json.JSONObject

internal fun parseCondition(json: JSONObject): Condition {
    return when (val type = json.getString("type")) {
        "truthy" -> Condition.Truthy(key = json.getString("key"))
        "notBlank" -> Condition.NotBlank(key = json.getString("key"))
        "expr" -> Condition.Expr(parseExpression(json.getJSONObject("expression")))
        else -> error("Unsupported ZeroUI condition type: $type")
    }
}
