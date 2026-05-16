package com.zero.zero_tools.zeroui.condition

sealed interface Condition {
    data class Truthy(val key: String) : Condition
    data class NotBlank(val key: String) : Condition

    /**
     * Generic condition driven by an [com.zero.zero_tools.zeroui.expression.Expression].
     * The expression is evaluated against the current state and its result is coerced to Bool.
     */
    data class Expr(
        val expression: com.zero.zero_tools.zeroui.expression.Expression
    ) : Condition
}
