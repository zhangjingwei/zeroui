package com.zero.zero_tools.zeroui.expression

import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.asBoolean
import com.zero.zero_tools.zeroui.state.asNumber
import com.zero.zero_tools.zeroui.state.asText
import com.zero.zero_tools.zeroui.state.getValue
import com.zero.zero_tools.zeroui.value.Value

/**
 * Evaluates an [Expression] against the given state/event scope.
 *
 * The evaluator is total — it never throws on missing keys or type mismatches;
 * instead it coerces via [asText]/[asNumber]/[asBoolean] and falls back to
 * [Value.Text]("") so the renderer always has something to paint.
 */
public fun Expression.evaluate(state: State, eventValue: Value? = null): Value {
    return when (this) {
        is Expression.Literal -> value
        is Expression.Ref -> state.getValue(key) ?: Value.Text("")
        Expression.EventRef -> eventValue ?: Value.Text("")
        is Expression.Unary -> evalUnary(op, expr.evaluate(state, eventValue))
        is Expression.Binary -> evalBinary(
            op,
            lhs.evaluate(state, eventValue),
            rhs.evaluate(state, eventValue)
        )
        is Expression.IfElse -> if (cond.evaluate(state, eventValue).asBoolean()) {
            then.evaluate(state, eventValue)
        } else {
            otherwise.evaluate(state, eventValue)
        }
        is Expression.Call -> evalCall(name, args.map { it.evaluate(state, eventValue) })
    }
}

private fun evalUnary(op: UnaryOp, value: Value): Value = when (op) {
    UnaryOp.Not -> Value.Bool(!value.asBoolean())
    UnaryOp.Neg -> Value.Number(-value.asNumber())
}

private fun evalBinary(op: BinaryOp, lhs: Value, rhs: Value): Value = when (op) {
    BinaryOp.Add -> Value.Number(lhs.asNumber() + rhs.asNumber())
    BinaryOp.Sub -> Value.Number(lhs.asNumber() - rhs.asNumber())
    BinaryOp.Mul -> Value.Number(lhs.asNumber() * rhs.asNumber())
    BinaryOp.Div -> {
        val r = rhs.asNumber()
        Value.Number(if (r == 0) 0 else lhs.asNumber() / r)
    }
    BinaryOp.Mod -> {
        val r = rhs.asNumber()
        Value.Number(if (r == 0) 0 else lhs.asNumber() % r)
    }
    BinaryOp.Eq -> Value.Bool(valuesEqual(lhs, rhs))
    BinaryOp.Neq -> Value.Bool(!valuesEqual(lhs, rhs))
    BinaryOp.Lt -> Value.Bool(lhs.asNumber() < rhs.asNumber())
    BinaryOp.Gt -> Value.Bool(lhs.asNumber() > rhs.asNumber())
    BinaryOp.Le -> Value.Bool(lhs.asNumber() <= rhs.asNumber())
    BinaryOp.Ge -> Value.Bool(lhs.asNumber() >= rhs.asNumber())
    BinaryOp.And -> Value.Bool(lhs.asBoolean() && rhs.asBoolean())
    BinaryOp.Or -> Value.Bool(lhs.asBoolean() || rhs.asBoolean())
    BinaryOp.Concat -> Value.Text(lhs.asText() + rhs.asText())
    BinaryOp.Coalesce -> if (isEmpty(lhs)) rhs else lhs
}

private fun evalCall(name: String, args: List<Value>): Value = when (name) {
    "len" -> when (val v = args.firstOrNull()) {
        is Value.Text -> Value.Number(v.value.length)
        is Value.List -> Value.Number(v.items.size)
        else -> Value.Number(0)
    }
    "lower" -> Value.Text(args.firstOrNull().asText().lowercase())
    "upper" -> Value.Text(args.firstOrNull().asText().uppercase())
    "concat" -> Value.Text(args.joinToString(separator = "") { it.asText() })
    "coalesce" -> args.firstOrNull { !isEmpty(it) } ?: Value.Text("")
    "format" -> {
        // format("hi {0} you are {1}", name, age)
        val template = args.firstOrNull().asText()
        val rest = args.drop(1)
        var result = template
        rest.forEachIndexed { index, value ->
            result = result.replace("{$index}", value.asText())
        }
        Value.Text(result)
    }
    else -> Value.Text("")
}

private fun valuesEqual(a: Value, b: Value): Boolean {
    // Permissive equality: cross-type compare goes through text representation so
    // `{ref:count} == "3"` works without forcing the JSON author to know exact types.
    return when {
        a is Value.Number && b is Value.Number -> a.value == b.value
        a is Value.Bool && b is Value.Bool -> a.value == b.value
        a is Value.Text && b is Value.Text -> a.value == b.value
        else -> a.asText() == b.asText()
    }
}

private fun isEmpty(value: Value?): Boolean = when (value) {
    null -> true
    is Value.Text -> value.value.isEmpty()
    is Value.Number -> false
    is Value.Bool -> false
    is Value.List -> value.items.isEmpty()
    is Value.Record -> value.fields.isEmpty()
}
