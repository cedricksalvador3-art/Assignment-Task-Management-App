package com.example.assignment_task_management_app

import com.example.assignment_task_management_app.utils.MathExpressionSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExpressionSolverTest {

    @Test
    fun testLinearEquation_standardForm() {
        val problem = "5x + 10 = 35"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("5x + 10 = 35", solution.problemText)
        assertEquals("x = 5", solution.finalAnswerText)
    }

    @Test
    fun testLinearEquation_constantFirst() {
        val problem = "10 + 5x = 35"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("10 + 5x = 35", solution.problemText)
        assertEquals("x = 5", solution.finalAnswerText)
    }

    @Test
    fun testLinearEquation_variableOnRhs() {
        val problem = "35 = 5x + 10"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("35 = 5x + 10", solution.problemText)
        assertEquals("x = 5", solution.finalAnswerText)
    }

    @Test
    fun testLinearEquation_division() {
        val problem = "x / 4 = 5"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("x / 4 = 5", solution.problemText)
        assertEquals("x = 20", solution.finalAnswerText)
    }

    @Test
    fun testLinearEquation_decimals() {
        val problem = "2.5x + 1.5 = 6.5"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("2.5x + 1.5 = 6.5", solution.problemText)
        assertEquals("x = 2", solution.finalAnswerText)
    }

    @Test
    fun testLinearEquation_differentVariableSymbol() {
        val problem = "3y - 12 = 42"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("3y - 12 = 42", solution.problemText)
        assertEquals("y = 18", solution.finalAnswerText)
    }

    @Test
    fun testParenthesesEquationSolving() {
        val problem = "2(x + 3) = 14"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("2(x + 3) = 14", solution.problemText)
        assertEquals("x = 4", solution.finalAnswerText)
        assertTrue(solution.stepByStepExplanations.contains("distributive property"))
    }

    @Test
    fun testQuadraticEquationSolving() {
        val problem = "x^2 - 5x + 6 = 0"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("x^2 - 5x + 6 = 0", solution.problemText)
        assertTrue(solution.finalAnswerText.contains("x = 3"))
        assertTrue(solution.finalAnswerText.contains("x = 2"))
        assertTrue(solution.stepByStepExplanations.contains("Discriminant"))
    }

    @Test
    fun testQuadraticEquationWithoutLinearTerm() {
        val problem = "2x^2 - 8 = 0"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertTrue(solution.finalAnswerText.contains("x = 2"))
        assertTrue(solution.finalAnswerText.contains("x = -2"))
    }

    @Test
    fun testSystemOfEquationsSolving() {
        val problem = "2x + y = 7; x - y = 2"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("2x + y = 7; x - y = 2", solution.problemText)
        assertEquals("x = 3, y = 1", solution.finalAnswerText)
    }

    @Test
    fun testSystemOfEquationsWithAnd() {
        val problem = "2x + y = 7 and x - y = 2"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("x = 3, y = 1", solution.finalAnswerText)
    }

    @Test
    fun testSystemOf3EquationsSolving() {
        val problem = "x + y + z = 6; 2x - y + z = 3; x - y - z = -4"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("x = 1, y = 2, z = 3", solution.finalAnswerText)
    }

    @Test
    fun testTrigonometryEvaluation() {
        val problem = "sin(30) + cos(60)"
        val valResult = MathExpressionSolver.evaluateExpression(problem)
        assertEquals(1.0, valResult, 0.01)
    }

    @Test
    fun testTrigonometryExactValues() {
        val problem = "sin(30)"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("sin(30) = 1/2", solution.finalAnswerText)
    }

    @Test
    fun testCalculusDerivative() {
        val problem = "d/dx(3x^2 + 5x - 4)"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("d/dx = 6x + 5", solution.finalAnswerText)
        assertTrue(solution.stepByStepExplanations.contains("Power Rule"))
    }

    @Test
    fun testCalculusTrigDerivative() {
        val problem = "d/dx(2sin(x) + 3cos(x))"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("d/dx = 2cos(x) - 3sin(x)", solution.finalAnswerText)
    }

    @Test
    fun testCalculusIntegral() {
        val problem = "int(3x^2 + 5x - 4)dx"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertTrue(solution.finalAnswerText.contains("x^3"))
        assertTrue(solution.stepByStepExplanations.contains("Integration Rules"))
    }

    @Test
    fun testDefiniteIntegral() {
        val problem = "int(0 to 2) 3x^2 dx"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Result = 8", solution.finalAnswerText)
    }

    @Test
    fun testCalculusLimitIndeterminate() {
        val problem = "lim x->2 (x^2 - 4)/(x - 2)"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Limit = 4", solution.finalAnswerText)
    }

    @Test
    fun testGeometryArea() {
        val problem = "area of circle with r = 5"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertTrue(solution.finalAnswerText.contains("Area = 78.54"))
    }

    @Test
    fun testGeometryHypotenuse() {
        val problem = "hypotenuse with a = 3, b = 4"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Hypotenuse c = 5", solution.finalAnswerText)
    }

    @Test
    fun testCoordinateGeometryDistance() {
        val problem = "distance between (1, 2) and (4, 6)"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Distance = 5", solution.finalAnswerText)
    }

    @Test
    fun testFractionsAddition() {
        val problem = "3/4 + 5/6"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Result = 19/12", solution.finalAnswerText)
    }

    @Test
    fun testPercentageCalculation() {
        val problem = "15% of 200"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Result = 30", solution.finalAnswerText)
    }

    @Test
    fun testPercentageWordCalculation() {
        val problem = "15 percent of 200"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Result = 30", solution.finalAnswerText)
    }

    @Test
    fun testStatisticsMeanMedian() {
        val problem = "mean of 10, 20, 30"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertTrue(solution.finalAnswerText.contains("Mean = 20"))
    }

    @Test
    fun testCombinatoricsPermutations() {
        val problem = "5P2"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("5P2 = 20", solution.finalAnswerText)
    }

    @Test
    fun testCombinatoricsCombinations() {
        val problem = "5C2"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("5C2 = 10", solution.finalAnswerText)
    }

    @Test
    fun testMatrixDeterminant2x2() {
        val problem = "det([[1, 2], [3, 4]])"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Determinant = -2", solution.finalAnswerText)
    }

    @Test
    fun testMatrixDeterminant3x3() {
        val problem = "det([[1, 2, 3], [0, 1, 4], [5, 6, 0]])"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Determinant = 1", solution.finalAnswerText)
    }

    @Test
    fun testVectorDotProduct() {
        val problem = "dot product of (1, 2, 3) and (4, 5, 6)"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Dot Product = 32", solution.finalAnswerText)
    }

    @Test
    fun testVectorCrossProduct() {
        val problem = "cross product of (1, 0, 0) and (0, 1, 0)"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("Cross Product = (0, 0, 1)", solution.finalAnswerText)
    }

    @Test
    fun testRadicalSimplification() {
        val problem = "sqrt(72)"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("√72 = 6√2", solution.finalAnswerText)
    }

    @Test
    fun testComplexMultiplication() {
        val problem = "(2+3i)(4-5i)"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("23 + 2i", solution.finalAnswerText)
    }

    @Test
    fun testExponentialEquation() {
        val problem = "2^x = 16"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("x = 4", solution.finalAnswerText)
    }

    @Test
    fun testLogarithmicEquation() {
        val problem = "log2(x) = 3"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("x = 8", solution.finalAnswerText)
    }

    @Test
    fun testAbsoluteValueEquation() {
        val problem = "|2x - 3| = 7"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertTrue(solution.finalAnswerText.contains("x = 5"))
        assertTrue(solution.finalAnswerText.contains("x = -2"))
    }

    @Test
    fun testLinearInequality() {
        val problem = "2x + 3 > 7"
        val solution = MathExpressionSolver.solveWithPhotomathSteps(problem)
        assertEquals("x > 2", solution.finalAnswerText)
    }

    @Test
    fun testImplicitMultiplicationInExpression() {
        val result = MathExpressionSolver.evaluateExpression("2(3 + 4)")
        assertEquals(14.0, result, 0.01)
    }

    @Test
    fun testOcrNoiseCleaning() {
        val rawOcr = "Solve: l0 + 5x = 35"
        val extracted = MathExpressionSolver.extractMathProblemFromText(rawOcr)
        val solution = MathExpressionSolver.solveWithPhotomathSteps(extracted)
        assertEquals("x = 5", solution.finalAnswerText)
    }
}
