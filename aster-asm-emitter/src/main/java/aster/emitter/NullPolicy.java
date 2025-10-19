package aster.emitter;

import java.util.Map;

/**
 * 空值策略配置，包含严格模式开关与按函数覆盖表。
 */
public record NullPolicy(
    boolean strict,
    Map<String, boolean[]> overrides
) {}
