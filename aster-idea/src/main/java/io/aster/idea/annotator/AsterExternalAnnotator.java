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
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import io.aster.idea.psi.AsterFile;
import io.aster.idea.reference.AsterModuleResolver;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>
 * 性能优化：
 * - 使用模块级缓存，仅当当前文件或导入模块被修改时才重新进行类型检查
 * - 缓存基于文件修改戳，自动失效
 */
public class AsterExternalAnnotator extends ExternalAnnotator<AsterExternalAnnotator.CollectedInfo, AsterExternalAnnotator.AnnotationResult> {

    /**
     * 诊断缓存，以文件路径为 key
     * <p>
     * 使用 ConcurrentHashMap 保证线程安全，因为 doAnnotate 在后台线程执行
     */
    private static final Map<String, CachedDiagnostics> DIAGNOSTICS_CACHE = new ConcurrentHashMap<>();

    /**
     * 缓存的诊断数据
     * <p>
     * 只保存不可变数据，避免持有 Document 等 UI 资源导致内存泄漏
     *
     * @param diagnostics 诊断结果列表（不可变）
     * @param lineOffset 行偏移量
     * @param sourceStamp 当前文件的修改戳
     * @param importedModuleStamps 导入模块的修改戳映射（模块路径 -> 修改戳）
     */
    private record CachedDiagnostics(
        List<Diagnostic> diagnostics,
        int lineOffset,
        long sourceStamp,
        Map<String, Long> importedModuleStamps
    ) {}

    /**
     * 收集阶段的信息
     * <p>
     * 包含文档修改戳和导入模块的修改戳，用于缓存验证
     */
    public record CollectedInfo(
        String source,
        Document document,
        AsterFile asterFile,
        Project project,
        long modificationStamp,
        Map<String, Long> importedModuleStamps
    ) {}

    /**
     * 注解结果
     * <p>
     * 保留修改戳以便在 apply 阶段校验
     */
    public record AnnotationResult(List<Diagnostic> diagnostics, Document document, int lineOffset,
                                    long modificationStamp) {}

