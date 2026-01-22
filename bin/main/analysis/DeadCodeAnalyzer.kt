package com.zombiedetector.analysis

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.messages.Topic
import com.zombiedetector.psi.PsiUtils
import com.zombiedetector.settings.DeadCodeSettings
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Project-level service that owns dead/zombie analysis:
 * - collects PHP PSI
 * - builds definition index + call graph
 * - runs reachability from entry points
 * - exposes cached DeadCodeSnapshot for inspections & tool window
 *
 * Heavy work always runs in a background task under ReadAction + smart mode.
 */
@Service(Level.PROJECT)
class DeadCodeAnalyzer(private val project: Project) {

    @Volatile
    private var snapshot: DeadCodeSnapshot? = null

    @Volatile
    private var lastModCount: Long = -1

    private val recomputing = AtomicBoolean(false)

    fun getSnapshot(): DeadCodeSnapshot? {
        maybeScheduleRecompute()
        return snapshot
    }

    private fun maybeScheduleRecompute() {
        val tracker = PsiModificationTracker.getInstance(project)
        val currentModCount = tracker.modificationCount
        if (currentModCount == lastModCount) return
        if (!recomputing.compareAndSet(false, true)) return

        lastModCount = currentModCount

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "PHP Dead & Zombie Code Analysis",
            false
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    computeInBackground(indicator)
                } finally {
                    recomputing.set(false)
                }
            }
        })
    }

    private fun computeInBackground(indicator: ProgressIndicator) {
        if (project.isDisposed) return

        indicator.text = "Collecting PHP files..."

        val vFiles = PsiUtils.allProjectPhpFiles(project)
        if (vFiles.isEmpty()) {
            updateSnapshot(null)
            return
        }

        val psiManager = PsiManager.getInstance(project)

        DumbService.getInstance(project).runReadActionInSmartMode {
            indicator.checkCanceled()

            val psiFiles = vFiles.mapNotNull { vf ->
                indicator.checkCanceled()
                psiManager.findFile(vf)
            }

            indicator.text = "Indexing PHP definitions..."
            val defs = DefinitionIndexer.index(project, psiFiles)

            indicator.text = "Building PHP call graph..."
            val graph = CallGraphBuilder.build(project, psiFiles, defs)

            indicator.text = "Computing entry points..."
            val settings = project.getService(DeadCodeSettings::class.java)
            val entryPoints = computeEntryPoints(defs, settings)

            indicator.text = "Analyzing reachability..."
            val reach = ReachabilityAnalyzer.analyze(defs, graph, entryPoints)

            val pointers: MutableMap<SymbolId, com.intellij.psi.SmartPsiElementPointer<out com.intellij.psi.PsiElement>> =
                HashMap()
            for ((vf, id) in defs.fileNodes) {
                psiManager.findFile(vf)?.let {
                    pointers[id] = com.intellij.psi.SmartPointerManager.getInstance(project)
                        .createSmartPsiElementPointer(it)
                }
            }
            for ((fqn, ptr) in defs.classesByFqn) pointers[ClassId(fqn)] = ptr
            for ((fqn, ptr) in defs.traitsByFqn) pointers[TraitId(fqn)] = ptr
            for ((fqn, ptr) in defs.functionsByFqn) pointers[FunctionId(fqn)] = ptr
            for ((id, ptr) in defs.methodsById) pointers[id] = ptr

            val newSnapshot = DeadCodeSnapshot(
                timestamp = System.currentTimeMillis(),
                deadClasses = reach.deadClasses,
                deadTraits = reach.deadTraits,
                deadFunctions = reach.deadFunctions,
                deadMethods = reach.deadMethods,
                zombieClasses = reach.zombieClasses,
                zombieTraits = reach.zombieTraits,
                zombieFunctions = reach.zombieFunctions,
                zombieMethods = reach.zombieMethods,
                symbolPointers = pointers
            )

            updateSnapshot(newSnapshot)
        }
    }

    private fun computeEntryPoints(
        defs: DefinitionIndex,
        settings: DeadCodeSettings
    ): Set<SymbolId> {
        val result = LinkedHashSet<SymbolId>()
        val baseDir = PsiUtils.projectBaseDir(project)

        for ((vf, fileId) in defs.fileNodes) {
            val name = vf.name
            val relPath = if (baseDir != null) vf.path.removePrefix(baseDir.path).trimStart('/', '\\') else vf.path

            // index.php anywhere
            if (name.equals("index.php", ignoreCase = true)) {
                result.add(fileId)
                continue
            }

            // public/*.php
            if (relPath.startsWith("public/") || relPath.startsWith("public\\")) {
                result.add(fileId)
                continue
            }

            // bin/*.php (CLI)
            if (relPath.startsWith("bin/") || relPath.startsWith("bin\\")) {
                result.add(fileId)
                continue
            }

            // root-level *.php
            if (baseDir != null && vf.parent == baseDir) {
                result.add(fileId)
                continue
            }

            // tests (optional)
            if (settings.state.includeTests) {
                if (relPath.startsWith("tests/") || relPath.startsWith("test/") ||
                    relPath.contains("/tests/") || relPath.contains("\\tests\\")
                ) {
                    result.add(fileId)
                    continue
                }
            }
        }

        return result
    }

    private fun updateSnapshot(newSnapshot: DeadCodeSnapshot?) {
        snapshot = newSnapshot
        project.messageBus.syncPublisher(TOPIC).analysisUpdated(newSnapshot)
    }

    fun resolve(symbolId: SymbolId): com.intellij.psi.PsiElement? {
        val snap = snapshot ?: return null
        val ptr = snap.symbolPointers[symbolId] ?: return null
        return ptr.element
    }

    interface Listener {
        fun analysisUpdated(snapshot: DeadCodeSnapshot?)
    }

    companion object {
        val TOPIC: Topic<Listener> = Topic.create(
            "PHP Dead & Zombie Code Detector",
            Listener::class.java,
            Topic.BroadcastDirection.TO_DIRECT_CHILDREN
        )

        private val LOG = Logger.getInstance(DeadCodeAnalyzer::class.java)
    }
}

