package com.zombiedetector.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.zombiedetector.analysis.ClassId
import com.zombiedetector.analysis.DeadCodeAnalyzer
import com.zombiedetector.analysis.MethodId
import com.zombiedetector.psi.isStaticMethod

class DeadMethodInspection : LocalInspectionTool() {

    override fun getShortName(): String = "DeadMethodInspection"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project
        val analyzer = project.getService(DeadCodeAnalyzer::class.java)

        // We never trigger analysis here: we only consume cached snapshot.
        val snapshot = analyzer.getSnapshot()

        return object : PhpElementVisitor() {
            override fun visitPhpMethod(method: Method) {
                val containingClass = method.containingClass ?: return
                val fqn = containingClass.fqn ?: return

                val id = MethodId(
                    ownerFqn = fqn,
                    name = method.name,
                    isStatic = method.isStaticMethod
                )

                val dead = snapshot?.deadMethods?.contains(id) == true
                val zombie = snapshot?.zombieMethods?.contains(id) == true
                if (!dead && !zombie) return

                // Do not flag magic methods, as they may be called implicitly.
                if (method.name.startsWith("__")) return

                val message = if (dead) {
                    "Method '${method.name}' is never called in this project (dead code)"
                } else {
                    "Method '${method.name}' is only reachable from dead code (zombie code)"
                }

                val nameIdentifier = method.nameIdentifier ?: method
                holder.registerProblem(
                    nameIdentifier,
                    message,
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL
                )
            }
        }
    }
}

