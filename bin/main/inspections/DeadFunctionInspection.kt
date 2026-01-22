package com.zombiedetector.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.zombiedetector.analysis.DeadCodeAnalyzer
import com.zombiedetector.analysis.FunctionId

class DeadFunctionInspection : LocalInspectionTool() {

    override fun getShortName(): String = "DeadFunctionInspection"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project
        val analyzer = project.getService(DeadCodeAnalyzer::class.java)
        val snapshot = analyzer.getSnapshot()

        return object : PhpElementVisitor() {
            override fun visitPhpFunction(function: Function) {
                // Skip methods; they are handled by DeadMethodInspection
                // Functions inside classes or traits are methods, not global functions
                if (function.context is PhpClass) return

                val fqn = function.fqn ?: return
                val id = FunctionId(fqn)
                val dead = snapshot?.deadFunctions?.contains(id) == true
                val zombie = snapshot?.zombieFunctions?.contains(id) == true
                if (!dead && !zombie) return

                val message = if (dead) {
                    "Function '${function.name}' is never called in this project (dead code)"
                } else {
                    "Function '${function.name}' is only reachable from dead code (zombie code)"
                }

                val nameIdentifier = function.nameIdentifier ?: function
                holder.registerProblem(
                    nameIdentifier,
                    message,
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL
                )
            }
        }
    }
}

