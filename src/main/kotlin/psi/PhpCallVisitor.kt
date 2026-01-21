package com.zombiedetector.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.NewExpression
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.jetbrains.php.lang.psi.elements.StaticMethodReference

/**
 * Collects *resolvable* call sites using PHP PSI.
 *
 * Dynamic dispatch, reflection, variable function names, and string-based calls are intentionally ignored
 * for performance + correctness tradeoffs in v1.0.
 */
class PhpCallVisitor(
    private val onResolvedTarget: (PsiElement) -> Unit,
    private val onNewClassResolved: (PsiElement) -> Unit
) : PsiRecursiveElementWalkingVisitor() {

    override fun visitElement(element: PsiElement) {
        when (element) {
            is FunctionReference -> resolveAndReport(element)
            is MethodReference -> resolveAndReport(element)
            is StaticMethodReference -> resolveAndReport(element)
            is NewExpression -> resolveNewAndReport(element)
        }
        super.visitElement(element)
    }

    private fun resolveAndReport(ref: PhpReference) {
        val resolved = try {
            ref.resolve()
        } catch (_: Throwable) {
            null
        }
        if (resolved != null) onResolvedTarget(resolved)
    }

    private fun resolveNewAndReport(expr: NewExpression) {
        val classRef = expr.classReference
        val resolved = try {
            classRef?.resolve()
        } catch (_: Throwable) {
            null
        }
        if (resolved != null) onNewClassResolved(resolved)
    }
}

