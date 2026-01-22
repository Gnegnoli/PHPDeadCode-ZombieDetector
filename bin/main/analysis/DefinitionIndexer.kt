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
import com.zombiedetector.psi.PsiUtils
import com.zombiedetector.psi.isStaticMethod

data class DefinitionIndex(
    val classesByFqn: Map<String, SmartPsiElementPointer<PhpClass>>,
    val traitsByFqn: Map<String, SmartPsiElementPointer<PhpClass>>, // Traits are PhpClass in PSI
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
        val traits = LinkedHashMap<String, SmartPsiElementPointer<PhpClass>>()
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
                    val id = MethodId(ownerFqn = fqn, name = m.name, isStatic = m.isStaticMethod)
                    methods[id] = spm.createSmartPsiElementPointer(m)
                }
            }

            // Traits are also PhpClass in PSI - detect by checking if it's a trait
            for (cls in fileClasses) {
                val fqn = runCatching { cls.fqn }.getOrNull()
                if (fqn.isNullOrBlank()) continue
                // Check if it's a trait (PhpClass has isTrait() or similar method)
                try {
                    if (cls.isTrait) {
                        traits[fqn] = spm.createSmartPsiElementPointer(cls)
                        for (m in cls.methods) {
                            val id = MethodId(ownerFqn = fqn, name = m.name, isStatic = m.isStaticMethod)
                            methods[id] = spm.createSmartPsiElementPointer(m)
                        }
                    }
                } catch (_: Throwable) {
                    // If isTrait doesn't exist, skip trait detection for now
                }
            }

            val fileFunctions = PsiTreeUtil.findChildrenOfType(psiFile, Function::class.java)
            for (fn in fileFunctions) {
                // Only global functions (not methods)
                if (fn.context is PhpClass) continue
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


    val Method.visibility: Visibility
        get() = try {
            when {
                modifier?.isPrivate == true -> Visibility.PRIVATE
                modifier?.isProtected == true -> Visibility.PROTECTED
                else -> Visibility.PUBLIC
            }
        } catch (_: Throwable) {
            Visibility.PUBLIC
        }
}

enum class Visibility { PUBLIC, PROTECTED, PRIVATE }

