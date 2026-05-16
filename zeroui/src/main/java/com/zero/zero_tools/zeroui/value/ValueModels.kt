package com.zero.zero_tools.zeroui.value

public sealed interface Value {
    public data class Text(val value: String) : Value
    public data class Number(val value: Int) : Value
    public data class Bool(val value: Boolean) : Value

    /** Ordered list of values; used by [com.zero.zero_tools.zeroui.node.Node.ForEach]. */
    public data class List(val items: kotlin.collections.List<Value>) : Value

    /** Named-field record; used so list items can expose `item.title` etc. through bindings. */
    public data class Record(val fields: Map<String, Value>) : Value
}

public sealed interface ValueSource {
    public data class Literal(val value: Value) : ValueSource
    public data object EventValue : ValueSource
    public data class StateValue(val key: String) : ValueSource
    public data class Template(val value: String) : ValueSource

    /**
     * Computed value driven by an [com.zero.zero_tools.zeroui.expression.Expression] AST.
     * Generalises Literal/StateValue/Template — used when the value needs to depend
     * on multiple keys, comparison, or string composition.
     */
    public data class Expr(
        val expression: com.zero.zero_tools.zeroui.expression.Expression
    ) : ValueSource
}
