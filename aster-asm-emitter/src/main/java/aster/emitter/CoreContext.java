package aster.emitter;

import java.util.Map;

/**
 * 核心上下文数据，整合模块输入、提示配置与空值策略。
 */
public record CoreContext(
    CoreModel.Module module,
    Map<String, Map<String, String>> hints,
    NullPolicy nullPolicy,
    boolean diagOverload
) {}
