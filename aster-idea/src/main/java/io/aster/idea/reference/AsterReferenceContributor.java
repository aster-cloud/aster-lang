package io.aster.idea.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import io.aster.idea.lang.AsterKeywords;
import io.aster.idea.lang.AsterLanguage;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 引用贡献者
 * <p>
 * 为 Aster 语言中的标识符提供引用解析，支持：
 * - Go to Definition (Ctrl+Click / Ctrl+B)
 * - Find Usages (Alt+F7)
 * - Rename Refactoring (Shift+F6)
 * <p>
 * 特别处理限定引用（如 alias.symbol）：
 * - 识别前缀别名，构建完整的限定名
 * - 使跨文件引用解析正常工作
 */
public class AsterReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // 为 IDENT 类型的 token 注册引用提供者
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withLanguage(AsterLanguage.INSTANCE)
                .withElementType(AsterTokenTypes.IDENT),
            new AsterReferenceProvider()
        );

        // 为 TYPE_IDENT 类型的 token 注册引用提供者（类型名称）
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withLanguage(AsterLanguage.INSTANCE)
                .withElementType(AsterTokenTypes.TYPE_IDENT),
            new AsterReferenceProvider()
        );
    }

    /**
     * Aster 引用提供者
     */
    private static class AsterReferenceProvider extends PsiReferenceProvider {

        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                @NotNull ProcessingContext context) {
            String text = element.getText();
            if (text == null || text.isEmpty()) {
                return PsiReference.EMPTY_ARRAY;
            }

            // 跳过关键字
            if (isKeyword(text)) {
                return PsiReference.EMPTY_ARRAY;
            }

            // 检查是否是限定表达式的一部分（前面有 alias.）
            String qualifiedPrefix = findQualifiedPrefix(element);

            // 创建引用，传入限定前缀
            TextRange range = new TextRange(0, text.length());
            return new PsiReference[]{new AsterReference(element, range, qualifiedPrefix)};
        }

        /**
         * 查找限定前缀
         * <p>
         * 向前遍历兄弟元素，识别完整的限定路径。
         * 使用 PsiTreeUtil.skipWhitespacesAndCommentsBackward 跳过空白和注释。
         * <p>
         * 示例：
         * <ul>
         *   <li>"math.core.add" 中的 "add" 返回 "math.core"</li>
         *   <li>"A.fetch_data" 中的 "fetch_data" 返回 "A"</li>
         *   <li>带注释的表达式也能正确处理，返回 "math.core"</li>
         * </ul>
         *
         * @param element 当前元素
         * @return 限定前缀（完整路径），如果不是限定引用则返回 null
         */
        private @Nullable String findQualifiedPrefix(@NotNull PsiElement element) {
            StringBuilder prefixBuilder = new StringBuilder();
            PsiElement current = element;

            while (true) {
                // 使用 PsiTreeUtil 跳过空白和注释
                PsiElement prev = PsiTreeUtil.skipWhitespacesAndCommentsBackward(current);

                // 检查是否是点号
                if (prev == null || !".".equals(prev.getText())) {
                    break;
                }

                // 跳过点号前面的空白和注释
                PsiElement beforeDot = PsiTreeUtil.skipWhitespacesAndCommentsBackward(prev);

                // 检查点号前面是否是标识符
                if (beforeDot == null || !isIdentifier(beforeDot)) {
                    break;
                }

                // 在前面添加这一段
                if (prefixBuilder.length() > 0) {
                    prefixBuilder.insert(0, ".");
                }
                prefixBuilder.insert(0, beforeDot.getText());

                // 继续向前查找
                current = beforeDot;
            }

            return prefixBuilder.length() > 0 ? prefixBuilder.toString() : null;
        }

        /**
         * 判断元素是否为标识符
         */
        private boolean isIdentifier(@NotNull PsiElement element) {
            IElementType type = element.getNode().getElementType();
            return type == AsterTokenTypes.IDENT || type == AsterTokenTypes.TYPE_IDENT;
        }

        /**
         * 判断是否为关键字（使用统一定义）
         */
        private boolean isKeyword(String text) {
            return AsterKeywords.isKeyword(text);
        }
    }
}
