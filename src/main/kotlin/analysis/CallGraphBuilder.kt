package com.zombiedetector.analysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpTrait
import com.zombiedetector.psi.PhpCallVisitor
import com.zombiedetector.psi.PsiUtils

data class CallGraph(
    val edges: Map<SymbolId, Set<SymbolId>>,
    val inboundCounts: Map<SymbolId, Int>
)

/**
 * Builds a conservative call graph by resolving call sites with PSI.
 *
 * Ignored in v1.0:
 * - reflection (call_user_func, variable function names, magic __call)
 * - dynamic include/require resolution
 * - framework routing / DI containers
 */
object CallGraphBuilder {

    fun build(project: Project, phpPsiFiles: List<PsiFile>, defs: DefinitionIndex): CallGraph {
        val edges = LinkedHashMap<SymbolId, MutableSet<SymbolId>>()
        val inbound = HashMap<SymbolId, Int>()

        fun addEdge(from: SymbolId, to: SymbolId) {
            edges.computeIfAbsent(from) { LinkedHashSet() }.add(to)
            inbound[to] = (inbound[to] ?: 0) + 1
        }

        for (psiFile in phpPsiFiles) {
            val vFile = psiFile.virtualFile ?: continue
            if (!PsiUtils.isInProjectContent(project, vFile)) continue
            if (PsiUtils.isInVendor(vFile)) continue
            if (psiFile !is PhpFile) continue

            val fileNode = defs.fileNodes[vFile] ?: FileId(vFile.path)

            // Top-level code: treat as "file entry node"
            psiFile.accept(PhpCallVisitor(
                onResolvedTarget = { resolved ->
                    resolvedToSymbolId(resolved)?.let { addEdge(fileNode, it) }
                },
                onNewClassResolved = { resolved ->
                    val classId = resolvedToSymbolId(resolved)
                    if (classId != null) addEdge(fileNode, classId)
                    // If class has constructor, approximate edge to it.
                    if (resolved is PhpClass) {
                        val ctor = resolved.constructor
                        if (ctor != null) {
                            val mid = MethodId(resolved.fqn, ctor.name, ctor.isStatic)
                            addEdge(fileNode, mid)
                        }
                    }
                }
            ))

            // Function bodies
            val functions = PsiTreeUtil.findChildrenOfType(psiFile, Function::class.java)
            for (fn in functions) {
                if (fn.context is PhpClass || fn.context is PhpTrait) continue
                val from = FunctionId(fn.fqn ?: continue)
                fn.accept(PhpCallVisitor(
                    onResolvedTarget = { resolved -> resolvedToSymbolId(resolved)?.let { addEdge(from, it) } },
                    onNewClassResolved = { resolved ->
                        val classId = resolvedToSymbolId(resolved)
                        if (classId != null) addEdge(from, classId)
                        if (resolved is PhpClass) {
                            resolved.constructor?.let { ctor ->
                                addEdge(from, MethodId(resolved.fqn, ctor.name, ctor.isStatic))
                            }
                        }
                    }
                ))
            }

            // Methods inside classes/traits
            val methods = PsiTreeUtil.findChildrenOfType(psiFile, Method::class.java)
            for (m in methods) {
                val owner = (m.containingClass?.fqn ?: m.containingTrait?.fqn) ?: continue
                val from = MethodId(ownerFqn = owner, name = m.name, isStatic = m.isStatic)
                m.accept(PhpCallVisitor(
                    onResolvedTarget = { resolved -> resolvedToSymbolId(resolved)?.let { addEdge(from, it) } },
                    onNewClassResolved = { resolved ->
                        val classId = resolvedToSymbolId(resolved)
                        if (classId != null) addEdge(from, classId)
                        if (resolved is PhpClass) {
                            resolved.constructor?.let { ctor ->
                                addEdge(from, MethodId(resolved.fqn, ctor.name, ctor.isStatic))
                            }
                        }
                    }
                ))
            }
        }

        return CallGraph(edges = edges, inboundCounts = inbound)
    }

    private fun resolvedToSymbolId(resolved: PsiElement): SymbolId? = when (resolved) {
        is Function -> {
            if (resolved.context is PhpClass || resolved.context is PhpTrait) null
            else resolved.fqn?.let { FunctionId(it) }
        }
        is Method -> {
            val owner = resolved.containingClass?.fqn ?: resolved.containingTrait?.fqn
            if (owner.isNullOrBlank()) null else MethodId(ownerFqn = owner, name = resolved.name, isStatic = resolved.isStatic)
        }
        is PhpClass -> resolved.fqn?.let { ClassId(it) }
        is PhpTrait -> resolved.fqn?.let { TraitId(it) }
        else -> null
    }
}

