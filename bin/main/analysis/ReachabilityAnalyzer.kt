package com.zombiedetector.analysis

import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.PsiElement

sealed interface SymbolId

data class FileId(val path: String) : SymbolId
data class ClassId(val fqn: String) : SymbolId
data class TraitId(val fqn: String) : SymbolId
data class FunctionId(val fqn: String) : SymbolId
data class MethodId(val ownerFqn: String, val name: String, val isStatic: Boolean) : SymbolId

data class DeadCodeSnapshot(
    val timestamp: Long,
    val deadClasses: Set<ClassId>,
    val deadTraits: Set<TraitId>,
    val deadFunctions: Set<FunctionId>,
    val deadMethods: Set<MethodId>,
    val zombieClasses: Set<ClassId>,
    val zombieTraits: Set<TraitId>,
    val zombieFunctions: Set<FunctionId>,
    val zombieMethods: Set<MethodId>,
    /**
     * Optional back-reference map for navigation, if analyzer chooses to populate it.
     * Keys may include file IDs as well, though the tool window only uses symbol-level IDs.
     */
    val symbolPointers: Map<SymbolId, SmartPsiElementPointer<out PsiElement>>
)

data class ReachabilityResult(
    val deadClasses: Set<ClassId>,
    val deadTraits: Set<TraitId>,
    val deadFunctions: Set<FunctionId>,
    val deadMethods: Set<MethodId>,
    val zombieClasses: Set<ClassId>,
    val zombieTraits: Set<TraitId>,
    val zombieFunctions: Set<FunctionId>,
    val zombieMethods: Set<MethodId>
)

/**
 * Given a call graph, a definition index, and a set of entry points, classifies symbols as:
 * - Dead: not reachable from any entry point and never referenced (inbound degree == 0)
 * - Zombie: not reachable from entry points but referenced from other unreachable code
 */
object ReachabilityAnalyzer {

    fun analyze(
        defs: DefinitionIndex,
        graph: CallGraph,
        entryPoints: Set<SymbolId>
    ): ReachabilityResult {
        val allSymbols: MutableSet<SymbolId> = LinkedHashSet()
        allSymbols.addAll(defs.fileNodes.values)
        allSymbols.addAll(defs.classesByFqn.keys.map(::ClassId))
        allSymbols.addAll(defs.traitsByFqn.keys.map(::TraitId))
        allSymbols.addAll(defs.functionsByFqn.keys.map(::FunctionId))
        allSymbols.addAll(defs.methodsById.keys)

        val reachable: MutableSet<SymbolId> = LinkedHashSet()

        // BFS from entry points
        val work = ArrayDeque<SymbolId>()
        for (e in entryPoints) {
            if (reachable.add(e)) work.add(e)
        }
        while (work.isNotEmpty()) {
            val cur = work.removeFirst()
            val outs = graph.edges[cur] ?: continue
            for (n in outs) {
                if (reachable.add(n)) {
                    work.add(n)
                }
            }
        }

        val deadClasses = LinkedHashSet<ClassId>()
        val zombieClasses = LinkedHashSet<ClassId>()
        val deadTraits = LinkedHashSet<TraitId>()
        val zombieTraits = LinkedHashSet<TraitId>()
        val deadFunctions = LinkedHashSet<FunctionId>()
        val zombieFunctions = LinkedHashSet<FunctionId>()
        val deadMethods = LinkedHashSet<MethodId>()
        val zombieMethods = LinkedHashSet<MethodId>()

        fun isDead(sym: SymbolId): Boolean = (graph.inboundCounts[sym] ?: 0) == 0

        for (sym in allSymbols) {
            if (sym in reachable) continue
            when (sym) {
                is ClassId -> if (isDead(sym)) deadClasses.add(sym) else zombieClasses.add(sym)
                is TraitId -> if (isDead(sym)) deadTraits.add(sym) else zombieTraits.add(sym)
                is FunctionId -> if (isDead(sym)) deadFunctions.add(sym) else zombieFunctions.add(sym)
                is MethodId -> if (isDead(sym)) deadMethods.add(sym) else zombieMethods.add(sym)
                is FileId -> {
                    // We don't expose file-level dead/zombie classification in v1.0
                }
            }
        }

        return ReachabilityResult(
            deadClasses = deadClasses,
            deadTraits = deadTraits,
            deadFunctions = deadFunctions,
            deadMethods = deadMethods,
            zombieClasses = zombieClasses,
            zombieTraits = zombieTraits,
            zombieFunctions = zombieFunctions,
            zombieMethods = zombieMethods
        )
    }
}

