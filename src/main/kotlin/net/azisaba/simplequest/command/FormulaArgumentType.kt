package net.azisaba.simplequest.command

enum class FormulaOperator { ADD, SUB, MUL, DIV, SET }

data class Formula(
    val op: FormulaOperator,
    val value: Int,
) {
    fun apply(base: Int): Int =
        when (op) {
            FormulaOperator.ADD -> base + value
            FormulaOperator.SUB -> base - value
            FormulaOperator.MUL -> base * value
            FormulaOperator.DIV -> base / value
            FormulaOperator.SET -> value
        }

    companion object {
        fun parse(input: String): Formula {
            val trimmed = input.trim()
            return when {
                trimmed.startsWith("+") -> Formula(FormulaOperator.ADD, trimmed.substring(1).toIntOrNull() ?: 0)
                trimmed.startsWith("-") -> Formula(FormulaOperator.SUB, trimmed.substring(1).toIntOrNull() ?: 0)
                trimmed.startsWith("*") -> Formula(FormulaOperator.MUL, trimmed.substring(1).toIntOrNull() ?: 1)
                trimmed.startsWith("/") -> Formula(FormulaOperator.DIV, trimmed.substring(1).toIntOrNull() ?: 1)
                trimmed.startsWith("=") -> Formula(FormulaOperator.SET, trimmed.substring(1).toIntOrNull() ?: 0)
                else -> Formula(FormulaOperator.SET, trimmed.toIntOrNull() ?: 0)
            }
        }
    }
}
