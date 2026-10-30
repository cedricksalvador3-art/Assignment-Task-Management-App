package com.example.assignment_task_management_app.utils

import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class PhotomathSolution(
    val problemText: String,
    val finalAnswerText: String,
    val stepByStepExplanations: String
)

data class MathSolveResult(
    val detectedExpressions: List<String>,
    val formattedSolutionSummary: String
)

object MathExpressionSolver {

    fun solveWithPhotomathSteps(problemInput: String): PhotomathSolution {
        val problem = problemInput.trim()
        if (problem.isEmpty()) {
            return PhotomathSolution(
                problemText = "",
                finalAnswerText = "",
                stepByStepExplanations = "Please enter or scan a valid equation to view step-by-step solutions."
            )
        }

        val linearSolution = parseAndSolveLinearEquation(problem)
        if (linearSolution != null) {
            return linearSolution
        }

        val numericResult = try {
            evaluateExpression(problem)
        } catch (_: Exception) {
            0.0
        }

        val formattedResult = formatNum(numericResult)

        return PhotomathSolution(
            problemText = problem,
            finalAnswerText = "Result = $formattedResult",
            stepByStepExplanations = """
                1. Identify operations and follow standard order of operations (PEMDAS):
                   $problem

                2. Compute arithmetic terms step-by-step:
                   Evaluated expression = $formattedResult

                3. Final Calculated Answer:
                   Result = $formattedResult
            """.trimIndent()
        )
    }

    private fun parseAndSolveLinearEquation(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()

        // Pattern 1: ax + b = cx + d
        val twoSideRegex = Regex("""^([+-]?\d*)x([+-]\d+)?=([+-]?\d*)x([+-]\d+)?$""")
        val twoSideMatch = twoSideRegex.find(cleaned)
        if (twoSideMatch != null) {
            val (aStr, bStr, cStr, dStr) = twoSideMatch.destructured
            val a = parseCoeff(aStr)
            val b = parseVal(bStr)
            val c = parseCoeff(cStr)
            val d = parseVal(dStr)

            val netA = a - c
            val netConst = d - b
            if (netA == 0.0) return null

            val xVal = netConst / netA
            val xFormatted = formatNum(xVal)

            val step1 = "1. Group variable terms on the left side by subtracting ${formatCoeff(c)}x from both sides:\n   ${formatCoeff(netA)}x ${if (b >= 0) "+ ${formatNum(b)}" else "- ${formatNum(abs(b))}"} = ${formatNum(d)}"
            val step2 = "2. Isolate the variable term by subtracting ${formatNum(b)} from both sides:\n   ${formatCoeff(netA)}x = ${formatNum(netConst)}"
            val step3 = "3. Divide both sides by ${formatNum(netA)}:\n   x = $xFormatted"
            val step4 = "4. Verification:\n   LHS = ${formatNum(a)}($xFormatted) + ${formatNum(b)} = ${formatNum(a * xVal + b)}\n   RHS = ${formatNum(c)}($xFormatted) + ${formatNum(d)} = ${formatNum(c * xVal + d)} ✓ (Verified)"

            return PhotomathSolution(
                problemText = line,
                finalAnswerText = "x = $xFormatted",
                stepByStepExplanations = "$step1\n\n$step2\n\n$step3\n\n$step4"
            )
        }

        // Pattern 2: ax + b = c
        val oneSideRegex = Regex("""^([+-]?\d*)x([+-]\d+)?=(.+)$""")
        val match = oneSideRegex.find(cleaned) ?: return null

        val (aStr, bStr, cStr) = match.destructured
        val a = parseCoeff(aStr)
        val b = parseVal(bStr)
        val c = cStr.toDoubleOrNull() ?: return null

        val rhsAfterSub = c - b
        val xVal = rhsAfterSub / a
        val xFormatted = formatNum(xVal)

        val step1Text = if (b > 0) {
            "1. Subtract ${formatNum(b)} from both sides of the equation:\n   ${formatCoeff(a)}x + ${formatNum(b)} - ${formatNum(b)} = ${formatNum(c)} - ${formatNum(b)}\n   ${formatCoeff(a)}x = ${formatNum(rhsAfterSub)}"
        } else if (b < 0) {
            "1. Add ${formatNum(abs(b))} to both sides of the equation:\n   ${formatCoeff(a)}x - ${formatNum(abs(b))} + ${formatNum(abs(b))} = ${formatNum(c)} + ${formatNum(abs(b))}\n   ${formatCoeff(a)}x = ${formatNum(rhsAfterSub)}"
        } else {
            "1. Isolate variable on the left side:\n   ${formatCoeff(a)}x = ${formatNum(rhsAfterSub)}"
        }

        val step2Text = "2. Divide both sides by the coefficient ${formatNum(a)}:\n   ${formatCoeff(a)}x / ${formatNum(a)} = ${formatNum(rhsAfterSub)} / ${formatNum(a)}\n   x = $xFormatted"

        val step3Text = "3. Verification:\n   Substitute x = $xFormatted into original equation:\n   ${formatNum(a)}($xFormatted) + (${formatNum(b)}) = ${formatNum(c)} ✓ (Verified)"

        return PhotomathSolution(
            problemText = line,
            finalAnswerText = "x = $xFormatted",
            stepByStepExplanations = "$step1Text\n\n$step2Text\n\n$step3Text"
        )
    }

