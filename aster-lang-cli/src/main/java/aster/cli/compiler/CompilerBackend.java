package aster.cli.compiler;

import aster.cli.TypeScriptBridge.Result;

import java.util.List;
import java.util.Map;

/**
 * 编译器后端接口
 * <p>
 * 定义统一的编译器调用协议，支持 TypeScript 和 Java 两种实现。
 * 这是渐进式迁移的核心抽象，允许逐阶段替换编译器实现。
 * <p>
 * <b>设计原则</b>：
 * <ul>
 *   <li>对用户透明：CLI 接口保持不变</li>
 *   <li>阶段级回退：某阶段 Java 实现不可用时自动回退到 TypeScript</li>
 *   <li>诊断格式一致：通过统一的 {@link Result} 类型保持输出格式</li>
 * </ul>
 *
 * @see TypeScriptCompilerBackend TypeScript 编译器后端实现
 * @see JavaCompilerBackend Java 编译器后端实现
 */
public interface CompilerBackend {

    /**
     * 执行编译命令
     * <p>
     * 调用底层编译器执行指定阶段的编译任务。
     *
     * @param stage   编译阶段或命令名称（如 "native:cli:class", "typecheck", "parse"）
     * @param args    命令参数（源文件路径、输出目录等）
     * @return 执行结果，包含退出码、标准输出、错误输出和诊断信息
     */
    Result execute(String stage, List<String> args);

    /**
     * 执行编译命令（支持环境变量覆盖）
     * <p>
     * 某些命令需要注入额外的环境变量（如 typecheck 的 ASTER_CAPS）。
     *
     * @param stage       编译阶段或命令名称
     * @param args        命令参数
     * @param envOverrides 环境变量覆盖（键值对）
     * @return 执行结果
     */
    Result execute(String stage, List<String> args, Map<String, String> envOverrides);

    /**
     * 获取编译器类型标识
     *
     * @return 编译器类型（"typescript" 或 "java"）
     */
    String getType();

    /**
     * 检查指定阶段是否可用
     * <p>
     * 用于健康检查和降级决策。
     *
     * @param stage 编译阶段名称
     * @return 如果该阶段已实现且可用则返回 true
     */
    boolean isStageAvailable(String stage);
}
