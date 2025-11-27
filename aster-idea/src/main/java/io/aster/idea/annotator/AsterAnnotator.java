package io.aster.idea.annotator;

import aster.core.ast.Module;
import aster.core.ir.CoreModel;
import aster.core.lowering.CoreLowering;
import aster.core.parser.AsterCustomLexer;
import aster.core.parser.AsterParser;
import aster.core.parser.AstBuilder;
import aster.core.typecheck.TypeChecker;
import aster.core.typecheck.model.Diagnostic;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import io.aster.idea.psi.AsterFile;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Aster 类型检查注解器
 * <p>
 * 使用 aster-core 的 TypeChecker 对 Aster 源代码进行类型检查，
 * 并将诊断信息显示在编辑器中。
 */
public class AsterAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // 仅在文件级别进行类型检查
        if (!(element instanceof AsterFile asterFile)) {
            return;
        }

        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) {
            return;
        }

        String source = psiFile.getText();
        if (source == null || source.isEmpty()) {
            return;
        }

        try {
            // 编译并获取诊断信息
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // 获取文档以进行偏移量计算
            Document document = PsiDocumentManager.getInstance(psiFile.getProject())
                .getDocument(psiFile);
            if (document == null) {
                return;
            }

            // 将诊断信息转换为注解
            for (Diagnostic diagnostic : diagnostics) {
                createAnnotation(holder, document, diagnostic);
            }
        } catch (Exception e) {
            // 编译或类型检查失败时静默处理
            // 词法或语法错误会由高亮器处理
        }
    }

    /**
     * 编译源代码并进行类型检查
     */
    private List<Diagnostic> compileAndCheck(String source) {
        // 词法分析
        AsterCustomLexer lexer = new AsterCustomLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // 语法分析
        AsterParser parser = new AsterParser(tokens);
        AsterParser.ModuleContext parseTree = parser.module();

        // 构建 AST
        AstBuilder astBuilder = new AstBuilder();
        Module ast = astBuilder.visitModule(parseTree);
        if (ast == null) {
            return List.of();
        }

        // 降级为 Core IR
        CoreLowering lowering = new CoreLowering();
        CoreModel.Module module = lowering.lowerModule(ast);
        if (module == null) {
            return List.of();
        }

        // 进行类型检查
        TypeChecker typeChecker = new TypeChecker();
        return typeChecker.typecheckModule(module);
    }

    /**
     * 将诊断信息转换为编辑器注解
     */
    private void createAnnotation(
        AnnotationHolder holder,
        Document document,
        Diagnostic diagnostic
    ) {
        // 计算文本范围
        TextRange range = getTextRange(document, diagnostic);
        if (range == null || range.isEmpty()) {
            return;
        }

        // 转换严重级别
        HighlightSeverity severity = switch (diagnostic.severity()) {
            case ERROR -> HighlightSeverity.ERROR;
            case WARNING -> HighlightSeverity.WARNING;
            case INFO -> HighlightSeverity.INFORMATION;
        };

        // 构建消息
        String message = diagnostic.message();
        diagnostic.help().ifPresent(help -> {
            // 附加提示信息（如果有）
        });

        // 创建注解
        holder.newAnnotation(severity, message)
            .range(range)
            .create();
    }

    /**
     * 将诊断信息的位置转换为文档偏移量范围
     */
    private TextRange getTextRange(Document document, Diagnostic diagnostic) {
        return diagnostic.span().map(origin -> {
            try {
                // 转换行列为偏移量
                int startLine = origin.start.line - 1; // 1-based to 0-based
                int startCol = origin.start.col - 1;
                int endLine = origin.end.line - 1;
                int endCol = origin.end.col - 1;

                // 边界检查
                if (startLine < 0 || startLine >= document.getLineCount()) {
                    return null;
                }
                if (endLine < 0 || endLine >= document.getLineCount()) {
                    endLine = startLine;
                    endCol = document.getLineEndOffset(startLine) - document.getLineStartOffset(startLine);
                }

                int startOffset = document.getLineStartOffset(startLine) + startCol;
                int endOffset = document.getLineStartOffset(endLine) + endCol;

                // 确保范围有效
                int docLength = document.getTextLength();
                startOffset = Math.max(0, Math.min(startOffset, docLength));
                endOffset = Math.max(startOffset, Math.min(endOffset, docLength));

                if (startOffset == endOffset) {
                    // 至少高亮一个字符
                    endOffset = Math.min(endOffset + 1, docLength);
                }

                return new TextRange(startOffset, endOffset);
            } catch (Exception e) {
                return null;
            }
        }).orElse(null);
    }
}
