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
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import io.aster.idea.psi.AsterFile;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Aster 外部注解器（ExternalAnnotator）
 * <p>
 * 使用 aster-core 的 TypeChecker 对 Aster 源代码进行类型检查。
 * 与普通 Annotator 不同，ExternalAnnotator 在后台线程运行，不阻塞 UI。
 * <p>
 * 三阶段执行模型：
 * 1. collectInformation - 在读操作中快速收集信息
 * 2. doAnnotate - 在后台线程中执行类型检查（可取消）
 * 3. apply - 将结果应用到编辑器（在 UI 线程）
 */
public class AsterExternalAnnotator extends ExternalAnnotator<AsterExternalAnnotator.CollectedInfo, AsterExternalAnnotator.AnnotationResult> {

    /**
     * 收集阶段的信息
     */
    public record CollectedInfo(String source, Document document) {}

    /**
     * 注解结果
     */
    public record AnnotationResult(List<Diagnostic> diagnostics, Document document) {}

    @Override
    public @Nullable CollectedInfo collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        if (!(file instanceof AsterFile)) {
            return null;
        }

        String source = file.getText();
        if (source == null || source.isEmpty()) {
            return null;
        }

        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return null;
        }

        return new CollectedInfo(source, document);
    }

    @Override
    public @Nullable CollectedInfo collectInformation(@NotNull PsiFile file) {
        if (!(file instanceof AsterFile)) {
            return null;
        }

        String source = file.getText();
        if (source == null || source.isEmpty()) {
            return null;
        }

        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return null;
        }

        return new CollectedInfo(source, document);
    }

    @Override
    public @Nullable AnnotationResult doAnnotate(CollectedInfo info) {
        if (info == null) {
            return null;
        }

        try {
            // 检查是否被取消
            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            if (indicator != null && indicator.isCanceled()) {
                return null;
            }

            // 在后台执行类型检查
            List<Diagnostic> diagnostics = compileAndCheck(info.source(), indicator);
            return new AnnotationResult(diagnostics, info.document());
        } catch (Exception e) {
            // 编译或类型检查失败时返回空结果
            return new AnnotationResult(Collections.emptyList(), info.document());
        }
    }

    @Override
    public void apply(@NotNull PsiFile file, AnnotationResult result, @NotNull AnnotationHolder holder) {
        if (result == null || result.diagnostics().isEmpty()) {
            return;
        }

        for (Diagnostic diagnostic : result.diagnostics()) {
            createAnnotation(holder, result.document(), diagnostic);
        }
    }

    /**
     * 编译源代码并进行类型检查（支持取消）
     */
    private List<Diagnostic> compileAndCheck(String source, @Nullable ProgressIndicator indicator) {
        // 词法分析
        if (indicator != null && indicator.isCanceled()) return Collections.emptyList();
        AsterCustomLexer lexer = new AsterCustomLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // 语法分析
        if (indicator != null && indicator.isCanceled()) return Collections.emptyList();
        AsterParser parser = new AsterParser(tokens);
        AsterParser.ModuleContext parseTree = parser.module();

        // 构建 AST
        if (indicator != null && indicator.isCanceled()) return Collections.emptyList();
        AstBuilder astBuilder = new AstBuilder();
        Module ast = astBuilder.visitModule(parseTree);
        if (ast == null) {
            return Collections.emptyList();
        }

        // 降级为 Core IR
        if (indicator != null && indicator.isCanceled()) return Collections.emptyList();
        CoreLowering lowering = new CoreLowering();
        CoreModel.Module module = lowering.lowerModule(ast);
        if (module == null) {
            return Collections.emptyList();
        }

        // 进行类型检查
        if (indicator != null && indicator.isCanceled()) return Collections.emptyList();
        TypeChecker typeChecker = new TypeChecker();
        return typeChecker.typecheckModule(module);
    }

    /**
     * 将诊断信息转换为编辑器注解
     */
    private void createAnnotation(AnnotationHolder holder, Document document, Diagnostic diagnostic) {
        TextRange range = getTextRange(document, diagnostic);
        if (range == null || range.isEmpty()) {
            return;
        }

        HighlightSeverity severity = switch (diagnostic.severity()) {
            case ERROR -> HighlightSeverity.ERROR;
            case WARNING -> HighlightSeverity.WARNING;
            case INFO -> HighlightSeverity.INFORMATION;
        };

        // 构建消息，包含帮助信息
        String message = diagnostic.message();
        String help = diagnostic.help().orElse(null);
        if (help != null && !help.isEmpty()) {
            message = message + "\n\n提示: " + help;
        }

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
                int startLine = origin.start.line - 1;
                int startCol = origin.start.col - 1;
                int endLine = origin.end.line - 1;
                int endCol = origin.end.col - 1;

                if (startLine < 0 || startLine >= document.getLineCount()) {
                    return null;
                }
                if (endLine < 0 || endLine >= document.getLineCount()) {
                    endLine = startLine;
                    endCol = document.getLineEndOffset(startLine) - document.getLineStartOffset(startLine);
                }

                int startOffset = document.getLineStartOffset(startLine) + startCol;
                int endOffset = document.getLineStartOffset(endLine) + endCol;

                int docLength = document.getTextLength();
                startOffset = Math.max(0, Math.min(startOffset, docLength));
                endOffset = Math.max(startOffset, Math.min(endOffset, docLength));

                if (startOffset == endOffset) {
                    endOffset = Math.min(endOffset + 1, docLength);
                }

                return new TextRange(startOffset, endOffset);
            } catch (Exception e) {
                return null;
            }
        }).orElse(null);
    }
}
