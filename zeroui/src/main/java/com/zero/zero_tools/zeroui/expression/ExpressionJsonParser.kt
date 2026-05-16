package com.zero.zero_tools.zeroui.expression

import com.zero.zero_tools.zeroui.interaction.mapObjects
import com.zero.zero_tools.zeroui.value.parseRawValue
import org.json.JSONObject

/**
 * Parses a JSON expression node into an [Expression].
 *
 * Dialect (every node has a `type` discriminator):
 * - `{"type": "lit", "value": <primitive | array | object>}`
 * - `{"type": "ref", "key": "amount"}` — supports dotted paths
 * - `{"type": "event"}`
 * - `{"type": "binary", "op": "<", "lhs": <expr>, "rhs": <expr>}`
 * - `{"type": "unary", "op": "not", "expr": <expr>}`
 * - `{"type": "if", "cond": <expr>, "then": <expr>, "else": <expr>}`
 * - `{"type": "call", "name": "len", "args": [<expr>, ...]}`
 */
public fun parseExpression(json: JSONObject): Expression {
    return when (val type = json.getString("type")) {
        "lit" -> Expression.Literal(parseRawValue(json.get("value")))
        "ref" -> Expression.Ref(key = json.getString("key"))
        "event" -> Expression.EventRef
        "binary" -> Expression.Binary(
            op = parseBinaryOp(json.getString("op")),
            lhs = parseExpression(json.getJSONObject("lhs")),
            rhs = parseExpression(json.getJSONObject("rhs"))
        )
        "unary" -> Expression.Unary(
            op = parseUnaryOp(json.getString("op")),
            expr = parseExpression(json.getJSONObject("expr"))
        )
        "if" -> Expression.IfElse(
            cond = parseExpression(json.getJSONObject("cond")),
            then = parseExpression(json.getJSONObject("then")),
            otherwise = parseExpression(json.getJSONObject("else"))
        )
        "call" -> Expression.Call(
            name = json.getString("name"),
            args = json.optJSONArray("args")?.mapObjects(::parseExpression) ?: emptyList()
        )
        else -> error("Unsupported ZeroUI expression type: $type")
    }
}

private fun parseBinaryOp(symbol: String): BinaryOp = when (symbol) {
    "+" -> BinaryOp.Add
    "-" -> BinaryOp.Sub
    "*" -> BinaryOp.Mul
    "/" -> BinaryOp.Div
    "%" -> BinaryOp.Mod
    "==" -> BinaryOp.Eq
    "!=" -> BinaryOp.Neq
    "<" -> BinaryOp.Lt
    ">" -> BinaryOp.Gt
    "<=" -> BinaryOp.Le
    ">=" -> BinaryOp.Ge
    "and" -> BinaryOp.And
    "or" -> BinaryOp.Or
    "concat" -> BinaryOp.Concat
    "??" -> BinaryOp.Coalesce
    else -> error("Unsupported ZeroUI binary operator: $symbol")
}

private fun parseUnaryOp(symbol: String): UnaryOp = when (symbol) {
    "not" -> UnaryOp.Not
    "neg" -> UnaryOp.Neg
    else -> error("Unsupported ZeroUI unary operator: $symbol")
}
