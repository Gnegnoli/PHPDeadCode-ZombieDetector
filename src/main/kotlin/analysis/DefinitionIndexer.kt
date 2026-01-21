package com.zombiedetector.analysis

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpModifier
import com.jetbrains.php.lang.psi.elements.PhpTrait
import com.zombiedetector.psi.PsiUtils

data class DefinitionIndex(
    val classesByFqn: Map<String, SmartPsiElementPointer<PhpClass>>,
    val traitsByFqn: Map<String, SmartPsiElementPointer<PhpTrait>>,
    val functionsByFqn: Map<String, SmartPsiElementPointer<Function>>,
    val methodsById: Map<MethodId, SmartPsiElementPointer<Method>>,
    val fileNodes: Map<VirtualFile, FileId>
)

/**
 * PSI-based definition indexing for PHP symbols.
 * Uses SmartPsiElementPointer so results remain valid across edits where possible.
 */
object DefinitionIndexer {

    fun index(project: Project, phpPsiFiles: List<PsiFile>): DefinitionIndex {
        val spm = SmartPointerManager.getInstance(project)

        val classes = LinkedHashMap<String, SmartPsiElementPointer<PhpClass>>()
        val traits = LinkedHashMap<String, SmartPsiElementPointer<PhpTrait>>()
        val functions = LinkedHashMap<String, SmartPsiElementPointer<Function>>()
        val methods = LinkedHashMap<MethodId, SmartPsiElementPointer<Method>>()
        val fileNodes = LinkedHashMap<VirtualFile, FileId>()

        for (psiFile in phpPsiFiles) {
            val vFile = psiFile.virtualFile ?: continue
            if (!PsiUtils.isInProjectContent(project, vFile)) continue
            if (PsiUtils.isInVendor(vFile)) continue
            if (psiFile !is PhpFile) continue

            fileNodes[vFile] = FileId(vFile.path)

            val fileClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass::class.java)
            for (cls in fileClasses) {
                val fqn = runCatching { cls.fqn }.getOrNull()
                if (fqn.isNullOrBlank()) continue
                classes[fqn] = spm.createSmartPsiElementPointer(cls)
                for (m in cls.methods) {
                    val id = MethodId(ownerFqn = fqn, name = m.name, isStatic = m.isStatic)
                    methods[id] = spm.createSmartPsiElementPointer(m)
                }
            }

            val fileTraits = PsiTreeUtil.findChildrenOfType(psiFile, PhpTrait::class.java)
            for (tr in fileTraits) {
                val fqn = runCatching { tr.fqn }.getOrNull()
                if (fqn.isNullOrBlank()) continue
                traits[fqn] = spm.createSmartPsiElementPointer(tr)
                for (m in tr.methods) {
                    val id = MethodId(ownerFqn = fqn, name = m.name, isStatic = m.isStatic)
                    methods[id] = spm.createSmartPsiElementPointer(m)
                }
            }

            val fileFunctions = PsiTreeUtil.findChildrenOfType(psiFile, Function::class.java)
            for (fn in fileFunctions) {
                // Only global functions (not methods)
                if (fn.context is PhpClass || fn.context is PhpTrait) continue
                val fqn = runCatching { fn.fqn }.getOrNull()
                if (fqn.isNullOrBlank()) continue
                functions[fqn] = spm.createSmartPsiElementPointer(fn)
            }
        }

        return DefinitionIndex(
            classesByFqn = classes,
            traitsByFqn = traits,
            functionsByFqn = functions,
            methodsById = methods,
            fileNodes = fileNodes
        )
    }

    val Method.isStatic: Boolean
        get() = try { this.isStatic } catch (_: Throwable) { false }

    val Method.visibility: Visibility
        get() = when {
            hasModifier(PhpModifier.PRIVATE) -> Visibility.PRIVATE
            hasModifier(PhpModifier.PROTECTED) -> Visibility.PROTECTED
            else -> Visibility.PUBLIC
        }
}

enum class Visibility { PUBLIC, PROTECTED, PRIVATE }

