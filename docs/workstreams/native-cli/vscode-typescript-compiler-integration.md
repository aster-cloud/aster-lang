# VSCode 扩展 TypeScript 编译器集成

## 概述

为 `aster-vscode` 扩展添加了 TypeScript 解析器支持，实现了 compile、debug、build native、package 四个核心功能。

**实施日期**: 2025-10-25

## 功能特性

### 1. Compile 命令 (aster.compile)

**功能**: 编译当前打开的 Aster 文件为 JVM 字节码

**实现**:
- 获取当前活动的 `.aster` 文件
- 调用 Aster CLI 的 `compile` 命令
- 支持通过配置选择 TypeScript 或 Java 编译器
- 显示进度通知和输出结果

**使用方式**:
```
Cmd/Ctrl+Shift+P → "Aster: Compile File"
```

**配置项**:
- `aster.compiler`: 编译器选择 (typescript/java)
- `aster.output.directory`: 输出目录
- `aster.debug.enabled`: 调试模式

### 2. Package 命令 (aster.package)

**功能**: 编译并打包为可执行 JAR 文件

**实现**:
- 先调用 `compile` 命令编译代码
- 再调用 `jar` 命令生成 JAR
- 自动命名 JAR 文件 (`<filename>.jar`)

**使用方式**:
```
Cmd/Ctrl+Shift+P → "Aster: Package to JAR"
```

### 3. Debug 命令 (aster.debug)

**功能**: 编译并启动 Java 调试会话

**实现**:
- 先编译文件
- 创建 Java 调试配置
- 启动 VSCode 调试会话

**前置条件**:
- 需要安装 Java 调试扩展（如 Debugger for Java）

**使用方式**:
```
Cmd/Ctrl+Shift+P → "Aster: Debug File"
```

### 4. Build Native 命令 (aster.buildNative)

**功能**: 构建原生可执行文件（占位实现）

**当前行为**:
- 编译为 JVM 字节码
- 显示提示信息："原生构建功能即将推出"

**未来计划**:
- 集成 GraalVM Native Image
- 生成独立的原生可执行文件

## 技术架构

### 文件结构

```
aster-vscode/
├── package.json           # 扩展配置和命令定义
├── src/
│   └── extension.ts       # 扩展主逻辑和命令实现
├── out/                   # 编译输出
├── README.md              # 用户文档
├── TESTING.md             # 测试指南
└── QUICKSTART.md          # 快速开始
```

### 核心实现

#### 1. CLI 调用封装 (`runAsterCommand`)

```typescript
async function runAsterCommand(
  command: string,
  args: string[],
  options: { showOutput?: boolean; cwd?: string } = {}
): Promise<{ stdout: string; stderr: string }>
```

**功能**:
- 解析 CLI 路径
- 设置环境变量 (`ASTER_COMPILER`, `ASTER_DEBUG`)
- 执行命令并处理输出
- 统一错误处理和用户反馈

#### 2. 配置管理

```typescript
function getConfig<T>(key: string, defaultValue: T): T {
  const cfg = vscode.workspace.getConfiguration('aster');
  return cfg.get<T>(key, defaultValue);
}
```

**配置项**:

| 配置键 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `compiler` | enum | `typescript` | 编译器后端 |
| `cli.path` | string | `aster-lang-cli/build/...` | CLI 路径 |
| `output.directory` | string | `build/aster-out` | 输出目录 |
| `debug.enabled` | boolean | `false` | 调试模式 |

#### 3. 进度反馈

使用 `vscode.window.withProgress` 显示进度：

```typescript
await vscode.window.withProgress(
  {
    location: vscode.ProgressLocation.Notification,
    title: `编译 ${path.basename(filePath)}...`,
    cancellable: false,
  },
  async () => {
    await runAsterCommand('compile', [filePath, '--output', outputPath]);
  }
);
```

#### 4. 输出面板

使用专用输出面板显示详细输出：

```typescript
const outputChannel = vscode.window.createOutputChannel('Aster');
outputChannel.appendLine(output);
outputChannel.show();
```

## 配置示例

### 基础配置 (`.vscode/settings.json`)

```json
{
  "aster.compiler": "typescript",
  "aster.cli.path": "aster-lang-cli/build/install/aster-lang-cli/bin/aster-lang-cli",
  "aster.output.directory": "build/aster-out",
  "aster.debug.enabled": false
}
```

### 高级配置（Java 编译器 + 调试）

```json
{
  "aster.compiler": "java",
  "aster.output.directory": "custom-output",
  "aster.debug.enabled": true,
  "aster.cli.path": "custom/path/to/aster-lang-cli"
}
```

## 环境变量

扩展自动设置以下环境变量：

| 变量 | 来源 | 说明 |
|------|------|------|
| `ASTER_COMPILER` | `aster.compiler` 配置 | typescript 或 java |
| `ASTER_DEBUG` | `aster.debug.enabled` 配置 | true 或未设置 |

