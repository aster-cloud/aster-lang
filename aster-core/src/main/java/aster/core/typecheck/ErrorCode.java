// 本文件由 scripts/generate_error_codes.ts 自动生成，请勿手动修改。
// 源数据: shared/error_codes.json

package aster.core.typecheck;

import java.util.Locale;

/**
 * 错误码与消息模板的枚举定义，由共享 JSON 自动生成，确保 Java 与 TypeScript 行为一致。
 */
public enum ErrorCode {
  TYPE_MISMATCH("E001", Category.TYPE, Severity.ERROR, "Type mismatch: expected %s, got %s", "检查类型标注与表达式的推断结果是否一致。"),
  TYPE_MISMATCH_ASSIGN("E002", Category.TYPE, Severity.ERROR, "Type mismatch assigning to '%s': %s vs %s", "确认变量先前绑定的类型与当前赋值结果一致。"),
  RETURN_TYPE_MISMATCH("E003", Category.TYPE, Severity.ERROR, "Return type mismatch: expected %s, got %s", "检查函数返回语句与声明的返回类型是否一致。"),
  TYPE_VAR_UNDECLARED("E004", Category.TYPE, Severity.ERROR, "Type variable '%s' is used in '%s' but not declared in its type parameters.", "在函数签名的 of 子句中显式声明使用到的类型变量。"),
  TYPE_PARAM_UNUSED("E005", Category.TYPE, Severity.WARNING, "Type parameter '%s' on '%s' is declared but not used.", "移除未使用的类型参数，避免造成误导。"),
  TYPEVAR_LIKE_UNDECLARED("E006", Category.TYPE, Severity.ERROR, "Type variable-like '%s' is used in '%s' but not declared; declare it with 'of %s'.", "对于看起来像类型变量的名称，务必在 of 子句中声明。"),
  TYPEVAR_INCONSISTENT("E007", Category.TYPE, Severity.ERROR, "Type variable '%s' inferred inconsistently: %s vs %s", "确认类型推断的多个使用点产出相同的具体类型。"),
  IF_BRANCH_MISMATCH("E008", Category.TYPE, Severity.ERROR, "If分支返回类型不一致: then分支 %s vs else分支 %s", "确保 if 两个分支返回类型保持一致。"),
  MATCH_BRANCH_MISMATCH("E009", Category.TYPE, Severity.ERROR, "Match case return types differ: %s vs %s", "检查 match 每个分支的返回类型是否统一。"),
  INTEGER_PATTERN_TYPE("E010", Category.TYPE, Severity.ERROR, "Integer pattern used on non-Int scrutinee (%s)", "仅在 Int 类型的匹配表达式中使用整数模式。"),
  UNKNOWN_FIELD("E011", Category.TYPE, Severity.ERROR, "Unknown field '%s' for %s", "检查构造体或数据类型的字段名称是否正确。"),
  FIELD_TYPE_MISMATCH("E012", Category.TYPE, Severity.ERROR, "Field '%s' expects %s, got %s", "校验字段初始化表达式的类型是否匹配声明。"),
  MISSING_REQUIRED_FIELD("E013", Category.TYPE, Severity.ERROR, "构造 %s 缺少必需字段 '%s'", "为数据构造提供声明中的所有必需字段。"),
  NOT_CALL_ARITY("E014", Category.TYPE, Severity.ERROR, "not(...) expects 1 argument", "调整 not 调用的参数数量为 1。"),
  AWAIT_TYPE("E015", Category.TYPE, Severity.WARNING, "await expects Maybe<T> or Result<T,E>, got %s", "仅对 Maybe 或 Result 类型调用 await。"),
  DUPLICATE_ENUM_CASE("E016", Category.TYPE, Severity.WARNING, "Duplicate enum case '%s' in match on %s.", "移除重复的枚举分支，保持匹配语句简洁。"),
  NON_EXHAUSTIVE_MAYBE("E017", Category.TYPE, Severity.WARNING, "Non-exhaustive match on Maybe type; missing %s case.", "为 Maybe 匹配补齐 null 与非 null 分支。"),
  NON_EXHAUSTIVE_ENUM("E018", Category.TYPE, Severity.WARNING, "Non-exhaustive match on %s; missing: %s", "补充所有未覆盖的枚举分支，或添加通配符。"),
  AMBIGUOUS_INTEROP_NUMERIC("E019", Category.TYPE, Severity.WARNING, "Ambiguous interop call '%s': mixing numeric kinds (Int=%s, Long=%s, Double=%s). Overload resolution may widen/box implicitly.", "统一互操作调用的参数数值类型，避免隐式装箱与拓宽。"),
  LIST_ELEMENT_TYPE_MISMATCH("E020", Category.TYPE, Severity.ERROR, "List literal element type mismatch: expected %s, got %s", "确保列表字面量中的所有元素类型一致。"),
  OPTIONAL_EXPECTED("E021", Category.TYPE, Severity.ERROR, "Optional value required here: expected Maybe or Option, but got %s", "传入 Maybe/Option 类型或显式包装值。"),
  WORKFLOW_COMPENSATE_TYPE("E022", Category.TYPE, Severity.ERROR, "Compensate block for step '%s' must return Result<Unit, %s>, got %s", "确保补偿块返回 Result<Unit, E>，其中 E 为 step 错误类型。"),
  WORKFLOW_COMPENSATE_MISSING("E023", Category.EFFECT, Severity.WARNING, "Step '%s' performs side effects but does not define a compensate block.", "为包含 IO 副作用的 step 提供 compensate 块以便回滚。"),
  WORKFLOW_RETRY_INVALID("E024", Category.TYPE, Severity.ERROR, "Workflow retry max attempts must be greater than zero (actual: %s).", "设置 retry.maxAttempts 为正整数。"),
  WORKFLOW_TIMEOUT_INVALID("E025", Category.TYPE, Severity.ERROR, "Workflow timeout must be greater than zero milliseconds (actual: %s).", "配置 timeout 秒数为正值，确保补偿逻辑可被触发。"),
  WORKFLOW_MISSING_IO_EFFECT("E026", Category.EFFECT, Severity.ERROR, "Workflow '%s' must declare @io effect before using a 'workflow' block.", "在函数 '{func}' 的头部添加 `It performs io ...`（可同时声明 capability），否则编译器拒绝 workflow 语句。"),
  WORKFLOW_UNDECLARED_CAPABILITY("E027", Category.CAPABILITY, Severity.ERROR, "Workflow '%s' step '%s' uses capability %s that is not declared on the function header.", "在 `It performs io with ...` 中列出 {capability}（例如 Http、Sql、Secrets），或调整 step 代码避免调用未授权能力。"),
  COMPENSATE_NEW_CAPABILITY("E028", Category.CAPABILITY, Severity.ERROR, "Compensate block for step '%s' in function '%s' introduces new capability %s that does not appear in the main step body.", "Compensate 只能重复主体已使用的能力；如需额外调用，请将相同行为移至主体或在主体中声明该 capability。"),
  DUPLICATE_IMPORT_ALIAS("E100", Category.SCOPE, Severity.WARNING, "Duplicate import alias '%s'.", "为不同的导入使用唯一别名，避免覆盖。"),
  UNDEFINED_VARIABLE("E101", Category.SCOPE, Severity.ERROR, "Undefined variable: %s", "在使用变量前先声明并初始化。"),
  EFF_MISSING_IO("E200", Category.EFFECT, Severity.ERROR, "Function '%s' may perform I/O but is missing @io effect.", "为具有 IO 行为的函数声明 @io 效果。"),
  EFF_MISSING_CPU("E201", Category.EFFECT, Severity.ERROR, "Function '%s' may perform CPU-bound work but is missing @cpu (or @io) effect.", "为 CPU 密集型函数声明 @cpu 或 @io 效果。"),
  EFF_SUPERFLUOUS_IO_CPU_ONLY("E202", Category.EFFECT, Severity.INFO, "Function '%s' declares @io but only CPU-like work found; @io subsumes @cpu and may be unnecessary.", "若函数仅执行 CPU 工作，可移除多余的 @io 声明。"),
  EFF_SUPERFLUOUS_IO("E203", Category.EFFECT, Severity.WARNING, "Function '%s' declares @io but no obvious I/O found.", "确认是否需要 @io；若无 IO 行为可移除。"),
  EFF_SUPERFLUOUS_CPU("E204", Category.EFFECT, Severity.WARNING, "Function '%s' declares @cpu but no obvious CPU-bound work found.", "移除多余的 @cpu 声明或增加相应的 CPU 工作。"),
  EFF_INFER_MISSING_IO("E205", Category.EFFECT, Severity.ERROR, "函数 '%s' 缺少 @io 效果声明，推断要求 IO。", "根据推断结果为函数添加 @io 效果。"),
  EFF_INFER_MISSING_CPU("E206", Category.EFFECT, Severity.ERROR, "函数 '%s' 缺少 @cpu 效果声明，推断要求 CPU（或 @io）。", "根据推断结果补齐 @cpu 或 @io 效果。"),
  EFF_INFER_REDUNDANT_IO("E207", Category.EFFECT, Severity.WARNING, "函数 '%s' 声明了 @io，但推断未发现 IO 副作用。", "确认是否需要保留 @io 声明。"),
  EFF_INFER_REDUNDANT_CPU("E208", Category.EFFECT, Severity.WARNING, "函数 '%s' 声明了 @cpu，但推断未发现 CPU 副作用。", "若无 CPU 副作用，可删除 @cpu 声明。"),
  EFF_INFER_REDUNDANT_CPU_WITH_IO("E209", Category.EFFECT, Severity.WARNING, "函数 '%s' 同时声明 @cpu 和 @io；由于需要 @io，@cpu 可移除。", "保留 @io 即可满足需求，移除多余的 @cpu。"),
  CAPABILITY_NOT_ALLOWED("E300", Category.CAPABILITY, Severity.ERROR, "Function '%s' requires %s capability but manifest for module '%s' denies it.", "更新能力清单或修改函数实现以符合限制。"),
  EFF_CAP_MISSING("E301", Category.CAPABILITY, Severity.ERROR, "Function '%s' uses %s capability but header declares [%s].", "在函数头部声明实际使用到的能力。"),
  EFF_CAP_SUPERFLUOUS("E302", Category.CAPABILITY, Severity.INFO, "Function '%s' declares %s capability but it is not used.", "移除未使用的能力声明以保持清晰。"),
  PII_HTTP_UNENCRYPTED("E400", Category.PII, Severity.ERROR, "PII data transmitted over HTTP without encryption", "使用加密通道（HTTPS）或脱敏处理后再传输 PII 数据。"),
  PII_ANNOTATION_MISSING("E401", Category.PII, Severity.ERROR, "PII annotation missing for value flowing into '%s'", "为敏感数据添加 @pii 标注以便跟踪。"),
  PII_SENSITIVITY_MISMATCH("E402", Category.PII, Severity.WARNING, "PII sensitivity mismatch: required %s, got %s", "调整数据的敏感级别或更新流程要求。"),
  ASYNC_START_NOT_WAITED("E500", Category.ASYNC, Severity.ERROR, "Started async task '%s' not waited", "对启动的异步任务调用 wait，确保执行完毕。"),
  ASYNC_WAIT_NOT_STARTED("E501", Category.ASYNC, Severity.ERROR, "Waiting for async task '%s' that was never started", "确认 wait 的任务名称在 Start 中正确出现。"),
  ASYNC_DUPLICATE_START("E502", Category.ASYNC, Severity.ERROR, "Async task '%s' started multiple times (%s occurrences)", "避免重复启动同名任务，可复用已有任务或改用新名称。"),
  ASYNC_DUPLICATE_WAIT("E503", Category.ASYNC, Severity.WARNING, "Async task '%s' waited multiple times (%s occurrences)", "确保每个任务仅等待一次，或使用单独的同步机制。"),
  ASYNC_WAIT_BEFORE_START("E504", Category.ASYNC, Severity.ERROR, "Wait for async task '%s' occurs before any matching start", "在 wait for 之前先执行 start，并确保两者位于兼容的控制路径。"),
  ;

  private final String code;
  private final Category category;
  private final Severity severity;
  private final String messageTemplate;
  private final String help;

  ErrorCode(String code, Category category, Severity severity, String messageTemplate, String help) {
    this.code = code;
    this.category = category;
    this.severity = severity;
    this.messageTemplate = messageTemplate;
    this.help = help;
  }

  public String code() {
    return code;
  }

  public Category category() {
    return category;
  }

  public Severity severity() {
    return severity;
  }

  public String messageTemplate() {
    return messageTemplate;
  }

  public String help() {
    return help;
  }

  /**
   * 使用占位符顺序填充消息模板，调用方需确保参数顺序正确。
   */
  public String format(Object... args) {
    return String.format(Locale.ROOT, messageTemplate, args);
  }

  public enum Category {
    TYPE,
    SCOPE,
    EFFECT,
    CAPABILITY,
    PII,
    ASYNC,
    OTHER
  }

  public enum Severity {
    ERROR,
    WARNING,
    INFO
  }
}
