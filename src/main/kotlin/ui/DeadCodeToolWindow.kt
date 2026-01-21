package com.zombiedetector.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTree
import com.intellij.ui.content.ContentFactory
import com.zombiedetector.analysis.DeadCodeAnalyzer
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode

import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class DeadCodeToolWindow : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val model = DeadCodeTreeModel(project)
        val tree = JBTree(model)
        tree.isRootVisible = true

        tree.addTreeSelectionListener { e ->
            val node = e.path.lastPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val userObject = node.userObject
            if (userObject is DeadCodeTreeModel.SymbolNode) {
                // Single-click selection only shows details; navigation is via double-click / Enter (handled below)
            }
        }

        tree.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selPath = tree.getPathForLocation(e.x, e.y) ?: return
                    val node = selPath.lastPathComponent as? DefaultMutableTreeNode ?: return
                    val userObject = node.userObject
                    if (userObject is DeadCodeTreeModel.SymbolNode) {
                        model.navigateTo(userObject)
                    }
                }
            }
        })

        val panel: JComponent = SimpleToolWindowPanel(true, true).apply {
            setContent(ScrollPaneFactory.createScrollPane(tree))
        }

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        val analyzer = project.getService(DeadCodeAnalyzer::class.java)
        model.update(analyzer.getSnapshot())

        project.messageBus.connect(toolWindow.disposable).subscribe(
            DeadCodeAnalyzer.TOPIC,
            object : DeadCodeAnalyzer.Listener {
                override fun analysisUpdated(snapshot: com.zombiedetector.analysis.DeadCodeSnapshot?) {
                    SwingUtilities.invokeLater {
                        model.update(snapshot)
                    }
                }
            }
        )
    }
}