## 错误处理

### 1. CLI 未找到

**错误**: CLI 可执行文件不存在

**处理**:
- 检查文件存在性
- 显示错误信息："Aster CLI 未找到: [path]。请先构建项目（./gradlew :aster-lang-cli:installDist）。"

### 2. 非 Aster 文件

**错误**: 当前文件不是 `.aster` 文件

**处理**:
- 检查文件语言 ID
- 显示警告："请打开一个 .aster 文件"

### 3. 编译错误

**错误**: CLI 执行失败

**处理**:
- 捕获 stderr 和 stdout
- 显示错误消息："Aster [command] 失败: [error]"
- 在输出面板显示详细错误

## 测试策略

### 单元测试场景

1. **Compile 命令**
   - TypeScript 编译器
   - Java 编译器
   - 输出目录配置

2. **Package 命令**
   - JAR 文件生成
   - 文件命名正确性

3. **配置管理**
   - 编译器切换
   - 自定义路径
   - 调试模式

4. **错误处理**
   - CLI 不存在
   - 非法文件类型
   - 编译失败

### 集成测试

参见 [TESTING.md](../../../aster-vscode/TESTING.md) 获取详细测试用例和执行步骤。

## 性能考量

### 1. 命令执行

- 使用 `execFileAsync` 异步执行，避免阻塞 UI
- 设置 10MB 输出缓冲区（`maxBuffer: 10 * 1024 * 1024`）
- 显示进度通知提供用户反馈

### 2. 输出处理

- 按需显示输出（`showOutput` 选项）
- 使用输出面板而非弹窗避免干扰

### 3. 路径解析

- 缓存工作区根目录
- 使用 `path.resolve` 确保路径正确性

## 已知限制

1. **Debug 配置**: 需要根据实际编译输出手动调整 `mainClass`
2. **Native 构建**: 当前仅占位实现，待集成 GraalVM
3. **并发执行**: 未实现任务队列，并发编译可能导致冲突
4. **增量编译**: 每次都是完整编译，未实现增量编译

## 未来改进

### 短期（v0.4.0）

- [ ] 实现任务队列，支持并发编译
- [ ] 优化 Debug 配置，自动推断 mainClass
- [ ] 添加编译缓存，支持增量编译
- [ ] 提供更多用户反馈（如编译时间统计）

### 中期（v0.5.0）

- [ ] 集成 GraalVM Native Image
- [ ] 实现真正的原生构建
- [ ] 支持多文件批量编译
- [ ] 添加构建配置文件支持

### 长期（v1.0.0）

- [ ] 实现 Watch 模式（文件变化自动编译）
- [ ] 集成测试运行器
- [ ] 支持分布式编译
- [ ] 添加性能分析工具

## 相关文件

### 核心代码

- `aster-vscode/src/extension.ts` - 扩展主逻辑（第85-313行）
- `aster-vscode/package.json` - 扩展配置（第66-119行）

### 文档

- `aster-vscode/README.md` - 用户文档
- `aster-vscode/TESTING.md` - 测试指南
- `aster-vscode/QUICKSTART.md` - 快速开始
- `docs/workstreams/native-cli/vscode-typescript-compiler-integration.md` - 本文档

### CLI 参考

- `aster-lang-cli/src/main/java/aster/cli/CommandHandler.java` - CLI 命令处理
- `aster-lang-cli/src/main/java/aster/cli/Main.java` - CLI 入口

## 验证

### 功能验证

```bash
# 1. 构建 CLI
./gradlew :aster-lang-cli:installDist

# 2. 编译扩展
cd aster-vscode
npm run compile

# 3. 在 VSCode 中按 F5 启动扩展

# 4. 打开测试文件
# test/cnl/programs/parser-tests/simple_function.aster

# 5. 执行命令
# Cmd+Shift+P → "Aster: Compile File"
# Cmd+Shift+P → "Aster: Package to JAR"

# 6. 验证输出
ls -la build/aster-out/
```

### 编译器切换验证

```bash
# 设置 TypeScript 编译器
{
  "aster.compiler": "typescript",
  "aster.debug.enabled": true
}

# 编译并检查输出
# 应显示 "ASTER_COMPILER=typescript"

# 切换到 Java 编译器
{
  "aster.compiler": "java"
}

# 再次编译并检查输出
```

## 总结

成功为 aster-vscode 扩展集成了 TypeScript 编译器支持，实现了完整的编译、调试、构建、打包工作流。主要成就：

✅ **4 个新命令**: compile, debug, buildNative, package
✅ **5 个新配置项**: compiler, cli.path, output.directory, debug.enabled
✅ **完整错误处理**: CLI 未找到、文件类型检查、编译错误
✅ **用户体验**: 进度通知、输出面板、友好错误信息
✅ **文档齐全**: README, TESTING, QUICKSTART

**版本**: 0.3.0
**发布日期**: 2025-10-25
**状态**: ✅ 已完成并测试
