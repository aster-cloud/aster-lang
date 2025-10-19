package aster.cli;

import java.util.Locale;

/**
 * 负责格式化并输出诊断信息，确保 CLI 输出统一且便于机器解析。
 */
public final class DiagnosticFormatter {
  /**
   * 将诊断信息格式化为 file:line:column: level: message 样式。
   *
   * @param diagnostic TypeScript 侧返回的诊断
   * @return 格式化字符串
   */
  public String formatDiagnostic(TypeScriptBridge.Diagnostic diagnostic) {
    final String file = diagnostic.file().orElse("(unknown)");
    final int line = Math.max(0, diagnostic.line());
    final int column = Math.max(0, diagnostic.column());
    final String level = diagnostic.severity().toLowerCase(Locale.ROOT);
    return "%s:%d:%d: %s: %s".formatted(file, line, column, level, diagnostic.message());
  }

  /**
   * 根据执行结果打印诊断或默认错误信息。
   *
   * @param result 子命令执行结果
   * @param defaultMessage 当无诊断与输出时使用的默认提示
   */
  public void reportDiagnostics(TypeScriptBridge.Result result, String defaultMessage) {
    if (result.diagnostics().isEmpty()) {
      if (!result.stderr().isBlank()) {
        System.err.println(result.stderr());
      } else if (!result.stdout().isBlank()) {
        System.err.println(result.stdout());
      } else {
        System.err.println(defaultMessage);
      }
      return;
    }
    result.diagnostics().stream().map(this::formatDiagnostic).forEach(System.err::println);
  }
}
