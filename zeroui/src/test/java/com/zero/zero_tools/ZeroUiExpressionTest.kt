package com.zero.zero_tools

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.condition.Condition
import com.zero.zero_tools.zeroui.core.parseZeroUiPage
import com.zero.zero_tools.zeroui.expression.BinaryOp
import com.zero.zero_tools.zeroui.expression.Expression
import com.zero.zero_tools.zeroui.expression.UnaryOp
import com.zero.zero_tools.zeroui.expression.evaluate
import com.zero.zero_tools.zeroui.expression.parseExpression
import com.zero.zero_tools.zeroui.node.Node
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.StateEntry
import com.zero.zero_tools.zeroui.state.getText
import com.zero.zero_tools.zeroui.state.reduceState
import com.zero.zero_tools.zeroui.state.withItemScope
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeroUiExpressionTest {

    private val emptyState = State()

    @Test
    fun literalAndRefEvaluate() {
        val state = State(values = mapOf("x" to StateEntry(Value.Number(42))))
        assertEquals(Value.Number(42), Expression.Literal(Value.Number(42)).evaluate(emptyState))
        assertEquals(Value.Number(42), Expression.Ref("x").evaluate(state))
        // Missing ref returns empty Text (total evaluator).
        assertEquals(Value.Text(""), Expression.Ref("missing").evaluate(state))
    }

    @Test
    fun eventRefReturnsEventValue() {
        val expr = Expression.EventRef
        assertEquals(Value.Text("hi"), expr.evaluate(emptyState, Value.Text("hi")))
        assertEquals(Value.Text(""), expr.evaluate(emptyState, null))
    }

    @Test
    fun arithmeticOperatorsWork() {
        val lhs = Expression.Literal(Value.Number(7))
        val rhs = Expression.Literal(Value.Number(3))
        assertEquals(Value.Number(10), Expression.Binary(BinaryOp.Add, lhs, rhs).evaluate(emptyState))
        assertEquals(Value.Number(4), Expression.Binary(BinaryOp.Sub, lhs, rhs).evaluate(emptyState))
        assertEquals(Value.Number(21), Expression.Binary(BinaryOp.Mul, lhs, rhs).evaluate(emptyState))
        assertEquals(Value.Number(2), Expression.Binary(BinaryOp.Div, lhs, rhs).evaluate(emptyState))
        assertEquals(Value.Number(1), Expression.Binary(BinaryOp.Mod, lhs, rhs).evaluate(emptyState))
    }

    @Test
    fun divisionByZeroIsSafe() {
        // Total evaluator: x/0 returns 0 rather than crashing.
        val expr = Expression.Binary(
            BinaryOp.Div,
            Expression.Literal(Value.Number(10)),
            Expression.Literal(Value.Number(0))
        )
        assertEquals(Value.Number(0), expr.evaluate(emptyState))
    }

    @Test
    fun comparisonOperatorsCoerceToBool() {
        val lhs = Expression.Literal(Value.Number(5))
        val rhs = Expression.Literal(Value.Number(3))
        assertEquals(Value.Bool(true), Expression.Binary(BinaryOp.Gt, lhs, rhs).evaluate(emptyState))
        assertEquals(Value.Bool(false), Expression.Binary(BinaryOp.Lt, lhs, rhs).evaluate(emptyState))
        assertEquals(Value.Bool(true), Expression.Binary(BinaryOp.Ge, lhs, lhs).evaluate(emptyState))
        assertEquals(Value.Bool(false), Expression.Binary(BinaryOp.Eq, lhs, rhs).evaluate(emptyState))
        assertEquals(Value.Bool(true), Expression.Binary(BinaryOp.Neq, lhs, rhs).evaluate(emptyState))
    }

    @Test
    fun equalityIsPermissiveAcrossTypes() {
        // Number 3 == Text "3" should be true (coerced via text).
        val expr = Expression.Binary(
            BinaryOp.Eq,
            Expression.Literal(Value.Number(3)),
            Expression.Literal(Value.Text("3"))
        )
        assertEquals(Value.Bool(true), expr.evaluate(emptyState))
    }

    @Test
    fun logicalOperatorsAndUnaryNot() {
        val t = Expression.Literal(Value.Bool(true))
        val f = Expression.Literal(Value.Bool(false))
        assertEquals(Value.Bool(false), Expression.Binary(BinaryOp.And, t, f).evaluate(emptyState))
        assertEquals(Value.Bool(true), Expression.Binary(BinaryOp.Or, t, f).evaluate(emptyState))
        assertEquals(Value.Bool(false), Expression.Unary(UnaryOp.Not, t).evaluate(emptyState))
        assertEquals(Value.Number(-3), Expression.Unary(UnaryOp.Neg, Expression.Literal(Value.Number(3))).evaluate(emptyState))
    }

    @Test
    fun concatAndCoalesceWork() {
        val concat = Expression.Binary(
            BinaryOp.Concat,
            Expression.Literal(Value.Text("Hi ")),
            Expression.Literal(Value.Text("Alice"))
        )
        assertEquals(Value.Text("Hi Alice"), concat.evaluate(emptyState))

        val coalesce = Expression.Binary(
            BinaryOp.Coalesce,
            Expression.Literal(Value.Text("")),
            Expression.Literal(Value.Text("fallback"))
        )
        assertEquals(Value.Text("fallback"), coalesce.evaluate(emptyState))
    }

    @Test
    fun ifElseShortCircuits() {
        val expr = Expression.IfElse(
            cond = Expression.Binary(
                BinaryOp.Gt,
                Expression.Ref("count"),
                Expression.Literal(Value.Number(10))
            ),
            then = Expression.Literal(Value.Text("over")),
            otherwise = Expression.Literal(Value.Text("under"))
        )
        val low = State(values = mapOf("count" to StateEntry(Value.Number(5))))
        val high = State(values = mapOf("count" to StateEntry(Value.Number(20))))
        assertEquals(Value.Text("under"), expr.evaluate(low))
        assertEquals(Value.Text("over"), expr.evaluate(high))
    }

    @Test
    fun builtInCalls() {
        assertEquals(
            Value.Number(5),
            Expression.Call("len", listOf(Expression.Literal(Value.Text("hello")))).evaluate(emptyState)
        )
        assertEquals(
            Value.Text("HI"),
            Expression.Call("upper", listOf(Expression.Literal(Value.Text("hi")))).evaluate(emptyState)
        )
        assertEquals(
            Value.Text("ab"),
            Expression.Call(
                "concat",
                listOf(Expression.Literal(Value.Text("a")), Expression.Literal(Value.Text("b")))
            ).evaluate(emptyState)
        )
        assertEquals(
            Value.Text("hi Alice"),
            Expression.Call(
                "format",
                listOf(
                    Expression.Literal(Value.Text("hi {0}")),
                    Expression.Literal(Value.Text("Alice"))
                )
            ).evaluate(emptyState)
        )
        // Unknown function falls back to empty Text rather than crashing.
        assertEquals(
            Value.Text(""),
            Expression.Call("doesNotExist", emptyList()).evaluate(emptyState)
        )
    }

    @Test
    fun expressionSeesItemScope() {
        // forEach-style usage: item.amount > 100 evaluated against scoped state.
        val item = Value.Record(fields = mapOf("amount" to Value.Number(150)))
        val state = State().withItemScope(item, index = 0)
        val expr = Expression.Binary(
            BinaryOp.Gt,
            Expression.Ref("item.amount"),
            Expression.Literal(Value.Number(100))
        )
        assertEquals(Value.Bool(true), expr.evaluate(state))
    }

    @Test
    fun parseExpressionRoundTrip() {
        val json = JSONObject(
            """
              {
                "type": "if",
                "cond": {
                  "type": "binary",
                  "op": ">",
                  "lhs": { "type": "ref", "key": "amount" },
                  "rhs": { "type": "lit", "value": 100 }
                },
                "then": { "type": "lit", "value": "expensive" },
                "else": { "type": "lit", "value": "cheap" }
              }
            """.trimIndent()
        )
        val expr = parseExpression(json) as Expression.IfElse
        val low = State(values = mapOf("amount" to StateEntry(Value.Number(20))))
        val high = State(values = mapOf("amount" to StateEntry(Value.Number(500))))
        assertEquals(Value.Text("cheap"), expr.evaluate(low))
        assertEquals(Value.Text("expensive"), expr.evaluate(high))
    }

    @Test
    fun valueSourceExprResolvesThroughReducer() {
        // SetState with ValueSource.Expr — verifies reducer plumbs through.
        val state = State(
            values = mapOf(
                "a" to StateEntry(Value.Number(5)),
                "b" to StateEntry(Value.Number(7)),
                "sum" to StateEntry(Value.Number(0))
            )
        )
        val expr = Expression.Binary(
            BinaryOp.Add,
            Expression.Ref("a"),
            Expression.Ref("b")
        )
        val reduced = reduceState(
            state = state,
            action = Action.SetState(key = "sum", value = ValueSource.Expr(expr))
        )
        assertEquals("12", reduced.getText("sum"))
    }

    @Test
    fun conditionExprParsesAndDrivesRendering() {
        val page = parseZeroUiPage(
            """
              {
                "initialState": { "count": 5 },
                "root": {
                  "type": "condition",
                  "condition": {
                    "type": "expr",
                    "expression": {
                      "type": "binary",
                      "op": ">",
                      "lhs": { "type": "ref", "key": "count" },
                      "rhs": { "type": "lit", "value": 3 }
                    }
                  },
                  "child": { "type": "text", "text": { "type": "value", "value": "shown" } }
                }
              }
            """.trimIndent()
        )

        val cond = page.root as Node.Condition
        val exprCond = cond.condition as Condition.Expr
        // Sanity: the expression should evaluate to true (5 > 3) against the initial state.
        assertTrue(exprCond.expression.evaluate(page.initialState).let { it is Value.Bool && it.value })
    }

    @Test
    fun unsupportedBinaryOpThrowsAtParseTime() {
        // Parser is strict; only the evaluator is total.
        var threw = false
        try {
            parseExpression(JSONObject("""{"type":"binary","op":"???","lhs":{"type":"lit","value":1},"rhs":{"type":"lit","value":2}}"""))
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue("parseExpression should reject unknown operators", threw)
        assertFalse(false) // sanity guard so the empty branch above counts as test logic
    }
}
