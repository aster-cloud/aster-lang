package io.aster.idea.findUsages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import io.aster.idea.lang.AsterLexerAdapter;
import io.aster.idea.lang.AsterTokenTypes;
import io.aster.idea.psi.AsterNamedElement;
import io.aster.idea.psi.impl.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster Find Usages 提供者
 * <p>
 * 支持 Find Usages (Alt+F7) 功能，用于查找标识符的所有使用位置。
 */
public class AsterFindUsagesProvider implements FindUsagesProvider {

    @Override
    public @Nullable WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(
            new AsterLexerAdapter(),
            TokenSet.create(AsterTokenTypes.IDENT, AsterTokenTypes.TYPE_IDENT),
            TokenSet.create(AsterTokenTypes.COMMENT),
            TokenSet.create(AsterTokenTypes.STRING)
        );
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return psiElement instanceof AsterNamedElement;
    }

    @Override
    public @Nullable @NonNls String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public @Nls @NotNull String getType(@NotNull PsiElement element) {
        if (element instanceof AsterFuncDeclImpl) {
            return "函数";
        } else if (element instanceof AsterDataDeclImpl) {
            return "数据类型";
        } else if (element instanceof AsterEnumDeclImpl) {
            return "枚举";
        } else if (element instanceof AsterTypeAliasDeclImpl) {
            return "类型别名";
        } else if (element instanceof AsterLetStmtImpl) {
            return "变量";
        } else if (element instanceof AsterParameterImpl) {
            return "参数";
        } else if (element instanceof AsterFieldDefImpl) {
            return "字段";
        } else if (element instanceof AsterModuleDeclImpl) {
            return "模块";
        } else if (element instanceof AsterImportDeclImpl) {
            return "导入";
        } else if (element instanceof AsterWorkflowStmtImpl) {
            return "工作流";
        } else if (element instanceof AsterForStmtImpl) {
            return "循环变量";
        }
        return "标识符";
    }

    @Override
    public @Nls @NotNull String getDescriptiveName(@NotNull PsiElement element) {
        if (element instanceof AsterNamedElement namedElement) {
            String name = namedElement.getName();
            return name != null ? name : "";
        }
        return "";
    }

    @Override
    public @Nls @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        if (element instanceof AsterNamedElement namedElement) {
            String name = namedElement.getName();
            if (name != null) {
                String type = getType(element);
                return type + " " + name;
            }
        }
        return element.getText();
    }
}
