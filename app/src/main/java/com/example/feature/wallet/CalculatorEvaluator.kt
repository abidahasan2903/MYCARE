package com.example.feature.wallet

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Offline recursive-descent mathematical expression evaluator supporting:
 * +, -, *, /, %, parentheses (), unary minus/plus, decimals, and proper operator precedence.
 */
object CalculatorEvaluator {

    fun evaluate(rawExpression: String): Result<String> {
        val expr = rawExpression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")
        if (expr.isBlank()) return Result.success("0")

        return runCatching {
            val parser = Parser(expr)
            val result = parser.parseExpression()
            if (parser.pos < expr.length) {
                error("Unexpected character: ${expr[parser.pos]}")
            }
            if (result.isNaN() || result.isInfinite()) {
                error("Division by zero or overflow")
            }
            formatResult(result)
        }
    }

    private fun formatResult(value: Double): String {
        val bd = BigDecimal.valueOf(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        return bd.toPlainString()
    }

    private class Parser(private val s: String) {
        var pos = 0

        fun parseExpression(): Double {
            var x = parseTerm()
            while (pos < s.length) {
                when (s[pos]) {
                    '+' -> { pos++; x += parseTerm() }
                    '-' -> { pos++; x -= parseTerm() }
                    else -> return x
                }
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseUnary()
            while (pos < s.length) {
                when (s[pos]) {
                    '*' -> { pos++; x *= parseUnary() }
                    '/' -> {
                        pos++
                        val d = parseUnary()
                        if (d == 0.0) error("Division by zero")
                        x /= d
                    }
                    else -> return x
                }
            }
            return x
        }

        private fun parseUnary(): Double {
            if (pos < s.length && s[pos] == '+') {
                pos++
                return parseUnary()
            }
            if (pos < s.length && s[pos] == '-') {
                pos++
                return -parseUnary()
            }
            return parsePostfix()
        }

        private fun parsePostfix(): Double {
            var x = parsePrimary()
            while (pos < s.length && s[pos] == '%') {
                pos++
                x /= 100.0
            }
            return x
        }

        private fun parsePrimary(): Double {
            if (pos >= s.length) error("Unexpected end of expression")
            if (s[pos] == '(') {
                pos++
                val x = parseExpression()
                if (pos >= s.length || s[pos] != ')') error("Missing closing parenthesis")
                pos++
                return x
            }
            val start = pos
            var hasDot = false
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) {
                if (s[pos] == '.') {
                    if (hasDot) error("Multiple decimals")
                    hasDot = true
                }
                pos++
            }
            if (start == pos) error("Expected number at $pos")
            return s.substring(start, pos).toDouble()
        }
    }
}