    @Override
    public @Nullable CollectedInfo collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return doCollectInformation(file);
    }

    @Override
    public @Nullable CollectedInfo collectInformation(@NotNull PsiFile file) {
        return doCollectInformation(file);
    }

    /**
     * 统一的信息收集逻辑
     * <p>
     * 在 ReadAction 中收集当前文件和所有导入模块的修改戳，用于后续缓存验证
     */
    private @Nullable CollectedInfo doCollectInformation(@NotNull PsiFile file) {
        if (!(file instanceof AsterFile asterFile)) {
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

        // 记录文档修改戳
        long modificationStamp = document.getModificationStamp();

        // 收集导入模块的修改戳（在 ReadAction 中执行）
        Map<String, Long> importedModuleStamps = collectImportedModuleStamps(asterFile);

        return new CollectedInfo(source, document, asterFile, file.getProject(),
                                 modificationStamp, importedModuleStamps);
    }

    /**
     * 收集导入模块的修改戳
     * <p>
     * 在 ReadAction 中遍历所有导入的模块，获取其修改戳
     *
     * @throws ProcessCanceledException 如果操作被取消，必须向上传播
     */
    private Map<String, Long> collectImportedModuleStamps(AsterFile asterFile) {
        Map<String, Long> stamps = new HashMap<>();
        try {
            AsterModuleResolver resolver = AsterModuleResolver.getInstance(asterFile.getProject());
            Map<String, AsterModuleResolver.ImportInfo> imports = resolver.collectImports(asterFile);

            Set<String> processedModules = new HashSet<>();
            for (AsterModuleResolver.ImportInfo importInfo : imports.values()) {
                // 计算导入类型
                AsterModuleResolver.ImportInfo.ImportTypeResult typeResult = importInfo.computeImportType(resolver);

                // 确定要加载的模块路径
                String modulePathToLoad;
                if (typeResult.type == AsterModuleResolver.ImportInfo.ImportType.SYMBOL) {
                    if (typeResult.parentModulePath == null) {
                        continue;
                    }
                    modulePathToLoad = typeResult.parentModulePath;
                } else {
                    modulePathToLoad = importInfo.modulePath;
                }

                // 避免重复处理
                if (processedModules.contains(modulePathToLoad)) {
                    continue;
                }
                processedModules.add(modulePathToLoad);

                // 获取模块文件的修改戳
                AsterFile moduleFile = resolver.resolveModule(modulePathToLoad);
                if (moduleFile != null) {
                    stamps.put(modulePathToLoad, moduleFile.getModificationStamp());
                }
            }
        } catch (ProcessCanceledException e) {
            // 必须向上传播取消异常，遵循 IntelliJ 取消契约
            throw e;
        } catch (Exception e) {
            // 其他异常：返回当前已收集的戳，避免完全失败
            // 注意：返回部分结果可能导致缓存不完整，但比返回空映射更安全
        }
        return stamps;
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

            // 获取缓存 key，可能为 null（临时 PSI 或注入片段）
            String cacheKey = getCacheKey(info);

            // 仅当 cacheKey 有效时才尝试从缓存获取
            if (cacheKey != null) {
                CachedDiagnostics cached = DIAGNOSTICS_CACHE.get(cacheKey);

                if (cached != null && isCacheValid(cached, info)) {
                    // 缓存有效，直接返回（使用当前的 document 和修改戳）
                    return new AnnotationResult(
                        cached.diagnostics(),
                        info.document(),
                        cached.lineOffset(),
                        info.modificationStamp()
                    );
                }
            }

            // 缓存无效、不存在或无法缓存，执行类型检查
            AnnotationResult result = performTypeCheck(info, indicator);

            // 仅当 cacheKey 有效时才更新缓存
            if (result != null && cacheKey != null) {
                DIAGNOSTICS_CACHE.put(cacheKey, new CachedDiagnostics(
                    List.copyOf(result.diagnostics()), // 保存不可变副本
                    result.lineOffset(),
                    info.modificationStamp(),
                    Map.copyOf(info.importedModuleStamps())
                ));
            }

            return result;
        } catch (ProcessCanceledException e) {
            // 必须重新抛出取消异常，遵循 IntelliJ 取消契约
            throw e;
        } catch (Exception e) {
            // 编译或类型检查失败时返回空结果
            return new AnnotationResult(Collections.emptyList(), info.document(), 0, info.modificationStamp());
        }
    }

    /**
     * 获取缓存 key（使用文件路径）
     */
    private @Nullable String getCacheKey(CollectedInfo info) {
        return ReadAction.compute(() -> {
            if (info.asterFile().getVirtualFile() != null) {
                return info.asterFile().getVirtualFile().getPath();
            }
            return null;
        });
    }

    /**
     * 检查缓存是否有效
     * <p>
     * 缓存有效条件：
     * 1. 当前文件的修改戳匹配
     * 2. 所有导入模块的修改戳都匹配
     */
    private boolean isCacheValid(CachedDiagnostics cached, CollectedInfo info) {
        // 检查当前文件修改戳
        if (cached.sourceStamp() != info.modificationStamp()) {
            return false;
        }

        // 检查导入模块数量是否一致
        if (cached.importedModuleStamps().size() != info.importedModuleStamps().size()) {
            return false;
        }

        // 检查每个导入模块的修改戳
        for (Map.Entry<String, Long> entry : info.importedModuleStamps().entrySet()) {
            Long cachedStamp = cached.importedModuleStamps().get(entry.getKey());
            if (cachedStamp == null || !cachedStamp.equals(entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 执行类型检查
     */
    private @Nullable AnnotationResult performTypeCheck(CollectedInfo info, @Nullable ProgressIndicator indicator) {
        // 收集导入模块的源代码（跨文件分析）
        String combinedSource = collectImportedModuleSources(info, indicator);

        // 检查取消状态
        ProgressManager.checkCanceled();

        // 计算当前文件在合并源代码中的行偏移
        int lineOffset = countPrefixLines(combinedSource, info.source());

        // 在后台执行类型检查
        List<Diagnostic> diagnostics = compileAndCheck(combinedSource, indicator);

        return new AnnotationResult(diagnostics, info.document(), lineOffset, info.modificationStamp());
    }

    /**
     * 收集导入模块的源代码，与当前文件合并进行跨文件类型检查
     * <p>
     * 支持两种导入类型：
     * - 模块导入：直接加载模块文件
     * - 符号导入：加载符号所在的父模块文件
     * <p>
     * 注意：PSI 访问必须在 ReadAction 中执行，以确保线程安全
     */
    private String collectImportedModuleSources(CollectedInfo info, @Nullable ProgressIndicator indicator) {
        StringBuilder combined = new StringBuilder();

        try {
            // 在 ReadAction 中执行所有 PSI 访问操作
            List<String> importedSources = ReadAction.compute(() -> {
                // 在长循环前检查取消状态
                ProgressManager.checkCanceled();

                List<String> sources = new ArrayList<>();
                AsterModuleResolver resolver = AsterModuleResolver.getInstance(info.project());
                Map<String, AsterModuleResolver.ImportInfo> imports = resolver.collectImports(info.asterFile());

                // 收集导入模块的源代码
                Set<String> processedModules = new HashSet<>();
                for (AsterModuleResolver.ImportInfo importInfo : imports.values()) {
                    // 在循环中定期检查取消状态
                    ProgressManager.checkCanceled();

                    // 计算一次导入类型并缓存结果，避免重复解析
                    AsterModuleResolver.ImportInfo.ImportTypeResult typeResult = importInfo.computeImportType(resolver);

                    // 确定要加载的模块路径
                    String modulePathToLoad;
                    if (typeResult.type == AsterModuleResolver.ImportInfo.ImportType.SYMBOL) {
                        // 符号导入：加载父模块
                        if (typeResult.parentModulePath == null) {
                            continue;
                        }
                        modulePathToLoad = typeResult.parentModulePath;
                    } else {
                        // 模块导入：直接使用模块路径
                        modulePathToLoad = importInfo.modulePath;
                    }

                    // 避免重复处理同一模块
                    if (processedModules.contains(modulePathToLoad)) {
                        continue;
                    }
                    processedModules.add(modulePathToLoad);

                    // 解析模块文件
                    AsterFile moduleFile = resolver.resolveModule(modulePathToLoad);
                    if (moduleFile != null) {
                        String moduleSource = moduleFile.getText();
                        if (moduleSource != null && !moduleSource.isEmpty()) {
                            sources.add("# Imported from: " + modulePathToLoad + "\n" + moduleSource + "\n\n");
                        }
                    }
                }
                return sources;
            });

            // 添加导入模块的源代码
            for (String importedSource : importedSources) {
                // 检查取消状态
                ProgressManager.checkCanceled();
                combined.append(importedSource);
            }
        } catch (ProcessCanceledException e) {
            // 必须重新抛出取消异常
            throw e;
        } catch (Exception e) {
            // 导入解析失败时继续使用当前文件源码
        }

        // 添加当前文件的源代码
        combined.append("# Current file\n");
        combined.append(info.source());

        return combined.toString();
    }

    /**
     * 计算当前文件在合并源代码中的起始行偏移
     */
    private int countPrefixLines(String combinedSource, String currentSource) {
        int currentIndex = combinedSource.lastIndexOf(currentSource);
        if (currentIndex <= 0) {
            return 0;
        }
        String prefix = combinedSource.substring(0, currentIndex);
        return (int) prefix.chars().filter(ch -> ch == '\n').count();
    }

    @Override
    public void apply(@NotNull PsiFile file, AnnotationResult result, @NotNull AnnotationHolder holder) {
        if (result == null || result.diagnostics().isEmpty()) {
            return;
        }

        // 校验文档修改戳，如果文档已被修改则丢弃过期的诊断结果
        // 这防止将旧的诊断信息套用到新编辑的文本上
        long currentStamp = result.document().getModificationStamp();
        if (currentStamp != result.modificationStamp()) {
            // 文档已被修改，丢弃结果，等待下一轮注解
            return;
        }

        int lineOffset = result.lineOffset();
        int documentLines = result.document().getLineCount();

        for (Diagnostic diagnostic : result.diagnostics()) {
            // 过滤并调整诊断位置，只显示当前文件中的
            createAnnotation(holder, result.document(), diagnostic, lineOffset, documentLines);
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
     * <p>
     * 过滤掉来自导入模块的诊断，并调整当前文件诊断的行号
     *
     * @param lineOffset 当前文件在合并源码中的起始行偏移
     * @param documentLines 当前文档的行数
     */
    private void createAnnotation(AnnotationHolder holder, Document document, Diagnostic diagnostic,
                                  int lineOffset, int documentLines) {
        // 检查诊断是否在当前文件范围内
        if (!isDiagnosticInCurrentFile(diagnostic, lineOffset, documentLines)) {
            return;
        }

        TextRange range = getTextRange(document, diagnostic, lineOffset);
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
     * 判断诊断是否属于当前文件
     */
    private boolean isDiagnosticInCurrentFile(Diagnostic diagnostic, int lineOffset, int documentLines) {
        return diagnostic.span().map(span -> {
            int startLine = span.start.line;
            int adjustedLine = startLine - lineOffset;
            // 诊断行号在调整后必须在当前文件范围内 (1-based)
            return adjustedLine >= 1 && adjustedLine <= documentLines;
        }).orElse(true); // 没有位置信息的诊断默认包含
    }

    /**
     * 将诊断信息的位置转换为文档偏移量范围
     * <p>
     * 对于没有位置信息的诊断（如文件级错误），返回文件第一行的范围，
     * 确保所有诊断都能显示在编辑器中。
     *
     * @param lineOffset 当前文件在合并源码中的起始行偏移，用于调整行号
     */
    private TextRange getTextRange(Document document, Diagnostic diagnostic, int lineOffset) {
        return diagnostic.span().map(origin -> {
            try {
                // 调整行号：减去导入模块占用的行数
                int startLine = origin.start.line - 1 - lineOffset;
                int startCol = origin.start.col - 1;
                int endLine = origin.end.line - 1 - lineOffset;
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
        }).orElseGet(() -> getFileLevelFallbackRange(document));
    }

    /**
     * 获取文件级诊断的回退范围
     * <p>
     * 对于没有具体位置信息的诊断（如能力声明缺失），
     * 返回文件第一行的范围，确保诊断能显示在编辑器中。
     */
    private @Nullable TextRange getFileLevelFallbackRange(Document document) {
        int docLength = document.getTextLength();
        if (docLength == 0) {
            // 空文件，返回 (0,0) 的范围，但这会被过滤掉
            // 对于空文件，没有合理的位置可以显示诊断
            return null;
        }

        // 返回第一行的范围
        if (document.getLineCount() > 0) {
            int firstLineEnd = document.getLineEndOffset(0);
            // 确保至少有一个字符的范围
            return new TextRange(0, Math.max(1, firstLineEnd));
        }

        // 回退：返回文件开头的一个字符
        return new TextRange(0, 1);
    }
}
