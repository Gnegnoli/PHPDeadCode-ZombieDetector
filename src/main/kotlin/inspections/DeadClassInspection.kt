package com.zombiedetector.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpTrait
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.zombiedetector.analysis.ClassId
import com.zombiedetector.analysis.DeadCodeAnalyzer
import com.zombiedetector.analysis.TraitId

class DeadClassInspection : LocalInspectionTool() {

    override fun getShortName(): String = "DeadClassInspection"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project
        val analyzer = project.getService(DeadCodeAnalyzer::class.java)
        val snapshot = analyzer.getSnapshot()

        return object : PhpElementVisitor() {
            override fun visitPhpClass(clazz: PhpClass) {
                val fqn = clazz.fqn ?: return
                val id = ClassId(fqn)

                val dead = snapshot?.deadClasses?.contains(id) == true
                val zombie = snapshot?.zombieClasses?.contains(id) == true
                if (!dead && !zombie) return

                val message = if (dead) {
                    "Class '${clazz.name}' is never instantiated or referenced in this project (dead code)"
                } else {
                    "Class '${clazz.name}' is only reachable from dead code (zombie code)"
                }

                val nameIdentifier = clazz.nameIdentifier ?: clazz
                holder.registerProblem(
                    nameIdentifier,
                    message,
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL
                )
            }

            override fun visitPhpTrait(trait: PhpTrait) {
                val fqn = trait.fqn ?: return
                val id = TraitId(fqn)

                val dead = snapshot?.deadTraits?.contains(id) == true
                val zombie = snapshot?.zombieTraits?.contains(id) == true
                if (!dead && !zombie) return

                val message = if (dead) {
                    "Trait '${trait.name}' is never used in this project (dead code)"
                } else {
                    "Trait '${trait.name}' is only reachable from dead code (zombie code)"
                }

                val nameIdentifier = trait.nameIdentifier ?: trait
                holder.registerProblem(
                    nameIdentifier,
                    message,
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL
                )
            }
        }
    }
}