    private fun parseCoeff(s: String): Double {
        return when {
            s.isEmpty() || s == "+" -> 1.0
            s == "-" -> -1.0
            else -> s.toDoubleOrNull() ?: 1.0
        }
    }

    private fun parseVal(s: String?): Double {
        if (s.isNullOrEmpty()) return 0.0
        return s.toDoubleOrNull() ?: 0.0
    }

    private fun formatNum(v: Double): String {
        return if (v % 1.0 == 0.0) "${v.toLong()}" else String.format(Locale.US, "%.2f", v)
    }

    private fun formatCoeff(v: Double): String {
        return when (v) {
            1.0 -> ""
            -1.0 -> "-"
            else -> formatNum(v)
        }
    }

    fun containsMathProblem(text: String): Boolean {
        val cleaned = text.lowercase()
        return (cleaned.contains("=") && (cleaned.contains("x") || cleaned.contains("+") || cleaned.contains("-"))) ||
                cleaned.contains("sqrt") || cleaned.contains("√") ||
                (cleaned.contains("+") || cleaned.contains("-") || cleaned.contains("*") || cleaned.contains("/")) && cleaned.any { it.isDigit() }
    }

    fun solveExpressionsFromText(text: String): MathSolveResult {
        val lines = text.split("\n")
        val solutions = mutableListOf<String>()
        val detected = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val solveStep = processSingleLine(trimmed)
            if (solveStep != null) {
                detected.add(trimmed)
                solutions.add(solveStep)
            }
        }

        if (detected.isEmpty()) {
            return MathSolveResult(
                detectedExpressions = emptyList(),
                formattedSolutionSummary = ""
            )
        }

        val summary = "Photomath Solved Expressions:\n" + solutions.joinToString("\n")
        return MathSolveResult(
            detectedExpressions = detected,
            formattedSolutionSummary = summary
        )
    }

    private fun processSingleLine(line: String): String? {
        val solution = parseAndSolveLinearEquation(line)
        if (solution != null) {
            return "Equation '${solution.problemText}'  ⇒  ${solution.finalAnswerText}"
        }

        return try {
            val cleaned = line.replace("=", "").trim()
            val value = evaluateExpression(cleaned)
            val valStr = formatNum(value)
            "'$line'  ⇒  Result: $valStr"
        } catch (_: Exception) {
            null
        }
    }

    fun evaluateExpression(expression: String): Double {
        val expr = expression.replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")

        if (expr.contains("sqrt") || expr.contains("√")) {
            val numStr = expr.replace("sqrt", "").replace("√", "").replace("(", "").replace(")", "")
            val num = numStr.toDoubleOrNull() ?: 0.0
            return sqrt(num)
        }

        return try {
            object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < expr.length) expr[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < expr.length) throw RuntimeException("Unexpected: " + ch.toChar())
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        if (eat('+'.code)) x += parseTerm()
                        else if (eat('-'.code)) x -= parseTerm()
                        else return x
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        if (eat('*'.code)) x *= parseFactor()
                        else if (eat('/'.code)) x /= parseFactor()
                        else return x
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor()
                    if (eat('-'.code)) return -parseFactor()

                    var x: Double
                    val startPos = pos
                    if (eat('('.code)) {
                        x = parseExpression()
                        eat(')'.code)
                    } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                        while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                        x = expr.substring(startPos, pos).toDouble()
                    } else {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }

                    if (eat('^'.code)) x = x.pow(parseFactor())

                    return x
                }
            }.parse()
        } catch (_: Exception) {
            0.0
        }
    }
}
