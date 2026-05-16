package com.zero.zero_tools.zeroui.expression

import com.zero.zero_tools.zeroui.value.Value

/**
 * Small AST-based expression language for ZeroUI.
 *
 * Lives one layer below [com.zero.zero_tools.zeroui.value.ValueSource]: a ValueSource is
 * what gets bound to an interaction or a render; an Expression is what produces a [Value]
 * from a State + event scope. Expressions can be wrapped in [com.zero.zero_tools.zeroui.value.ValueSource.Expr]
 * and in [com.zero.zero_tools.zeroui.condition.Condition.Expr].
 *
 * JSON form: every node carries a `type` discriminator. See ExpressionJsonParser for the dialect.
 */
public sealed interface Expression {

    /** A literal value baked into the expression. */
    public data class Literal(val value: Value) : Expression

    /** Reads a state key. Supports dotted-path access into [Value.Record] (e.g. `item.title`). */
    public data class Ref(val key: String) : Expression

    /** Returns the current event value (sender of an interaction) or [Value.Text]("") when absent. */
    public data object EventRef : Expression

    /** Binary operator: arithmetic, comparison, logical, string concat, null-coalescing. */
    public data class Binary(
        val op: BinaryOp,
        val lhs: Expression,
        val rhs: Expression
    ) : Expression

    /** Unary operator: logical not, numeric negation. */
    public data class Unary(
        val op: UnaryOp,
        val expr: Expression
    ) : Expression

    /** Ternary: `cond ? then : otherwise`. */
    public data class IfElse(
        val cond: Expression,
        val then: Expression,
        val otherwise: Expression
    ) : Expression

    /** Built-in function call (e.g. `len`, `lower`, `upper`, `coalesce`). */
    public data class Call(
        val name: String,
        val args: List<Expression>
    ) : Expression
}

public enum class BinaryOp {
    Add, Sub, Mul, Div, Mod,
    Eq, Neq, Lt, Gt, Le, Ge,
    And, Or,
    Concat,    // string concatenation
    Coalesce   // first non-empty / non-null
}

public enum class UnaryOp {
    Not,
    Neg
}
