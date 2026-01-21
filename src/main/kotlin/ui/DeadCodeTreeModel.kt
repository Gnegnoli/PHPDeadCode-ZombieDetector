package com.zombiedetector.ui

import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.OpenSourceUtil
import com.zombiedetector.analysis.ClassId
import com.zombiedetector.analysis.DeadCodeAnalyzer
import com.zombiedetector.analysis.DeadCodeSnapshot
import com.zombiedetector.analysis.FunctionId
import com.zombiedetector.analysis.MethodId
import com.zombiedetector.analysis.SymbolId
import com.zombiedetector.analysis.TraitId
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class DeadCodeTreeModel(
    private val project: Project
) : DefaultTreeModel(DefaultMutableTreeNode("Dead & Zombie Code")) {

    fun update(snapshot: DeadCodeSnapshot?) {
        val root = DefaultMutableTreeNode("Dead & Zombie Code")
        if (snapshot != null) {
            val deadClassesNode = DefaultMutableTreeNode("Dead Classes (${snapshot.deadClasses.size})")
            val deadMethodsNode = DefaultMutableTreeNode("Dead Methods (${snapshot.deadMethods.size})")
            val deadFunctionsNode = DefaultMutableTreeNode("Dead Functions (${snapshot.deadFunctions.size})")

            val zombieClassesNode = DefaultMutableTreeNode("Zombie Classes (${snapshot.zombieClasses.size})")
            val zombieMethodsNode = DefaultMutableTreeNode("Zombie Methods (${snapshot.zombieMethods.size})")
            val zombieFunctionsNode = DefaultMutableTreeNode("Zombie Functions (${snapshot.zombieFunctions.size})")

            for (id in snapshot.deadClasses.sortedBy { it.fqn }) {
                rootOrAdd(deadClassesNode, SymbolNode("Class ${id.fqn}", id))
            }
            for (id in snapshot.deadTraits.sortedBy { it.fqn }) {
                rootOrAdd(deadClassesNode, SymbolNode("Trait ${id.fqn}", id))
            }
            for (id in snapshot.deadMethods.sortedWith(compareBy({ it.ownerFqn }, { it.name }))) {
                val label = "${id.ownerFqn}::${id.name}"
                rootOrAdd(deadMethodsNode, SymbolNode(label, id))
            }
            for (id in snapshot.deadFunctions.sortedBy { it.fqn }) {
                rootOrAdd(deadFunctionsNode, SymbolNode(id.fqn, id))
            }

            for (id in snapshot.zombieClasses.sortedBy { it.fqn }) {
                rootOrAdd(zombieClassesNode, SymbolNode("Class ${id.fqn}", id))
            }
            for (id in snapshot.zombieTraits.sortedBy { it.fqn }) {
                rootOrAdd(zombieClassesNode, SymbolNode("Trait ${id.fqn}", id))
            }
            for (id in snapshot.zombieMethods.sortedWith(compareBy({ it.ownerFqn }, { it.name }))) {
                val label = "${id.ownerFqn}::${id.name}"
                rootOrAdd(zombieMethodsNode, SymbolNode(label, id))
            }
            for (id in snapshot.zombieFunctions.sortedBy { it.fqn }) {
                rootOrAdd(zombieFunctionsNode, SymbolNode(id.fqn, id))
            }

            if (deadClassesNode.childCount > 0) root.add(deadClassesNode)
            if (deadMethodsNode.childCount > 0) root.add(deadMethodsNode)
            if (deadFunctionsNode.childCount > 0) root.add(deadFunctionsNode)
            if (zombieClassesNode.childCount > 0) root.add(zombieClassesNode)
            if (zombieMethodsNode.childCount > 0) root.add(zombieMethodsNode)
            if (zombieFunctionsNode.childCount > 0) root.add(zombieFunctionsNode)
        }

        setRoot(root)
        reload()
    }

    private fun rootOrAdd(parent: DefaultMutableTreeNode, symbolNode: SymbolNode) {
        parent.add(DefaultMutableTreeNode(symbolNode))
    }

    data class SymbolNode(
        val text: String,
        val id: SymbolId
    ) {
        override fun toString(): String = text
    }

    fun navigateTo(node: SymbolNode) {
        val analyzer = project.getService(DeadCodeAnalyzer::class.java)
        val element = analyzer.resolve(node.id) ?: return
        openInEditor(element)
    }

    private fun openInEditor(element: PsiElement) {
        val navigatable = element as? Navigatable ?: PsiUtilCore.getNavigationElement(element) as? Navigatable
        if (navigatable != null && navigatable.canNavigate()) {
            OpenSourceUtil.navigate(true, navigatable)
        }
    }
}

