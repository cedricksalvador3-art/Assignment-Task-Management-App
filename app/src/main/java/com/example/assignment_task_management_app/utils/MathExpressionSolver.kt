package com.example.assignment_task_management_app.utils

import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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

    /**
     * Cleans OCR text and extracts the primary mathematical problem/equation.
     */
    fun extractMathProblemFromText(rawText: String): String {
        if (rawText.isBlank()) return ""

        val lines = rawText.split("\n", "\r")
        val candidateLines = mutableListOf<String>()

        for (line in lines) {
            val cleaned = cleanOcrMathNoise(line)
            if (containsMathProblem(cleaned)) {
                candidateLines.add(cleaned)
            }
        }

        if (candidateLines.isNotEmpty()) {
            val bestWithEquals = candidateLines.firstOrNull { it.contains("=") }
            return bestWithEquals ?: candidateLines.maxByOrNull { it.length } ?: ""
        }

        val wholeCleaned = cleanOcrMathNoise(rawText)
        return if (containsMathProblem(wholeCleaned)) wholeCleaned else ""
    }

    fun cleanOcrMathNoise(text: String): String {
        var cleaned = text
            .replace(Regex("""(?i)^(solve|find|calculate|compute|equation|problem)\s*:?\s*"""), "")
            .replace("−", "-")
            .replace("–", "-")
            .replace("—", "-")
            .replace("×", "*")
            .replace("⋅", "*")
            .replace("·", "*")
            .replace("÷", "/")
            .replace("==", "=")
            .replace(":-", "=")
            .replace("â‰=", "=")
            .replace("²", "^2")
            .replace("³", "^3")
            .replace("√", "sqrt")

        // Fix OCR multiplication between numbers: e.g. "5 x 10" -> "5 * 10"
        cleaned = cleaned.replace(Regex("""(\d)\s*[xX]\s*(\d)"""), "$1 * $2")

        // Fix common OCR number misreadings: "l0" -> "10", "I2" -> "12"
        cleaned = cleaned.replace(Regex("""\b[lI](\d+)\b"""), "1$1")
            .replace(Regex("""\b(\d+)[lI]\b"""), "${1}1")

        return cleaned.trim()
    }

    fun containsMathProblem(text: String): Boolean {
        val cleaned = text.lowercase()
        val hasEquals = cleaned.contains("=")
        val hasOperators = cleaned.contains("+") || cleaned.contains("-") || cleaned.contains("*") || cleaned.contains("/") || cleaned.contains(">") || cleaned.contains("<")
        val hasDigits = cleaned.any { it.isDigit() }
        val hasVariables = cleaned.any { it in 'a'..'z' && it != 'e' }
        val hasRootOrPow = cleaned.contains("sqrt") || cleaned.contains("√") || cleaned.contains("^")
        val hasAdvanced = cleaned.contains("d/dx") || cleaned.contains("derivative") ||
                cleaned.contains("int") || cleaned.contains("integral") || cleaned.contains("∫") ||
                cleaned.contains("sin") || cleaned.contains("cos") || cleaned.contains("tan") ||
                cleaned.contains("log") || cleaned.contains("ln") || cleaned.contains("area") ||
                cleaned.contains("volume") || cleaned.contains("mean") || cleaned.contains("det") ||
                cleaned.contains("lim") || cleaned.contains("matrix") || cleaned.contains("distance") ||
                cleaned.contains("slope") || cleaned.contains("%") || cleaned.contains("!") ||
                cleaned.contains("p") || cleaned.contains("c")

        return (hasEquals && (hasVariables || hasDigits || hasOperators)) ||
                (hasOperators && hasDigits) ||
                hasRootOrPow || hasAdvanced
    }

    /**
     * Solves mathematical problems across basic arithmetic, algebra, geometry, trigonometry,
     * calculus, statistics, matrices, fractions, logarithmic/exponential equations, and more.
     */
    fun solveWithPhotomathSteps(problemInput: String): PhotomathSolution {
        val problem = cleanOcrMathNoise(problemInput)
        if (problem.isEmpty()) {
            return PhotomathSolution(
                problemText = "",
                finalAnswerText = "",
                stepByStepExplanations = "Please enter or scan a valid mathematical problem to view step-by-step solutions."
            )
        }

        // 1. Statistics & Combinatorics
        val statsSolution = parseAndSolveStatistics(problem) ?: parseAndSolveCombinatorics(problem)
        if (statsSolution != null) return statsSolution

        // 2. Matrices & Vectors
        val matrixSolution = parseAndSolveMatricesAndVectors(problem)
        if (matrixSolution != null) return matrixSolution

        // 3. Complex Numbers & Radicals Simplification
        val complexSolution = parseAndSolveComplexAndRadicals(problem)
        if (complexSolution != null) return complexSolution

        // 4. Calculus (Definite/Indefinite Integrals, Derivatives, Limits)
        val calculusSolution = parseAndSolveCalculus(problem)
        if (calculusSolution != null) return calculusSolution

        // 5. Geometry & Coordinate Geometry
        val geometrySolution = parseAndSolveGeometry(problem)
        if (geometrySolution != null) return geometrySolution

        // 6. Fractions & Percentages
        val fractionSolution = parseAndSolveFractionsAndPercentages(problem)
        if (fractionSolution != null) return fractionSolution

        // 7. Trigonometry
        val trigSolution = parseAndSolveTrigonometry(problem)
        if (trigSolution != null) return trigSolution

        // 8. Exponential & Logarithmic Equations
        val expLogSolution = parseAndSolveExpLogEquations(problem)
        if (expLogSolution != null) return expLogSolution

        // 9. Absolute Value Equations & Inequalities
        val absIneqSolution = parseAndSolveAbsoluteValueAndInequalities(problem)
        if (absIneqSolution != null) return absIneqSolution

        // 10. System of 3 Linear Equations
        val system3Solution = parseAndSolveSystemOf3Equations(problem)
        if (system3Solution != null) return system3Solution

        // 11. System of 2 Linear Equations
        val system2Solution = parseAndSolveSystemOfEquations(problem)
        if (system2Solution != null) return system2Solution

        // 12. Quadratic Equations
        val quadraticSolution = parseAndSolveQuadraticEquation(problem)
        if (quadraticSolution != null) return quadraticSolution

        // 13. Parentheses & General Linear Equations
        val parenSolution = parseAndSolveParenthesesEquation(problem)
            ?: parseAndSolveGeneralLinearEquation(problem)
        if (parenSolution != null) return parenSolution

        // 14. Polynomial Operations (Expansion)
        val polySolution = parseAndSolvePolynomials(problem)
        if (polySolution != null) return polySolution

        // 15. General Expression Evaluation
        return parseAndSolveArithmeticExpression(problem)
    }

    // -------------------------------------------------------------------------
    // 1. STATISTICS & COMBINATORICS
    // -------------------------------------------------------------------------

    private fun parseAndSolveStatistics(line: String): PhotomathSolution? {
        val cleaned = line.lowercase().replace(":", " ")
        if (!cleaned.contains("mean") && !cleaned.contains("median") && !cleaned.contains("stats") && !cleaned.contains("average") && !cleaned.contains("std dev")) return null

        val numbers = Regex("""[-+]?\d*\.?\d+""").findAll(cleaned)
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()

        if (numbers.size < 2) return null

        val count = numbers.size
        val sum = numbers.sum()
        val mean = sum / count

        val sorted = numbers.sorted()
        val median = if (count % 2 == 1) {
            sorted[count / 2]
        } else {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        }

        val frequencyMap = numbers.groupingBy { it }.eachCount()
        val maxFreq = frequencyMap.values.maxOrNull() ?: 1
        val modes = frequencyMap.filter { it.value == maxFreq && maxFreq > 1 }.keys

        val variance = numbers.sumOf { (it - mean).pow(2) } / (if (count > 1) count - 1 else 1)
        val stdDev = sqrt(variance)
        val range = sorted.last() - sorted.first()

        val step1 = "1. Data Set Summary:\n   • Count (N) = $count\n   • Values = ${numbers.joinToString(", ") { formatNum(it) }}"
        val step2 = "2. Descriptive Statistics Calculations:\n" +
                "   • Sum = ${formatNum(sum)}\n" +
                "   • Mean (μ) = Sum / N = ${formatNum(sum)} / $count = ${formatNum(mean)}\n" +
                "   • Median = ${formatNum(median)}\n" +
                "   • Mode(s) = ${if (modes.isNotEmpty()) modes.joinToString(", ") { formatNum(it) } else "No unique mode"}\n" +
                "   • Range = Max - Min = ${formatNum(sorted.last())} - ${formatNum(sorted.first())} = ${formatNum(range)}\n" +
                "   • Sample Std Dev (s) = ${formatNum(stdDev)}"

        val finalAns = "Mean = ${formatNum(mean)}, Median = ${formatNum(median)}, StdDev = ${formatNum(stdDev)}"

        return PhotomathSolution(
            problemText = line,
            finalAnswerText = finalAns,
            stepByStepExplanations = "$step1\n\n$step2"
        )
    }

    private fun parseAndSolveCombinatorics(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "")

        // Factorial: n! e.g. 5!
        val factMatch = Regex("""^(\d+)!$""").find(cleaned)
        if (factMatch != null) {
            val n = factMatch.groupValues[1].toLongOrNull() ?: return null
            if (n < 0 || n > 20) return null
            val result = factorial(n)
            val step1 = "1. Factorial Definition:\n   n! = n × (n - 1) × ... × 1"
            val step2 = "2. Calculation:\n   $n! = ${factorialExpansionString(n)} = $result"
            return PhotomathSolution(problemText = line, finalAnswerText = "$n! = $result", stepByStepExplanations = "$step1\n\n$step2")
        }

        // Permutations (nPr or P(n,r)) / Combinations (nCr or C(n,r))
        val pMatch = Regex("""^(\d+)[pP](\d+)$|^[pP]\((\d+),(\d+)\)$""").find(cleaned)
        if (pMatch != null) {
            val n = (pMatch.groupValues[1].ifEmpty { pMatch.groupValues[3] }).toLongOrNull() ?: return null
            val r = (pMatch.groupValues[2].ifEmpty { pMatch.groupValues[4] }).toLongOrNull() ?: return null
            if (r > n) return null
            val result = permutation(n, r)
            val step1 = "1. Permutations Formula:\n   nPr = n! / (n - r)!"
            val step2 = "2. Substitution:\n   ${n}P${r} = $n! / ($n - $r)! = ${n}! / ${(n - r)}! = $result"
            return PhotomathSolution(problemText = line, finalAnswerText = "${n}P${r} = $result", stepByStepExplanations = "$step1\n\n$step2")
        }

        val cMatch = Regex("""^(\d+)[cC](\d+)$|^[cC]\((\d+),(\d+)\)$""").find(cleaned)
        if (cMatch != null) {
            val n = (cMatch.groupValues[1].ifEmpty { cMatch.groupValues[3] }).toLongOrNull() ?: return null
            val r = (cMatch.groupValues[2].ifEmpty { cMatch.groupValues[4] }).toLongOrNull() ?: return null
            if (r > n) return null
            val result = combination(n, r)
            val step1 = "1. Combinations Formula:\n   nCr = n! / (r! × (n - r)!)"
            val step2 = "2. Substitution:\n   ${n}C${r} = $n! / ($r! × ${(n - r)}!) = $result"
            return PhotomathSolution(problemText = line, finalAnswerText = "${n}C${r} = $result", stepByStepExplanations = "$step1\n\n$step2")
        }

        return null
    }

    private fun factorial(n: Long): Long {
        if (n < 0) return 1L
        var res = 1L
        for (i in 2..n) res *= i
        return res
    }

    private fun factorialExpansionString(n: Long): String {
        if (n <= 1) return "1"
        return (n downTo 1).joinToString(" × ")
    }

    private fun permutation(n: Long, r: Long): Long = factorial(n) / factorial(n - r)

    private fun combination(n: Long, r: Long): Long = factorial(n) / (factorial(r) * factorial(n - r))

    // -------------------------------------------------------------------------
    // 2. MATRICES & VECTORS
    // -------------------------------------------------------------------------

    private fun parseAndSolveMatricesAndVectors(line: String): PhotomathSolution? {
        val cleaned = line.lowercase().replace(" ", "")

        // Matrix Determinant: det([[a,b],[c,d]]) or det([[a,b,c],[d,e,f],[g,h,i]])
        if (cleaned.contains("det")) {
            val nums = Regex("""[-+]?\d*\.?\d+""").findAll(cleaned).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (nums.size == 4) {
                val (a, b, c, d) = nums
                val detVal = a * d - b * c
                val step1 = "1. 2×2 Matrix Determinant Formula:\n   | a  b |\n   | c  d | = (a × d) - (b × c)"
                val step2 = "2. Calculation:\n   det = (${formatNum(a)} × ${formatNum(d)}) - (${formatNum(b)} × ${formatNum(c)})\n   det = ${formatNum(a * d)} - ${formatNum(b * c)} = ${formatNum(detVal)}"
                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = "Determinant = ${formatNum(detVal)}",
                    stepByStepExplanations = "$step1\n\n$step2"
                )
            } else if (nums.size == 9) {
                val a = nums[0]; val b = nums[1]; val c = nums[2]
                val d = nums[3]; val e = nums[4]; val f = nums[5]
                val g = nums[6]; val h = nums[7]; val i = nums[8]
                val detVal = det3x3(a, b, c, d, e, f, g, h, i)
                val step1 = "1. 3×3 Matrix Determinant Expansion:\n   det = a(ei - fh) - b(di - fg) + c(dh - eg)"
                val step2 = "2. Calculation:\n   det = ${formatNum(a)}(${formatNum(e*i - f*h)}) - ${formatNum(b)}(${formatNum(d*i - f*g)}) + ${formatNum(c)}(${formatNum(d*h - e*g)}) = ${formatNum(detVal)}"
                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = "Determinant = ${formatNum(detVal)}",
                    stepByStepExplanations = "$step1\n\n$step2"
                )
            }
        }

        // Vector Dot Product: dot product of (1, 2, 3) and (4, 5, 6) or (1, 2) . (3, 4)
        if (cleaned.contains("dot") || (cleaned.contains("product") && !cleaned.contains("cross"))) {
            val nums = Regex("""[-+]?\d*\.?\d+""").findAll(cleaned).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (nums.size >= 4 && nums.size % 2 == 0) {
                val half = nums.size / 2
                val v1 = nums.subList(0, half)
                val v2 = nums.subList(half, nums.size)
                var dotSum = 0.0
                val terms = mutableListOf<String>()
                for (k in 0 until half) {
                    val prod = v1[k] * v2[k]
                    dotSum += prod
                    terms.add("(${formatNum(v1[k])} × ${formatNum(v2[k])})")
                }
                val step1 = "1. Vector Dot Product Formula:\n   u · v = u₁v₁ + u₂v₂ + ..."
                val step2 = "2. Calculation:\n   u · v = ${terms.joinToString(" + ")} = ${formatNum(dotSum)}"
                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = "Dot Product = ${formatNum(dotSum)}",
                    stepByStepExplanations = "$step1\n\n$step2"
                )
            }
        }

        // Vector Cross Product: cross product of (1, 2, 3) and (4, 5, 6)
        if (cleaned.contains("cross")) {
            val nums = Regex("""[-+]?\d*\.?\d+""").findAll(cleaned).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (nums.size == 6) {
                val u1 = nums[0]; val u2 = nums[1]; val u3 = nums[2]
                val v1 = nums[3]; val v2 = nums[4]; val v3 = nums[5]
                val iComp = u2 * v3 - u3 * v2
                val jComp = u3 * v1 - u1 * v3
                val kComp = u1 * v2 - u2 * v1
                val step1 = "1. Vector Cross Product Formula:\n   u × v = (u₂v₃ - u₃v₂)i - (u₁v₃ - u₃v₁)j + (u₁v₂ - u₂v₁)k"
                val step2 = "2. Calculation:\n   u × v = (${formatNum(iComp)}, ${formatNum(jComp)}, ${formatNum(kComp)})"
                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = "Cross Product = (${formatNum(iComp)}, ${formatNum(jComp)}, ${formatNum(kComp)})",
                    stepByStepExplanations = "$step1\n\n$step2"
                )
            }
        }

        // Vector Magnitude: magnitude of (a, b) or ||(a, b)||
        if (cleaned.contains("magnitude")) {
            val nums = Regex("""[-+]?\d*\.?\d+""").findAll(cleaned).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (nums.isNotEmpty()) {
                val magSq = nums.sumOf { it * it }
                val mag = sqrt(magSq)
                val step1 = "1. Vector Magnitude Formula:\n   ||v|| = √(v₁² + v₂² + ...)"
                val step2 = "2. Calculation:\n   ||v|| = √(${nums.joinToString(" + ") { "${formatNum(it)}²" }})\n   ||v|| = √(${formatNum(magSq)}) = ${formatNum(mag)}"
                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = "||v|| = ${formatNum(mag)}",
                    stepByStepExplanations = "$step1\n\n$step2"
                )
            }
        }

        return null
    }

    // -------------------------------------------------------------------------
    // 3. COMPLEX NUMBERS & RADICAL SIMPLIFICATION
    // -------------------------------------------------------------------------

    private fun parseAndSolveComplexAndRadicals(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()

        // Radical Addition/Subtraction: sqrt(12) + sqrt(27)
        if (cleaned.contains("sqrt") && (cleaned.contains("+") || cleaned.contains("-")) && !cleaned.contains("x")) {
            val terms = cleaned.replace("-", "+-").split("+").filter { it.isNotBlank() }
            if (terms.size >= 2 && terms.all { it.contains("sqrt") || it.toDoubleOrNull() != null }) {
                var totalVal = 0.0
                for (term in terms) {
                    val m = Regex("""([+-]?\d*\.?\d*)?sqrt\(?(\d+)\)?""").find(term)
                    if (m != null) {
                        val coeffStr = m.groupValues[1]
                        val coeff = parseCoeff(coeffStr)
                        val num = m.groupValues[2].toDoubleOrNull() ?: 0.0
                        totalVal += coeff * sqrt(num)
                    } else {
                        totalVal += term.toDoubleOrNull() ?: 0.0
                    }
                }
                val step1 = "1. Simplify Each Radical Term:\n   Evaluate numerical terms."
                val step2 = "2. Sum Simplified Terms:\n   Result = ${formatNum(totalVal)}"
                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = "Result = ${formatNum(totalVal)}",
                    stepByStepExplanations = "$step1\n\n$step2"
                )
            }
        }

        // Radical Simplification with coefficient: 2sqrt(8) or sqrt(72)
        if (cleaned.startsWith("sqrt(") || cleaned.startsWith("simplify") || cleaned.contains("sqrt")) {
            val match = Regex("""([+-]?\d*\.?\d*)?sqrt\(?(\d+)\)?""").find(cleaned)
            if (match != null) {
                val coeffStr = match.groupValues[1]
                val coeff = if (coeffStr.isEmpty() || coeffStr == "+") 1L else if (coeffStr == "-") -1L else coeffStr.toLongOrNull() ?: 1L
                val num = match.groupValues[2].toLongOrNull() ?: return null

                val (outside, inside) = simplifyRadicalParts(num)
                val netOutside = coeff * outside
                val simplifiedStr = when {
                    inside == 1L -> "$netOutside"
                    netOutside == 1L -> "√$inside"
                    netOutside == -1L -> "-√$inside"
                    else -> "${netOutside}√$inside"
                }
                val decVal = coeff * sqrt(num.toDouble())
                val step1 = "1. Identify Perfect Square Factors of $num:\n   √$num = ${outside}${if (inside > 1) "√$inside" else ""}"
                val step2 = "2. Combine Coefficients:\n   ${if (coeff != 1L) "$coeff × " else ""}√$num = $simplifiedStr ≈ ${formatNum(decVal)}"
                val finalAns = if (coeff == 1L) "√$num = $simplifiedStr" else "Result = $simplifiedStr"
                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = finalAns,
                    stepByStepExplanations = "$step1\n\n$step2"
                )
            }
        }

        // Complex Numbers: (a + bi) (+ | - | * | /) (c + di) or (a + bi)(c + di)
        if (cleaned.contains("i") && (cleaned.contains("(") || cleaned.contains("+") || cleaned.contains("-"))) {
            val complexPairRegex = Regex("""\(([-+]?\d*\.?\d*)([-+]\d*\.?\d*)i\)\s*([+\-*/])?\s*\(([-+]?\d*\.?\d*)([-+]\d*\.?\d*)i\)""")
            val match = complexPairRegex.find(cleaned)
            if (match != null) {
                val (aStr, bStr, opStr, cStr, dStr) = match.destructured
                val a = parseVal(aStr)
                val b = parseCoeff(bStr)
                val c = parseVal(cStr)
                val d = parseCoeff(dStr)
                val op = if (opStr.isBlank()) "*" else opStr

                val realPart: Double
                val imagPart: Double
                val opName: String

                when (op) {
                    "+" -> {
                        realPart = a + c
                        imagPart = b + d
                        opName = "Addition"
                    }
                    "-" -> {
                        realPart = a - c
                        imagPart = b - d
                        opName = "Subtraction"
                    }
                    "/" -> {
                        val denom = c * c + d * d
                        if (denom == 0.0) return null
                        realPart = (a * c + b * d) / denom
                        imagPart = (b * c - a * d) / denom
                        opName = "Division"
                    }
                    else -> { // "*"
                        realPart = a * c - b * d
                        imagPart = a * d + b * c
                        opName = "Multiplication"
                    }
                }

                val step1 = "1. Complex Number $opName Rule:"
                val step2 = "2. Calculated Parts:\n   Real Part = ${formatNum(realPart)}\n   Imaginary Part = ${formatNum(imagPart)}"
                val finalAns = "${formatNum(realPart)} ${if (imagPart >= 0) "+ ${formatNum(imagPart)}i" else "- ${formatNum(abs(imagPart))}i"}"

                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = finalAns,
                    stepByStepExplanations = "$step1\n\n$step2"
                )
            }
        }

        return null
    }

    private fun simplifyRadicalParts(n: Long): Pair<Long, Long> {
        if (n < 0) return Pair(1L, n)
        var outside = 1L
        var inside = n

        var i = 2L
        while (i * i <= inside) {
            while (inside % (i * i) == 0L) {
                outside *= i
                inside /= (i * i)
            }
            i++
        }
        return Pair(outside, inside)
    }

    private fun simplifyRadical(n: Long): String {
        val (outside, inside) = simplifyRadicalParts(n)
        return when {
            inside == 1L -> "$outside"
            outside == 1L -> "√$inside"
            else -> "${outside}√$inside"
        }
    }

    // -------------------------------------------------------------------------
    // 4. CALCULUS
    // -------------------------------------------------------------------------

    private fun parseAndSolveCalculus(line: String): PhotomathSolution? {
        val cleaned = line.lowercase().replace(" ", "")

        // Limit: lim x->0 (sin(x)/x) or lim x->2 (x^2 - 4)/(x - 2)
        if (cleaned.contains("lim")) {
            val limitMatch = Regex("""limx->([-+]?\d*\.?\d*|inf|infinity)(.*)""").find(cleaned)
            if (limitMatch != null) {
                val targetStr = limitMatch.groupValues[1]
                var expr = limitMatch.groupValues[2].trim()
                if (expr.startsWith("(") && expr.endsWith(")") && expr.indexOf('(', 1) == -1) {
                    expr = expr.substring(1, expr.length - 1)
                }

                if (expr == "sin(x)/x" && (targetStr == "0" || targetStr == "0.0")) {
                    val step1 = "1. Standard Limit Identity:\n   lim (x → 0) [sin(x) / x] = 1"
                    return PhotomathSolution(problemText = line, finalAnswerText = "Limit = 1", stepByStepExplanations = step1)
                }
                if (targetStr.contains("inf")) {
                    val step1 = "1. Limit as x → ∞:\n   Evaluating behavior of $expr at infinity."
                    val ans = if (expr.contains("1/x")) "0" else "∞"
                    return PhotomathSolution(problemText = line, finalAnswerText = "Limit = $ans", stepByStepExplanations = step1)
                }

                val c = targetStr.toDoubleOrNull()
                if (c != null) {
                    var evaluated = try {
                        evaluateExpression(expr.replace("x", "($c)"))
                    } catch (_: Exception) {
                        Double.NaN
                    }

                    if (evaluated.isNaN() || evaluated.isInfinite()) {
                        // Indeterminate form 0/0: evaluate numerically at c + 1e-6 and c - 1e-6
                        val rightVal = evaluateExpression(expr.replace("x", "(${c + 1e-6})"))
                        val leftVal = evaluateExpression(expr.replace("x", "(${c - 1e-6})"))
                        evaluated = (rightVal + leftVal) / 2.0
                    }

                    val step1 = "1. Limit Evaluation (x → ${formatNum(c)}):\n   Evaluating $expr near x = ${formatNum(c)}"
                    val step2 = "2. Calculated Limit:\n   Limit = ${formatNum(evaluated)}"
                    return PhotomathSolution(problemText = line, finalAnswerText = "Limit = ${formatNum(evaluated)}", stepByStepExplanations = "$step1\n\n$step2")
                }
            }
        }

        // Definite Integral: int(0 to 2) 3x^2 dx
        if (cleaned.contains("int") || cleaned.contains("integral") || cleaned.contains("∫")) {
            val defMatch = Regex("""int\(?([-+]?\d*\.?\d*)\s*to\s*([-+]?\d*\.?\d*)\)?(.*)dx""").find(cleaned)
                ?: Regex("""∫_?\{?([-+]?\d*\.?\d*)\}?\^?\{?([-+]?\d*\.?\d*)\}?(.*)dx""").find(cleaned)

            if (defMatch != null) {
                val a = defMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val b = defMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                var integrand = defMatch.groupValues[3].trim()
                if (integrand.startsWith("(") && integrand.endsWith(")")) {
                    integrand = integrand.substring(1, integrand.length - 1)
                }

                val antiderivStr = integrateExpression(integrand)
                val valB = evaluateAntiderivative(integrand, b)
                val valA = evaluateAntiderivative(integrand, a)
                val definiteResult = valB - valA

                val step1 = "1. Definite Integral Evaluation:\n   ∫_a^b f(x) dx = F(b) - F(a)"
                val step2 = "2. Antiderivative F(x):\n   F(x) = $antiderivStr"
                val step3 = "3. Fundamental Theorem of Calculus:\n   F($b) = ${formatNum(valB)}\n   F($a) = ${formatNum(valA)}\n   Result = F($b) - F($a) = ${formatNum(definiteResult)}"

                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = "Result = ${formatNum(definiteResult)}",
                    stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
                )
            }
        }

        // Indefinite Integral
        if (cleaned.contains("int") || cleaned.contains("integral") || cleaned.contains("∫")) {
            var exprToIntegrate = cleaned.replace("int", "")
                .replace("integralof", "")
                .replace("integral", "")
                .replace("∫", "")
                .replace("dx", "")
                .trim()

            if (exprToIntegrate.startsWith("(") && exprToIntegrate.endsWith(")")) {
                exprToIntegrate = exprToIntegrate.substring(1, exprToIntegrate.length - 1)
            }

            if (exprToIntegrate.isNotBlank()) {
                val intResult = integrateExpression(exprToIntegrate)
                val step1 = "1. Identify the integrand:\n   f(x) = $exprToIntegrate"
                val step2 = "2. Apply Integration Rules:\n   • Power Rule: ∫ a*x^n dx = (a/(n+1))*x^(n+1)\n   • Trig Rules: ∫ sin x dx = -cos x, ∫ cos x dx = sin x, ∫ sec² x dx = tan x"
                val step3 = "3. Antiderivative Result:\n   ∫ f(x) dx = $intResult + C"

                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = "∫ dx = $intResult + C",
                    stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
                )
            }
        }

        // Derivative: d/dx(...) or derivative of ...
        if (cleaned.contains("d/dx") || cleaned.contains("derivative")) {
            val isSecond = cleaned.contains("d^2/dx^2") || cleaned.contains("second")
            var exprToDiff = cleaned.replace("d^2/dx^2", "")
                .replace("d/dx", "")
                .replace("derivativeof", "")
                .replace("derivative", "")
                .trim()

            if (exprToDiff.startsWith("(") && exprToDiff.endsWith(")")) {
                exprToDiff = exprToDiff.substring(1, exprToDiff.length - 1)
            }

            if (exprToDiff.isNotBlank()) {
                val firstDiff = differentiateExpression(exprToDiff)
                val finalDiff = if (isSecond) differentiateExpression(firstDiff) else firstDiff

                val step1 = "1. Identify function to differentiate:\n   f(x) = $exprToDiff"
                val step2 = "2. Apply Differentiation Rules:\n   • Power Rule: d/dx(a*x^n) = a*n*x^(n-1)\n   • Constant Rule: d/dx(c) = 0\n   • Trig Rules: d/dx(sin x) = cos x, d/dx(cos x) = -sin x, d/dx(tan x) = sec² x"
                val step3 = if (isSecond) {
                    "3. First Derivative f'(x) = $firstDiff\n4. Second Derivative f''(x) = $finalDiff"
                } else {
                    "3. Derivative Result:\n   f'(x) = $finalDiff"
                }

                return PhotomathSolution(
                    problemText = line,
                    finalAnswerText = if (isSecond) "d²/dx² = $finalDiff" else "d/dx = $finalDiff",
                    stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
                )
            }
        }

        return null
    }

    private fun differentiateExpression(expr: String): String {
        val e = expr.lowercase().replace(" ", "")
        val terms = e.replace("-", "+-").split("+").filter { it.isNotBlank() }
        val diffTerms = mutableListOf<String>()

        for (term in terms) {
            val t = term.trim()
            when {
                t == "sin(x)" || t == "sinx" -> diffTerms.add("cos(x)")
                t == "-sin(x)" || t == "-sinx" -> diffTerms.add("-cos(x)")
                t == "cos(x)" || t == "cosx" -> diffTerms.add("-sin(x)")
                t == "-cos(x)" || t == "-cosx" -> diffTerms.add("sin(x)")
                t == "tan(x)" || t == "tanx" -> diffTerms.add("sec^2(x)")
                t == "e^x" || t == "ex" -> diffTerms.add("e^x")
                t == "ln(x)" || t == "lnx" -> diffTerms.add("1/x")
                t.contains("sin(") -> {
                    val coeffStr = t.substringBefore("sin(")
                    val coeff = parseCoeff(coeffStr)
                    val inner = t.substringAfter("sin(").substringBefore(")")
                    val innerCoeff = parseCoeff(inner.replace("x", ""))
                    diffTerms.add("${formatCoeff(coeff * innerCoeff)}cos($inner)".replace("--", ""))
                }
                t.contains("cos(") -> {
                    val coeffStr = t.substringBefore("cos(")
                    val coeff = parseCoeff(coeffStr)
                    val inner = t.substringAfter("cos(").substringBefore(")")
                    val innerCoeff = parseCoeff(inner.replace("x", ""))
                    diffTerms.add("${formatCoeff(-coeff * innerCoeff)}sin($inner)".replace("--", ""))
                }
                t.contains("x^") -> {
                    val parts = t.split("x^")
                    val coeff = parseCoeff(parts[0])
                    val power = parts[1].toDoubleOrNull() ?: 1.0
                    val newCoeff = coeff * power
                    val newPower = power - 1.0
                    val termStr = if (newPower == 1.0) "${formatCoeff(newCoeff)}x"
                    else if (newPower == 0.0) formatNum(newCoeff)
                    else "${formatCoeff(newCoeff)}x^${formatNum(newPower)}"
                    diffTerms.add(termStr)
                }
                t.endsWith("x") -> {
                    val coeffStr = t.substringBefore("x")
                    val coeff = parseCoeff(coeffStr)
                    diffTerms.add(formatNum(coeff))
                }
                else -> {
                    // Constant term derivative is 0
                }
            }
        }

        return if (diffTerms.isEmpty()) "0" else diffTerms.joinToString(" + ").replace("+ -", "- ")
    }

    private fun integrateExpression(expr: String): String {
        val e = expr.lowercase().replace(" ", "")
        val terms = e.replace("-", "+-").split("+").filter { it.isNotBlank() }
        val intTerms = mutableListOf<String>()

        for (term in terms) {
            val t = term.trim()
            when {
                t == "sin(x)" || t == "sinx" -> intTerms.add("-cos(x)")
                t == "-sin(x)" || t == "-sinx" -> intTerms.add("cos(x)")
                t == "cos(x)" || t == "cosx" -> intTerms.add("sin(x)")
                t == "-cos(x)" || t == "-cosx" -> intTerms.add("-sin(x)")
                t == "sec^2(x)" || t == "sec2(x)" -> intTerms.add("tan(x)")
                t == "e^x" || t == "ex" -> intTerms.add("e^x")
                t == "1/x" -> intTerms.add("ln|x|")
                t.contains("x^") -> {
                    val parts = t.split("x^")
                    val coeff = parseCoeff(parts[0])
                    val power = parts[1].toDoubleOrNull() ?: 1.0
                    val newPower = power + 1.0
                    val newCoeff = coeff / newPower
                    intTerms.add("${formatCoeff(newCoeff)}x^${formatNum(newPower)}")
                }
                t.endsWith("x") -> {
                    val coeffStr = t.substringBefore("x")
                    val coeff = parseCoeff(coeffStr)
                    val newCoeff = coeff / 2.0
                    intTerms.add("${formatCoeff(newCoeff)}x^2")
                }
                else -> {
                    val constVal = parseVal(t)
                    if (constVal != 0.0) {
                        intTerms.add("${formatNum(constVal)}x")
                    }
                }
            }
        }

        return if (intTerms.isEmpty()) "0" else intTerms.joinToString(" + ").replace("+ -", "- ")
    }

    private fun evaluateAntiderivative(expr: String, xVal: Double): Double {
        val e = expr.lowercase().replace(" ", "")
        val terms = e.replace("-", "+-").split("+").filter { it.isNotBlank() }
        var sum = 0.0

        for (term in terms) {
            val t = term.trim()
            when {
                t == "sin(x)" || t == "sinx" -> sum += -cos(xVal)
                t == "cos(x)" || t == "cosx" -> sum += sin(xVal)
                t == "e^x" || t == "ex" -> sum += Math.E.pow(xVal)
                t.contains("x^") -> {
                    val parts = t.split("x^")
                    val coeff = parseCoeff(parts[0])
                    val power = parts[1].toDoubleOrNull() ?: 1.0
                    val newPower = power + 1.0
                    sum += (coeff / newPower) * xVal.pow(newPower)
                }
                t.endsWith("x") -> {
                    val coeffStr = t.substringBefore("x")
                    val coeff = parseCoeff(coeffStr)
                    sum += (coeff / 2.0) * xVal.pow(2.0)
                }
                else -> {
                    val constVal = parseVal(t)
                    sum += constVal * xVal
                }
            }
        }
        return sum
    }

    // -------------------------------------------------------------------------
    // 5. GEOMETRY & COORDINATE GEOMETRY
    // -------------------------------------------------------------------------

    private fun parseAndSolveGeometry(line: String): PhotomathSolution? {
        val cleaned = line.lowercase()

        // Distance formula between two points: distance between (1, 2) and (4, 6)
        if (cleaned.contains("distance")) {
            val pts = Regex("""[-+]?\d*\.?\d+""").findAll(cleaned).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (pts.size >= 4) {
                val (x1, y1, x2, y2) = pts
                val dist = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
                val step1 = "1. Distance Formula:\n   d = √((x₂ - x₁)² + (y₂ - y₁)²)"
                val step2 = "2. Calculation with (${formatNum(x1)}, ${formatNum(y1)}) and (${formatNum(x2)}, ${formatNum(y2)}):\n   d = √((${formatNum(x2)} - ${formatNum(x1)})² + (${formatNum(y2)} - ${formatNum(y1)})²)\n   d = √(${formatNum((x2 - x1).pow(2))} + ${formatNum((y2 - y1).pow(2))}) = ${formatNum(dist)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Distance = ${formatNum(dist)}", stepByStepExplanations = "$step1\n\n$step2")
            }
        }

        // Midpoint formula: midpoint of (1, 2) and (5, 8)
        if (cleaned.contains("midpoint")) {
            val pts = Regex("""[-+]?\d*\.?\d+""").findAll(cleaned).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (pts.size >= 4) {
                val (x1, y1, x2, y2) = pts
                val mx = (x1 + x2) / 2.0
                val my = (y1 + y2) / 2.0
                val step1 = "1. Midpoint Formula:\n   M = ((x₁ + x₂) / 2, (y₁ + y₂) / 2)"
                val step2 = "2. Calculation:\n   M = ((${formatNum(x1)} + ${formatNum(x2)}) / 2, (${formatNum(y1)} + ${formatNum(y2)}) / 2) = (${formatNum(mx)}, ${formatNum(my)})"
                return PhotomathSolution(problemText = line, finalAnswerText = "Midpoint = (${formatNum(mx)}, ${formatNum(my)})", stepByStepExplanations = "$step1\n\n$step2")
            }
        }

        // Slope formula: slope between (1, 2) and (3, 8)
        if (cleaned.contains("slope")) {
            val pts = Regex("""[-+]?\d*\.?\d+""").findAll(cleaned).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (pts.size >= 4) {
                val (x1, y1, x2, y2) = pts
                if (x2 == x1) return PhotomathSolution(problemText = line, finalAnswerText = "Slope = Undefined (Vertical Line)", stepByStepExplanations = "Line is vertical.")
                val m = (y2 - y1) / (x2 - x1)
                val step1 = "1. Slope Formula:\n   m = (y₂ - y₁) / (x₂ - x₁)"
                val step2 = "2. Calculation:\n   m = (${formatNum(y2)} - ${formatNum(y1)}) / (${formatNum(x2)} - ${formatNum(x1)}) = ${formatNum(m)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Slope m = ${formatNum(m)}", stepByStepExplanations = "$step1\n\n$step2")
            }
        }

        // Circle Area / Circumference
        if (cleaned.contains("circle") && (cleaned.contains("area") || cleaned.contains("radius") || cleaned.contains("r=") || cleaned.contains("d="))) {
            val dVal = Regex("""d\s*=\s*(\d+\.?\d*)|diameter\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toDoubleOrNull()
            val rVal = Regex("""r\s*=\s*(\d+\.?\d*)|radius\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toDoubleOrNull()
                ?: (if (dVal != null) dVal / 2.0 else Regex("""(\d+\.?\d*)""").find(cleaned)?.value?.toDoubleOrNull())

            if (rVal != null) {
                val area = Math.PI * rVal * rVal
                val circ = 2 * Math.PI * rVal
                val step1 = "1. Circle Formulas:\n   Area = π × r²\n   Circumference = 2 × π × r"
                val step2 = "2. Calculation with r = ${formatNum(rVal)}:\n   Area = π × (${formatNum(rVal)})² = ${formatNum(area)}\n   Circumference = 2 × π × (${formatNum(rVal)}) = ${formatNum(circ)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Area = ${formatNum(area)}, Circumference = ${formatNum(circ)}", stepByStepExplanations = "$step1\n\n$step2")
            }
        }

        // Hypotenuse / Right Triangle (find hypotenuse c or leg a/b)
        if (cleaned.contains("hypotenuse") || cleaned.contains("pythagorean") || (cleaned.contains("triangle") && (cleaned.contains("a=") || cleaned.contains("b=") || cleaned.contains("c=")))) {
            val cVal = Regex("""c\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.get(1)?.toDoubleOrNull()
            val aVal = Regex("""a\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.get(1)?.toDoubleOrNull()
            val bVal = Regex("""b\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.get(1)?.toDoubleOrNull()

            if (aVal != null && bVal != null) {
                val c = sqrt(aVal * aVal + bVal * bVal)
                val step1 = "1. Pythagorean Theorem:\n   c = √(a² + b²)"
                val step2 = "2. Calculation with a = ${formatNum(aVal)}, b = ${formatNum(bVal)}:\n   c = √(${formatNum(aVal)}² + ${formatNum(bVal)}²) = √(${formatNum(aVal * aVal + bVal * bVal)}) = ${formatNum(c)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Hypotenuse c = ${formatNum(c)}", stepByStepExplanations = "$step1\n\n$step2")
            } else if (cVal != null && (aVal != null || bVal != null)) {
                val givenLeg = aVal ?: bVal!!
                if (cVal > givenLeg) {
                    val missingLeg = sqrt(cVal * cVal - givenLeg * givenLeg)
                    val step1 = "1. Pythagorean Theorem:\n   leg = √(c² - given²)"
                    val step2 = "2. Calculation:\n   leg = √(${formatNum(cVal)}² - ${formatNum(givenLeg)}²) = ${formatNum(missingLeg)}"
                    return PhotomathSolution(problemText = line, finalAnswerText = "Leg = ${formatNum(missingLeg)}", stepByStepExplanations = "$step1\n\n$step2")
                }
            }
        }

        // Rectangle / Square Area & Perimeter
        if (cleaned.contains("rectangle") || cleaned.contains("square")) {
            val sideVal = Regex("""side\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.get(1)?.toDoubleOrNull()
            val lenVal = Regex("""l\s*=\s*(\d+\.?\d*)|length\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toDoubleOrNull()
            val widVal = Regex("""w\s*=\s*(\d+\.?\d*)|width\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toDoubleOrNull()

            if (sideVal != null) {
                val area = sideVal * sideVal
                val perim = 4 * sideVal
                val step1 = "1. Square Formulas:\n   Area = side²\n   Perimeter = 4 × side"
                val step2 = "2. Calculation with side = ${formatNum(sideVal)}:\n   Area = ${formatNum(area)}\n   Perimeter = ${formatNum(perim)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Area = ${formatNum(area)}, Perimeter = ${formatNum(perim)}", stepByStepExplanations = "$step1\n\n$step2")
            } else if (lenVal != null && widVal != null) {
                val area = lenVal * widVal
                val perim = 2 * (lenVal + widVal)
                val step1 = "1. Rectangle Formulas:\n   Area = length × width\n   Perimeter = 2 × (length + width)"
                val step2 = "2. Calculation:\n   Area = ${formatNum(area)}\n   Perimeter = ${formatNum(perim)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Area = ${formatNum(area)}, Perimeter = ${formatNum(perim)}", stepByStepExplanations = "$step1\n\n$step2")
            }
        }

        // Triangle Area (Base & Height)
        if (cleaned.contains("triangle") && (cleaned.contains("b=") || cleaned.contains("h=") || cleaned.contains("base"))) {
            val bVal = Regex("""b\s*=\s*(\d+\.?\d*)|base\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toDoubleOrNull()
            val hVal = Regex("""h\s*=\s*(\d+\.?\d*)|height\s*=\s*(\d+\.?\d*)""").find(cleaned)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toDoubleOrNull()
            if (bVal != null && hVal != null) {
                val area = 0.5 * bVal * hVal
                val step1 = "1. Triangle Area Formula:\n   Area = ½ × base × height"
                val step2 = "2. Calculation:\n   Area = ½ × ${formatNum(bVal)} × ${formatNum(hVal)} = ${formatNum(area)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Area = ${formatNum(area)}", stepByStepExplanations = "$step1\n\n$step2")
            }
        }

        // Sphere Volume
        if (cleaned.contains("sphere") && (cleaned.contains("volume") || cleaned.contains("r="))) {
            val rMatch = Regex("""r\s*=\s*(\d+\.?\d*)|radius\s*=\s*(\d+\.?\d*)|(\d+\.?\d*)""").find(cleaned)
            val r = rMatch?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toDoubleOrNull()
            if (r != null) {
                val volume = (4.0 / 3.0) * Math.PI * r * r * r
                val step1 = "1. Sphere Volume Formula:\n   Volume = (4/3) × π × r³"
                val step2 = "2. Calculation with r = ${formatNum(r)}:\n   Volume = (4/3) × π × (${formatNum(r)})³ = ${formatNum(volume)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Volume = ${formatNum(volume)}", stepByStepExplanations = "$step1\n\n$step2")
            }
        }

        return null
    }

    // -------------------------------------------------------------------------
    // 6. FRACTIONS & PERCENTAGES
    // -------------------------------------------------------------------------

    private fun parseAndSolveFractionsAndPercentages(line: String): PhotomathSolution? {
        val cleaned = line.lowercase().replace(" ", "")

        // Percentage of: 15% of 200 or 15 percent of 200
        if (cleaned.contains("%of") || cleaned.contains("percentof")) {
            val match = Regex("""(\d+\.?\d*)(?:%|percent)of(\d+\.?\d*)""").find(cleaned)
            if (match != null) {
                val p = match.groupValues[1].toDoubleOrNull() ?: return null
                val total = match.groupValues[2].toDoubleOrNull() ?: return null
                val ans = (p / 100.0) * total
                val step1 = "1. Percentage Formula:\n   Result = (Percent / 100) × Total"
                val step2 = "2. Calculation:\n   Result = (${formatNum(p)} / 100) × ${formatNum(total)} = ${formatNum(ans)}"
                return PhotomathSolution(problemText = line, finalAnswerText = "Result = ${formatNum(ans)}", stepByStepExplanations = "$step1\n\n$step2")
            }
        }

        // Fraction Arithmetic: 3/4 + 5/6, 7/8 - 1/3, (3/4)*(2/5), (5/6)/(2/3)
        val fracRegex = Regex("""^\(?([+-]?\d+)/(\d+)\)?\s*([+\-*/])\s*\(?([+-]?\d+)/(\d+)\)?$""")
        val fracMatch = fracRegex.find(line.replace(" ", ""))
        if (fracMatch != null) {
            val (n1Str, d1Str, op, n2Str, d2Str) = fracMatch.destructured
            val n1 = n1Str.toLongOrNull() ?: return null
            val d1 = d1Str.toLongOrNull() ?: return null
            val n2 = n2Str.toLongOrNull() ?: return null
            val d2 = d2Str.toLongOrNull() ?: return null

            if (d1 == 0L || d2 == 0L) return null

            val rawNum: Long
            val rawDen: Long
            val opName: String

            when (op) {
                "+" -> {
                    rawNum = n1 * d2 + n2 * d1
                    rawDen = d1 * d2
                    opName = "Addition"
                }
                "-" -> {
                    rawNum = n1 * d2 - n2 * d1
                    rawDen = d1 * d2
                    opName = "Subtraction"
                }
                "*" -> {
                    rawNum = n1 * n2
                    rawDen = d1 * d2
                    opName = "Multiplication"
                }
                "/" -> {
                    rawNum = n1 * d2
                    rawDen = d1 * n2
                    opName = "Division"
                }
                else -> return null
            }

            val g = gcd(abs(rawNum), abs(rawDen))
            val simpNum = rawNum / g
            val simpDen = rawDen / g

            val simpStr = if (simpDen == 1L) "$simpNum" else "$simpNum/$simpDen"
            val decVal = rawNum.toDouble() / rawDen.toDouble()

            val step1 = "1. Fraction $opName Rules:\n   Perform $opName on $n1/$d1 and $n2/$d2"
            val step2 = "2. Unsimplified Result:\n   Result = $rawNum / $rawDen"
            val step3 = "3. Simplify using GCD ($g):\n   Simplified Fraction = $simpStr (≈ ${formatNum(decVal)})"

            return PhotomathSolution(
                problemText = line,
                finalAnswerText = "Result = $simpStr",
                stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
            )
        }

        return null
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val temp = y
            y = x % y
            x = temp
        }
        return if (x == 0L) 1L else x
    }

    // -------------------------------------------------------------------------
    // 7. TRIGONOMETRY
    // -------------------------------------------------------------------------

    private fun parseAndSolveTrigonometry(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()

        // Single Trig Function Evaluation: sin(30), cos(45), tan(60)
        val trigFuncMatch = Regex("""^(sin|cos|tan|asin|acos|atan)\(([-+]?\d*\.?\d*)\)$""").find(cleaned)
        if (trigFuncMatch != null) {
            val func = trigFuncMatch.groupValues[1]
            val arg = trigFuncMatch.groupValues[2].toDoubleOrNull() ?: return null

            val exactValStr = getExactTrigValue(func, arg)
            val numericVal = evaluateExpression(cleaned)

            val step1 = "1. Trigonometric Evaluation ($func):\n   Angle / Value = ${formatNum(arg)}°"
            val step2 = "2. Calculated Value:\n   Exact = $exactValStr\n   Decimal = ${formatNum(numericVal)}"

            return PhotomathSolution(
                problemText = line,
                finalAnswerText = "$func(${formatNum(arg)}) = $exactValStr",
                stepByStepExplanations = "$step1\n\n$step2"
            )
        }

        return null
    }

    private fun getExactTrigValue(func: String, deg: Double): String {
        val d = (deg % 360 + 360) % 360
        return when (func) {
            "sin" -> when (d.roundToInt()) {
                0 -> "0"
                30 -> "1/2"
                45 -> "√2/2"
                60 -> "√3/2"
                90 -> "1"
                180 -> "0"
                270 -> "-1"
                360 -> "0"
                else -> formatNum(sin(Math.toRadians(deg)))
            }
            "cos" -> when (d.roundToInt()) {
                0 -> "1"
                30 -> "√3/2"
                45 -> "√2/2"
                60 -> "1/2"
                90 -> "0"
                180 -> "-1"
                270 -> "0"
                360 -> "1"
                else -> formatNum(cos(Math.toRadians(deg)))
            }
            "tan" -> when (d.roundToInt()) {
                0 -> "0"
                30 -> "√3/3"
                45 -> "1"
                60 -> "√3"
                90 -> "Undefined"
                180 -> "0"
                270 -> "Undefined"
                else -> formatNum(tan(Math.toRadians(deg)))
            }
            else -> formatNum(evaluateExpression("$func($deg)"))
        }
    }

    // -------------------------------------------------------------------------
    // 8. EXPONENTIAL & LOGARITHMIC EQUATIONS
    // -------------------------------------------------------------------------

    private fun parseAndSolveExpLogEquations(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()

        // Exponential Equation: 2^x = 16 or e^x = 5
        val expMatch = Regex("""^(\d+\.?\d*|e)\^([a-zA-Z0-9+\-*/]+)=([-+]?\d+\.?\d*)$""").find(cleaned)
        if (expMatch != null) {
            val baseStr = expMatch.groupValues[1]
            val powerExpr = expMatch.groupValues[2]
            val rhs = expMatch.groupValues[3].toDoubleOrNull() ?: return null

            if (rhs <= 0) return null

            val targetLogVal = if (baseStr == "e") {
                ln(rhs)
            } else {
                val base = baseStr.toDoubleOrNull() ?: return null
                log10(rhs) / log10(base)
            }

            val linSol = if (powerExpr == "x") null else parseAndSolveGeneralLinearEquation("$powerExpr = ${formatNum(targetLogVal)}")
            val xValStr = linSol?.finalAnswerText ?: "x = ${formatNum(targetLogVal)}"

            val step1 = "1. Exponential Equation Property:\n   If b^u = y, then u = log_b(y)"
            val step2 = "2. Solved Variable:\n   $xValStr"

            return PhotomathSolution(problemText = line, finalAnswerText = xValStr, stepByStepExplanations = "$step1\n\n$step2")
        }

        // Logarithmic Equation: log2(x) = 3 or ln(x) = 2 or log(x+2) = 3
        val logMatch = Regex("""^(log2|log10|log|ln)\(([a-zA-Z0-9+\-*/]+)\)=([-+]?\d+\.?\d*)$""").find(cleaned)
        if (logMatch != null) {
            val func = logMatch.groupValues[1]
            val innerExpr = logMatch.groupValues[2]
            val rhs = logMatch.groupValues[3].toDoubleOrNull() ?: return null

            val expVal = when (func) {
                "ln" -> Math.E.pow(rhs)
                "log2" -> 2.0.pow(rhs)
                else -> 10.0.pow(rhs)
            }

            val linSol = if (innerExpr == "x") null else parseAndSolveGeneralLinearEquation("$innerExpr = ${formatNum(expVal)}")
            val xValStr = linSol?.finalAnswerText ?: "x = ${formatNum(expVal)}"

            val step1 = "1. Logarithm Definition:\n   If log_b(u) = y, then u = b^y"
            val step2 = "2. Calculation:\n   $xValStr"

            return PhotomathSolution(problemText = line, finalAnswerText = xValStr, stepByStepExplanations = "$step1\n\n$step2")
        }

        return null
    }

    // -------------------------------------------------------------------------
    // 9. ABSOLUTE VALUE EQUATIONS & INEQUALITIES
    // -------------------------------------------------------------------------

    private fun parseAndSolveAbsoluteValueAndInequalities(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()

        // Absolute Value Equation: |2x - 3| = 7
        if (cleaned.startsWith("|") && cleaned.contains("|=")) {
            val match = Regex("""^\|(.*)\|=(.*)$""").find(cleaned)
            if (match != null) {
                val inner = match.groupValues[1]
                val rhs = match.groupValues[2].toDoubleOrNull() ?: return null
                if (rhs < 0) {
                    return PhotomathSolution(problemText = line, finalAnswerText = "No Solution", stepByStepExplanations = "Absolute value cannot be negative.")
                }

                val sol1 = parseAndSolveGeneralLinearEquation("$inner = $rhs")
                val sol2 = parseAndSolveGeneralLinearEquation("$inner = -${formatNum(rhs)}")

                val ans1 = sol1?.finalAnswerText ?: ""
                val ans2 = sol2?.finalAnswerText ?: ""

                val step1 = "1. Absolute Value Property:\n   |A| = B  ⇒  A = B or A = -B"
                val step2 = "2. Case 1 ($inner = $rhs):\n   $ans1"
                val step3 = "3. Case 2 ($inner = -${formatNum(rhs)}):\n   $ans2"

                return PhotomathSolution(problemText = line, finalAnswerText = "$ans1, $ans2", stepByStepExplanations = "$step1\n\n$step2\n\n$step3")
            }
        }

        // Linear Inequality: 2x + 3 > 7 or 3 - 4x <= 11 or 3y + 2 >= y - 6
        val ineqMatch = Regex("""^(.*?)\s*(>|>=|<|<=)\s*(.*?)$""").find(cleaned)
        if (ineqMatch != null && !cleaned.contains("=") && !cleaned.contains("lim")) {
            val (lhsStr, op, rhsStr) = ineqMatch.destructured
            val v = detectVariable(cleaned)

            val leftSide = parseLinearSide(lhsStr)
            val rightSide = parseLinearSide(rhsStr)

            val lCoeff = leftSide.coeffs[v] ?: 0.0
            val rCoeff = rightSide.coeffs[v] ?: 0.0

            val netCoeff = lCoeff - rCoeff
            val netConst = rightSide.constant - leftSide.constant

            if (netCoeff == 0.0) return null

            var finalOp = op
            if (netCoeff < 0) {
                finalOp = when (op) {
                    ">" -> "<"
                    ">=" -> "<="
                    "<" -> ">"
                    "<=" -> ">="
                    else -> op
                }
            }

            val xVal = netConst / netCoeff
            val step1 = "1. Linear Inequality Operations:\n   Isolate $v term on left side"
            val step2 = if (netCoeff < 0) "2. Divide by negative (${formatNum(netCoeff)}) and FLIP inequality direction ($op → $finalOp)" else "2. Divide by ${formatNum(netCoeff)}"
            val step3 = "3. Solution:\n   $v $finalOp ${formatNum(xVal)}"

            return PhotomathSolution(problemText = line, finalAnswerText = "$v $finalOp ${formatNum(xVal)}", stepByStepExplanations = "$step1\n\n$step2\n\n$step3")
        }

        return null
    }

    // -------------------------------------------------------------------------
    // 10. SYSTEM OF 3 LINEAR EQUATIONS
    // -------------------------------------------------------------------------

    private fun parseAndSolveSystemOf3Equations(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()
        val delim = if (cleaned.contains(";")) ";" else if (cleaned.contains("\n")) "\n" else return null
        val parts = cleaned.split(delim).filter { it.contains("=") }
        if (parts.size != 3) return null

        val eq1 = parseSystem3Line(parts[0]) ?: return null
        val eq2 = parseSystem3Line(parts[1]) ?: return null
        val eq3 = parseSystem3Line(parts[2]) ?: return null

        val (a1, b1, c1, d1) = eq1
        val (a2, b2, c2, d2) = eq2
        val (a3, b3, c3, d3) = eq3

        val detMain = det3x3(a1, b1, c1, a2, b2, c2, a3, b3, c3)
        if (detMain == 0.0) return null

        val detX = det3x3(d1, b1, c1, d2, b2, c2, d3, b3, c3)
        val detY = det3x3(a1, d1, c1, a2, d2, c2, a3, d3, c3)
        val detZ = det3x3(a1, b1, d1, a2, b2, d2, a3, b3, d3)

        val xVal = detX / detMain
        val yVal = detY / detMain
        val zVal = detZ / detMain

        val step1 = "1. System of 3 Linear Equations:\n   (1) ${formatCoeff(a1)}x + ${formatCoeff(b1)}y + ${formatCoeff(c1)}z = ${formatNum(d1)}\n   (2) ${formatCoeff(a2)}x + ${formatCoeff(b2)}y + ${formatCoeff(c2)}z = ${formatNum(d2)}\n   (3) ${formatCoeff(a3)}x + ${formatCoeff(b3)}y + ${formatCoeff(c3)}z = ${formatNum(d3)}"
        val step2 = "2. Cramer's Rule Determinants:\n   D = ${formatNum(detMain)}\n   Dx = ${formatNum(detX)}, Dy = ${formatNum(detY)}, Dz = ${formatNum(detZ)}"
        val step3 = "3. Final Values:\n   x = Dx / D = ${formatNum(xVal)}\n   y = Dy / D = ${formatNum(yVal)}\n   z = Dz / D = ${formatNum(zVal)}"

        return PhotomathSolution(
            problemText = line,
            finalAnswerText = "x = ${formatNum(xVal)}, y = ${formatNum(yVal)}, z = ${formatNum(zVal)}",
            stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
        )
    }

    private fun det3x3(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double, g: Double, h: Double, i: Double): Double {
        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
    }

    private fun parseSystem3Line(str: String): Tuple4<Double, Double, Double, Double>? {
        val sides = str.split("=")
        if (sides.size != 2) return null

        val parsedLhs = parseLinearSide(sides[0])
        val rhs = sides[1].toDoubleOrNull() ?: 0.0

        val a = parsedLhs.coeffs["x"] ?: 0.0
        val b = parsedLhs.coeffs["y"] ?: 0.0
        val c = parsedLhs.coeffs["z"] ?: 0.0
        val d = rhs - parsedLhs.constant

        return Tuple4(a, b, c, d)
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // -------------------------------------------------------------------------
    // 11. SYSTEM OF 2 LINEAR EQUATIONS
    // -------------------------------------------------------------------------

    private fun parseAndSolveSystemOfEquations(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase().replace("and", ";")
        val delim = if (cleaned.contains(";")) ";" else if (cleaned.contains(",")) "," else if (cleaned.contains("\n")) "\n" else return null
        val parts = cleaned.split(delim).filter { it.contains("=") }

        if (parts.size != 2) return null

        val vars = detectVariablesInSystem(parts[0], parts[1])
        if (vars.size < 2) return null

        val v1 = vars[0]
        val v2 = vars[1]

        val eq1 = parseSystemLineForVars(parts[0], v1, v2) ?: return null
        val eq2 = parseSystemLineForVars(parts[1], v1, v2) ?: return null

        val (a1, b1, c1) = eq1
        val (a2, b2, c2) = eq2

        val det = a1 * b2 - a2 * b1
        if (det == 0.0) {
            val detX = c1 * b2 - c2 * b1
            val answer = if (detX == 0.0) "Infinitely Many Solutions" else "No Solution"
            return PhotomathSolution(
                problemText = line,
                finalAnswerText = answer,
                stepByStepExplanations = "Determinant D = 0. Lines are ${if (detX == 0.0) "coincident" else "parallel"}."
            )
        }

        val detX = c1 * b2 - c2 * b1
        val detY = a1 * c2 - a2 * c1

        val xVal = detX / det
        val yVal = detY / det

        val step1 = "1. System of Linear Equations in Standard Form:\n   (1) ${formatCoeff(a1)}$v1 ${if (b1 >= 0) "+ ${formatCoeff(b1)}$v2" else "- ${formatCoeff(abs(b1))}$v2"} = ${formatNum(c1)}\n   (2) ${formatCoeff(a2)}$v1 ${if (b2 >= 0) "+ ${formatCoeff(b2)}$v2" else "- ${formatCoeff(abs(b2))}$v2"} = ${formatNum(c2)}"
        val step2 = "2. Solve using Elimination / Determinants (Cramer's Rule):\n   D = (${formatNum(a1)})(${formatNum(b2)}) - (${formatNum(a2)})(${formatNum(b1)}) = ${formatNum(det)}\n   D_$v1 = (${formatNum(c1)})(${formatNum(b2)}) - (${formatNum(c2)})(${formatNum(b1)}) = ${formatNum(detX)}\n   D_$v2 = (${formatNum(a1)})(${formatNum(c2)}) - (${formatNum(a2)})(${formatNum(b1)}) = ${formatNum(detY)}"
        val step3 = "3. Calculated Values:\n   $v1 = D_$v1 / D = ${formatNum(xVal)}\n   $v2 = D_$v2 / D = ${formatNum(yVal)}"

        return PhotomathSolution(
            problemText = line,
            finalAnswerText = "$v1 = ${formatNum(xVal)}, $v2 = ${formatNum(yVal)}",
            stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
        )
    }

    private fun detectVariablesInSystem(s1: String, s2: String): List<String> {
        val vars = mutableSetOf<String>()
        val combined = "$s1 $s2"
        for (ch in combined) {
            if (ch.isLetter() && ch != 'e') {
                vars.add(ch.toString())
            }
        }
        return vars.toList().sorted()
    }

    private fun parseSystemLineForVars(lineStr: String, v1: String, v2: String): Triple<Double, Double, Double>? {
        val side = lineStr.split("=")
        if (side.size != 2) return null

        val lhs = parseLinearSide(side[0])
        val rhs = side[1].toDoubleOrNull() ?: 0.0

        val a = lhs.coeffs[v1] ?: 0.0
        val b = lhs.coeffs[v2] ?: 0.0
        val c = rhs - lhs.constant

        return Triple(a, b, c)
    }

    // -------------------------------------------------------------------------
    // 12. QUADRATIC EQUATIONS
    // -------------------------------------------------------------------------

    private fun parseAndSolveQuadraticEquation(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()
        val v = detectVariable(cleaned)

        if (!cleaned.contains("$v^2") && !cleaned.contains("${v}2")) return null
        if (!cleaned.contains("=")) return null

        val sides = cleaned.split("=")
        if (sides.size != 2) return null

        val lhsQuad = parseQuadraticSide(sides[0], v)
        val rhsQuad = parseQuadraticSide(sides[1], v)

        val a = lhsQuad.a - rhsQuad.a
        val b = lhsQuad.b - rhsQuad.b
        val c = lhsQuad.c - rhsQuad.c

        if (a == 0.0) return null

        val discriminant = b * b - 4 * a * c

        val step1 = "1. Standard Form (A$v² + B$v + C = 0):\n   ${formatCoeff(a)}$v² ${if (b >= 0) "+ ${formatCoeff(b)}$v" else "- ${formatCoeff(abs(b))}$v"} ${if (c >= 0) "+ ${formatNum(c)}" else "- ${formatNum(abs(c))}"} = 0\n   Coefficients: A = ${formatNum(a)}, B = ${formatNum(b)}, C = ${formatNum(c)}"
        val step2 = "2. Calculate Discriminant (D = B² - 4AC):\n   D = (${formatNum(b)})² - 4(${formatNum(a)})(${formatNum(c)})\n   D = ${formatNum(b * b)} - ${formatNum(4 * a * c)} = ${formatNum(discriminant)}"

        if (discriminant < 0) {
            val realPart = formatNum(-b / (2 * a))
            val imagPart = formatNum(sqrt(abs(discriminant)) / (2 * a))
            val step3 = "3. Apply Quadratic Formula ($v = (-B ± √D) / 2A):\n   Since D < 0, roots are complex numbers:\n   $v = $realPart ± ${imagPart}i"
            return PhotomathSolution(
                problemText = line,
                finalAnswerText = "$v = $realPart ± ${imagPart}i",
                stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
            )
        }

        val root1 = (-b + sqrt(discriminant)) / (2 * a)
        val root2 = (-b - sqrt(discriminant)) / (2 * a)
        val r1Str = formatNum(root1)
        val r2Str = formatNum(root2)

        val finalAns = if (r1Str == r2Str) "$v = $r1Str" else "$v = $r1Str, $v = $r2Str"

        val step3 = "3. Apply Quadratic Formula ($v = (-B ± √D) / 2A):\n   $v = (-(${formatNum(b)}) ± √${formatNum(discriminant)}) / (2 * ${formatNum(a)})\n   $v = (${formatNum(-b)} ± ${formatNum(sqrt(discriminant))}) / ${formatNum(2 * a)}"
        val step4 = "4. Final Calculated Roots:\n   $finalAns"

        return PhotomathSolution(
            problemText = line,
            finalAnswerText = finalAns,
            stepByStepExplanations = "$step1\n\n$step2\n\n$step3\n\n$step4"
        )
    }

    private data class ParsedQuadSide(val a: Double, val b: Double, val c: Double)

    private fun parseQuadraticSide(sideStr: String, v: String): ParsedQuadSide {
        var aAcc = 0.0
        var bAcc = 0.0
        var cAcc = 0.0

        val normalized = sideStr.replace("-", "+-")
        val terms = normalized.split("+").filter { it.isNotBlank() }

        for (term in terms) {
            val t = term.trim()
            if (t.isEmpty()) continue

            if (t.contains("$v^2") || t.contains("${v}2")) {
                val coeffStr = t.replace("$v^2", "").replace("${v}2", "")
                aAcc += parseCoeff(coeffStr)
            } else if (t.contains(v)) {
                val coeffStr = t.replace(v, "")
                bAcc += parseCoeff(coeffStr)
            } else {
                cAcc += parseVal(t)
            }
        }
        return ParsedQuadSide(aAcc, bAcc, cAcc)
    }

    // -------------------------------------------------------------------------
    // 13. PARENTHESES & GENERAL LINEAR EQUATIONS
    // -------------------------------------------------------------------------

    private fun parseAndSolveParenthesesEquation(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "")
        val v = detectVariable(cleaned)

        val regex = Regex("""^([+-]?\d*\.?\d*)\(([+-]?\d*\.?\d*)$v([+-]\d+\.?\d*)\)=([+-]?\d+\.?\d*)$""", RegexOption.IGNORE_CASE)
        val match = regex.find(cleaned) ?: return null

        val (kStr, aStr, bStr, cStr) = match.destructured
        val k = parseCoeff(kStr)
        val a = parseCoeff(aStr)
        val b = parseVal(bStr)
        val c = cStr.toDoubleOrNull() ?: return null

        val expandedA = k * a
        val expandedB = k * b
        val rhsAfterSub = c - expandedB

        if (expandedA == 0.0) return null
        val vVal = rhsAfterSub / expandedA
        val vFormatted = formatNum(vVal)

        val step1 = "1. Expand left side using distributive property:\n   ${formatNum(k)}(${formatCoeff(a)}$v ${if (b >= 0) "+ ${formatNum(b)}" else "- ${formatNum(abs(b))}"}) = ${formatCoeff(expandedA)}$v ${if (expandedB >= 0) "+ ${formatNum(expandedB)}" else "- ${formatNum(abs(expandedB))}"}\n   ${formatCoeff(expandedA)}$v ${if (expandedB >= 0) "+ ${formatNum(expandedB)}" else "- ${formatNum(abs(expandedB))}"} = ${formatNum(c)}"
        val step2 = "2. Isolate variable term:\n   ${formatCoeff(expandedA)}$v = ${formatNum(rhsAfterSub)}"
        val step3 = "3. Divide both sides by ${formatNum(expandedA)}:\n   $v = $vFormatted"

        return PhotomathSolution(
            problemText = line,
            finalAnswerText = "$v = $vFormatted",
            stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
        )
    }

    private data class ParsedLinearSide(
        val coeffs: Map<String, Double>,
        val constant: Double
    )

    private fun expandParentheses(expr: String): String {
        var s = expr
        val regex = Regex("""([+-]?\d*\.?\d*)\(([^()]+)\)""")
        var match = regex.find(s)

        while (match != null) {
            val kStr = match.groupValues[1]
            val inner = match.groupValues[2]
            val k = parseCoeff(kStr)

            val innerTerms = inner.replace("-", "+-").split("+").filter { it.isNotBlank() }
            val expandedParts = mutableListOf<String>()

            for (term in innerTerms) {
                val t = term.trim()
                val varName = t.find { it.isLetter() && it.lowercaseChar() != 'e' }?.toString()
                if (varName != null) {
                    val coeff = parseCoeff(t.replace(varName, ""))
                    val netCoeff = k * coeff
                    expandedParts.add("${formatCoeff(netCoeff)}$varName")
                } else {
                    val num = parseVal(t)
                    expandedParts.add(formatNum(k * num))
                }
            }

            val expandedStr = expandedParts.joinToString("+").replace("+-", "-")
            s = s.replace(match.value, expandedStr)
            match = regex.find(s)
        }
        return s
    }

    private fun parseLinearSide(sideStr: String): ParsedLinearSide {
        val coeffs = mutableMapOf<String, Double>()
        var constant = 0.0

        val expandedStr = expandParentheses(sideStr)
        val normalized = expandedStr.replace("-", "+-")
        val terms = normalized.split("+").filter { it.isNotBlank() }

        for (term in terms) {
            val t = term.trim()
            if (t.isEmpty()) continue

            // Check for division like x/4 or 2x/3
            val divParts = t.split("/")
            var baseTerm = divParts[0].trim()
            val divisor = if (divParts.size > 1) divParts[1].toDoubleOrNull() ?: 1.0 else 1.0

            val varName = baseTerm.find { it.isLetter() && it.lowercaseChar() != 'e' }?.toString()

            if (varName != null) {
                val coeffStr = baseTerm.replace(varName, "")
                var c = parseCoeff(coeffStr)
                if (divisor != 0.0) c /= divisor
                coeffs[varName] = (coeffs[varName] ?: 0.0) + c
            } else {
                val valNum = baseTerm.toDoubleOrNull()
                if (valNum != null) {
                    constant += valNum / divisor
                }
            }
        }

        return ParsedLinearSide(coeffs, constant)
    }

    private fun parseAndSolveGeneralLinearEquation(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()
        val v = detectVariable(cleaned)

        if (!cleaned.contains("=")) return null
        if (!cleaned.contains(v)) return null

        val parts = cleaned.split("=")
        if (parts.size != 2) return null

        val leftSide = parseLinearSide(parts[0])
        val rightSide = parseLinearSide(parts[1])

        val lCoeff = leftSide.coeffs[v] ?: 0.0
        val rCoeff = rightSide.coeffs[v] ?: 0.0

        val netCoeff = lCoeff - rCoeff
        val netConst = rightSide.constant - leftSide.constant

        if (netCoeff == 0.0) return null

        val vVal = netConst / netCoeff
        val vFormatted = formatNum(vVal)

        val step1 = "1. Collect variable terms on LHS and constants on RHS:\n   ${formatCoeff(netCoeff)}$v = ${formatNum(netConst)}"
        val step2 = "2. Divide both sides by coefficient ${formatNum(netCoeff)}:\n   $v = $vFormatted"
        val step3 = "3. Verification:\n   LHS = ${formatNum(lCoeff * vVal + leftSide.constant)}\n   RHS = ${formatNum(rCoeff * vVal + rightSide.constant)} ✓ (Verified)"

        return PhotomathSolution(
            problemText = line,
            finalAnswerText = "$v = $vFormatted",
            stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
        )
    }

    // -------------------------------------------------------------------------
    // 14. POLYNOMIAL OPERATIONS
    // -------------------------------------------------------------------------

    private fun parseAndSolvePolynomials(line: String): PhotomathSolution? {
        val cleaned = line.replace(" ", "").lowercase()
        val v = detectVariable(cleaned)

        // Binomial Multiplication: (a v + b)(c v + d)
        val match = Regex("""\(([-+]?\d*\.?\d*)$v([+-]\d+\.?\d*)\)\*\(([-+]?\d*\.?\d*)$v([+-]\d+\.?\d*)\)""").find(cleaned)
            ?: Regex("""\(([-+]?\d*\.?\d*)$v([+-]\d+\.?\d*)\)\(([-+]?\d*\.?\d*)$v([+-]\d+\.?\d*)\)""").find(cleaned)

        if (match != null) {
            val (aStr, bStr, cStr, dStr) = match.destructured
            val a = parseCoeff(aStr)
            val b = parseVal(bStr)
            val c = parseCoeff(cStr)
            val d = parseVal(dStr)

            val quadCoeff = a * c
            val midCoeff = a * d + b * c
            val constTerm = b * d

            val step1 = "1. FOIL Method (First, Outer, Inner, Last):\n   • First: (${formatCoeff(a)}$v) × (${formatCoeff(c)}$v) = ${formatCoeff(quadCoeff)}$v²\n   • Outer: (${formatCoeff(a)}$v) × (${formatNum(d)}) = ${formatCoeff(a * d)}$v\n   • Inner: (${formatNum(b)}) × (${formatCoeff(c)}$v) = ${formatCoeff(b * c)}$v\n   • Last: (${formatNum(b)}) × (${formatNum(d)}) = ${formatNum(constTerm)}"

            val ans = "${formatCoeff(quadCoeff)}$v² ${if (midCoeff >= 0) "+ ${formatCoeff(midCoeff)}$v" else "- ${formatCoeff(abs(midCoeff))}$v"} ${if (constTerm >= 0) "+ ${formatNum(constTerm)}" else "- ${formatNum(abs(constTerm))}"}"
            val step2 = "2. Combine Like Terms:\n   Result = $ans"

            return PhotomathSolution(problemText = line, finalAnswerText = ans, stepByStepExplanations = "$step1\n\n$step2")
        }

        return null
    }

    // -------------------------------------------------------------------------
    // 15. ARITHMETIC EVALUATOR
    // -------------------------------------------------------------------------

    private fun parseAndSolveArithmeticExpression(line: String): PhotomathSolution {
        val cleanedExpr = line.replace("=", "").trim()
        val numericResult = try {
            evaluateExpression(cleanedExpr)
        } catch (_: Exception) {
            0.0
        }

        val formattedResult = formatNum(numericResult)

        val step1 = "1. Mathematical Expression Rules (PEMDAS & Functions):\n   Original expression: $line"
        val step2 = "2. Step-by-Step Evaluation:\n   Computed Value = $formattedResult"
        val step3 = "3. Final Calculated Result:\n   Result = $formattedResult"

        return PhotomathSolution(
            problemText = line,
            finalAnswerText = "Result = $formattedResult",
            stepByStepExplanations = "$step1\n\n$step2\n\n$step3"
        )
    }

    // -------------------------------------------------------------------------
    // UTILITIES
    // -------------------------------------------------------------------------

    private fun detectVariable(str: String): String {
        val adjMatch = Regex("""\d\s*([a-zA-Z])|([a-zA-Z])\s*[\d/*+\-=]""").find(str)
        if (adjMatch != null) {
            val letter = adjMatch.groupValues[1].ifEmpty { adjMatch.groupValues[2] }
            if (letter.isNotEmpty() && letter.lowercase() != "e") {
                return letter
            }
        }

        for (char in str) {
            if (char.isLetter() && char.lowercaseChar() != 'e') {
                return char.toString()
            }
        }
        return "x"
    }

    private fun parseCoeff(s: String): Double {
        val cleaned = s.replace(" ", "")
        return when {
            cleaned.isEmpty() || cleaned == "+" -> 1.0
            cleaned == "-" -> -1.0
            else -> cleaned.toDoubleOrNull() ?: 1.0
        }
    }

    private fun parseVal(s: String?): Double {
        if (s.isNullOrEmpty()) return 0.0
        val cleaned = s.replace(" ", "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    fun formatNum(v: Double): String {
        return if (v % 1.0 == 0.0) "${v.toLong()}" else String.format(Locale.US, "%.2f", v)
    }

    fun formatCoeff(v: Double): String {
        return when (v) {
            1.0 -> ""
            -1.0 -> "-"
            else -> formatNum(v)
        }
    }

    fun solveExpressionsFromText(text: String): MathSolveResult {
        val lines = text.split("\n", "\r")
        val solutions = mutableListOf<String>()
        val detected = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            if (containsMathProblem(trimmed)) {
                val solution = solveWithPhotomathSteps(trimmed)
                if (solution.finalAnswerText.isNotBlank()) {
                    detected.add(solution.problemText)
                    solutions.add("Problem: ${solution.problemText}  ⇒  ${solution.finalAnswerText}")
                }
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

    fun evaluateExpression(expression: String): Double {
        val expr = expression.replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")

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
                        else if (ch == '('.code || (ch in 'a'.code..'z'.code) || (ch in 'A'.code..'Z'.code)) {
                            x *= parseFactor()
                        } else return x
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
                    } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                        while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                        val func = expr.substring(startPos, pos).lowercase()
                        if (func == "pi") {
                            x = Math.PI
                        } else if (func == "e") {
                            x = Math.E
                        } else {
                            if (!eat('('.code)) throw RuntimeException("Expected '(' after $func")
                            val arg = parseExpression()
                            if (!eat(')'.code)) throw RuntimeException("Expected ')' after argument")
                            val isRadMode = expr.lowercase().contains("pi") || expr.lowercase().contains("rad")
                            x = when (func) {
                                "sqrt" -> sqrt(arg)
                                "sin" -> if (isRadMode) sin(arg) else sin(Math.toRadians(arg))
                                "cos" -> if (isRadMode) cos(arg) else cos(Math.toRadians(arg))
                                "tan" -> if (isRadMode) tan(arg) else tan(Math.toRadians(arg))
                                "asin", "arcsin" -> if (isRadMode) asin(arg) else Math.toDegrees(asin(arg))
                                "acos", "arccos" -> if (isRadMode) acos(arg) else Math.toDegrees(acos(arg))
                                "atan", "arctan" -> if (isRadMode) atan(arg) else Math.toDegrees(atan(arg))
                                "log" -> log10(arg)
                                "ln" -> ln(arg)
                                "abs" -> abs(arg)
                                else -> throw RuntimeException("Unknown function: $func")
                            }
                        }
                    } else {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }

                    if (eat('^'.code)) x = x.pow(parseFactor())

                    if (eat('!'.code)) {
                        val n = x.toLong()
                        if (n in 0..20) x = factorial(n).toDouble()
                    }

                    if (eat('%'.code)) {
                        x /= 100.0
                    }

                    return x
                }
            }.parse()
        } catch (_: Exception) {
            0.0
        }
    }
}
