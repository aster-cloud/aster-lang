package aster.cli.compiler;

import aster.cli.TypeScriptBridge;
import aster.cli.TypeScriptBridge.BridgeException;
import aster.cli.TypeScriptBridge.Result;

import java.util.List;
import java.util.Map;

/**
 * TypeScript 编译器后端实现
 * <p>
 * 包装现有的 {@link TypeScriptBridge}，提供统一的编译器接口。
 * 这是当前的默认实现，将逐步被 Java 实现替代。
 */
public final class TypeScriptCompilerBackend implements CompilerBackend {

    private final TypeScriptBridge bridge;

    /**
     * 创建 TypeScript 编译器后端
     *
     * @param bridge TypeScript 桥接器
     */
    public TypeScriptCompilerBackend(TypeScriptBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public Result execute(String stage, List<String> args) {
        try {
            return bridge.executeCommand(stage, args);
        } catch (BridgeException e) {
            // 将桥接异常转换为 Result（便于统一错误处理）
            return new Result(1, "", e.getMessage(), List.of());
        }
    }

    @Override
    public Result execute(String stage, List<String> args, Map<String, String> envOverrides) {
        try {
            return bridge.executeCommand(stage, args, envOverrides);
        } catch (BridgeException e) {
            return new Result(1, "", e.getMessage(), List.of());
        }
    }

    @Override
    public String getType() {
        return "typescript";
    }

    @Override
    public boolean isStageAvailable(String stage) {
        // TypeScript 编译器所有阶段均可用
        return true;
    }
}
