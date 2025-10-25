# Aster VSCode Extension 测试指南

## 前置准备

### 1. 构建 Aster CLI
```bash
# 在项目根目录
./gradlew :aster-lang-cli:installDist
```

验证 CLI 可用：
```bash
./aster-lang-cli/build/install/aster-lang-cli/bin/aster-lang-cli --help
```

### 2. 构建 LSP 服务器
```bash
# 在项目根目录
npm install
npm run build
```

验证 LSP 服务器存在：
```bash
ls dist/src/lsp/server.js
```

### 3. 编译并安装扩展
```bash
cd aster-vscode
npm install
npm run compile
```

## 测试方案

### 测试 1: Compile 命令

**测试文件**: `test/cnl/programs/parser-tests/simple_function.aster`

```aster
This module is simple.

To main, produce Int:
  Return 42.
```

**测试步骤**:
1. 在 VSCode 中打开 `simple_function.aster`
2. 按 `Cmd+Shift+P` (macOS) 或 `Ctrl+Shift+P` (Windows/Linux)
3. 输入 "Aster: Compile File" 并回车
4. 查看进度通知和输出面板

**预期结果**:
- 显示进度通知 "编译 simple_function.aster..."
- 编译成功后显示 "编译成功: simple_function.aster"
- 输出目录生成编译结果：`build/aster-out/`
- 输出面板显示编译输出

**验证编译输出**:
```bash
ls -la build/aster-out/
# 应该包含编译生成的 .class 文件
```

### 测试 2: Package 命令

**测试文件**: 同上 `simple_function.aster`

**测试步骤**:
1. 打开 `simple_function.aster`
2. 执行 "Aster: Package to JAR"
3. 等待编译和打包完成

**预期结果**:
- 显示进度通知 "打包 simple_function.aster 为 JAR..."
- 成功后显示 "JAR 已生成: /path/to/build/aster-out/simple_function.jar"
- 输出面板显示 jar 命令输出

**验证 JAR 文件**:
```bash
ls -lh build/aster-out/simple_function.jar
file build/aster-out/simple_function.jar
# 应显示 "Java archive data (JAR)"
```

### 测试 3: 编译器切换 (TypeScript vs Java)

**测试步骤**:
1. 打开 VSCode 设置 (Cmd+, 或 Ctrl+,)
2. 搜索 "aster.compiler"
3. 切换到 "java"
4. 执行编译命令
5. 切换回 "typescript"
6. 再次执行编译命令

**预期结果**:
- 两种编译器都能成功编译
- 启用调试模式 (`aster.debug.enabled=true`) 可在输出中看到编译器选择信息

### 测试 4: Debug 命令

**前置条件**:
- 已安装 [Debugger for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-debug)

**测试步骤**:
1. 打开 `simple_function.aster`
2. 执行 "Aster: Debug File"
3. 观察调试启动过程

**预期结果**:
- 自动编译文件
- 启动 Java 调试会话
- 如果未安装 Java 调试扩展，显示错误提示

### 测试 5: Build Native 命令

**测试步骤**:
1. 打开任意 `.aster` 文件
2. 执行 "Aster: Build Native Executable"

**预期结果**:
- 显示进度通知
- 编译为 JVM 字节码
- 显示信息 "原生构建功能即将推出。当前已编译为 JVM 字节码。"

### 测试 6: 配置选项

**测试不同配置组合**:

#### 配置 A: 默认设置
```json
{
  "aster.compiler": "typescript",
  "aster.output.directory": "build/aster-out",
  "aster.debug.enabled": false
}
```

#### 配置 B: Java 编译器 + 调试
```json
{
  "aster.compiler": "java",
  "aster.output.directory": "custom-output",
  "aster.debug.enabled": true
}
```

#### 配置 C: 自定义 CLI 路径
```json
{
  "aster.cli.path": "custom/path/to/aster-lang-cli"
}
```

**验证**:
- 每个配置都能正确应用
- 输出目录遵循配置
- 调试模式影响环境变量

### 测试 7: 错误处理

#### 7.1 CLI 未构建
**设置**: 删除或重命名 CLI 可执行文件

**预期结果**:
- 显示错误信息 "Aster CLI 未找到: [path]。请先构建项目（./gradlew :aster-lang-cli:installDist）。"

#### 7.2 非 Aster 文件
**测试**: 打开 `.txt` 或其他文件类型，执行编译命令

**预期结果**:
- 显示警告 "请打开一个 .aster 文件"

#### 7.3 编译错误
**测试文件**: 创建包含语法错误的 `.aster` 文件

**预期结果**:
- 显示错误信息 "Aster compile 失败: [error details]"
- 输出面板显示详细错误

## 测试检查清单

- [ ] Compile 命令：TypeScript 编译器
- [ ] Compile 命令：Java 编译器
- [ ] Package 命令：生成 JAR 文件
- [ ] Debug 命令：启动调试会话
- [ ] Build Native 命令：显示占位信息
- [ ] 配置项：编译器切换正常工作
- [ ] 配置项：输出目录配置生效
- [ ] 配置项：调试模式开关生效
- [ ] 配置项：CLI 路径配置生效
- [ ] 错误处理：CLI 未找到
- [ ] 错误处理：非 Aster 文件
- [ ] 错误处理：编译错误
- [ ] 输出面板：正确显示输出
- [ ] 进度通知：及时显示进度
- [ ] 成功消息：显示成功提示

## 性能测试

### 大文件编译
**测试**: 编译包含 100+ 函数的 Aster 文件

**观察点**:
- 编译时间
- 内存占用
- 输出缓冲是否足够（当前设置 10MB）

### 并发编译
**测试**: 在编译运行时尝试启动另一个编译

**预期**:
- 正常排队执行或显示适当提示

## 已知限制

1. **Debug 命令**: 需要 Java 调试扩展，且调试配置可能需要根据实际编译输出调整 mainClass
2. **Native 构建**: 当前仅编译为 JVM 字节码，原生构建功能待实现
3. **CLI 路径**: 假设 CLI 在默认 Gradle 安装位置，如果使用自定义构建需要手动配置

## 报告问题

测试中发现问题请提供以下信息：
1. VSCode 版本
2. 操作系统
3. 扩展版本 (0.3.0)
4. 配置文件内容 (`.vscode/settings.json`)
5. 错误信息和输出面板日志
6. 重现步骤
