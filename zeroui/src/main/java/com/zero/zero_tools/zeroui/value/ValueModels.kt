package com.zero.zero_tools.zeroui.value

sealed interface Value {
    data class Text(val value: String) : Value
    data class Number(val value: Int) : Value
    data class Bool(val value: Boolean) : Value

    /** Ordered list of values; used by [com.zero.zero_tools.zeroui.node.Node.ForEach]. */
    data class List(val items: kotlin.collections.List<Value>) : Value

    /** Named-field record; used so list items can expose `item.title` etc. through bindings. */
    data class Record(val fields: Map<String, Value>) : Value
}

sealed interface ValueSource {
    data class Literal(val value: Value) : ValueSource
    data object EventValue : ValueSource
    data class StateValue(val key: String) : ValueSource
    data class Template(val value: String) : ValueSource

    /**
     * Computed value driven by an [com.zero.zero_tools.zeroui.expression.Expression] AST.
     * Generalises Literal/StateValue/Template — used when the value needs to depend
     * on multiple keys, comparison, or string composition.
     */
    data class Expr(
        val expression: com.zero.zero_tools.zeroui.expression.Expression
    ) : ValueSource
}
