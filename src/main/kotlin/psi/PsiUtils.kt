package com.zombiedetector.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.php.lang.psi.PhpFile

object PsiUtils {

    fun isInVendor(file: VirtualFile): Boolean {
        // Fast path: check any ancestor folder named "vendor"
        var cur: VirtualFile? = file
        while (cur != null) {
            if (cur.name.equals("vendor", ignoreCase = true)) return true
            cur = cur.parent
        }
        return false
    }

    fun isInProjectContent(project: Project, file: VirtualFile): Boolean {
        return ProjectFileIndex.getInstance(project).isInContent(file)
    }

    fun isPhpFile(psiFile: PsiFile?): Boolean = psiFile is PhpFile

    fun allProjectPhpFiles(project: Project): List<VirtualFile> {
        val scope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getAllFilesByExt(project, "php", scope)
            .asSequence()
            .filter { isInProjectContent(project, it) }
            .filterNot { isInVendor(it) }
            .toList()
    }

    fun projectBaseDir(project: Project): VirtualFile? = project.baseDir

    fun isUnder(child: VirtualFile, parent: VirtualFile): Boolean =
        VfsUtilCore.isAncestor(parent, child, true)
}

