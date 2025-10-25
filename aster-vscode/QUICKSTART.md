# Aster VSCode 扩展快速开始

## 5 分钟快速上手

### 步骤 1: 安装前置依赖

确保已安装：
- Visual Studio Code >= 1.85.0
- Node.js >= 16
- Java SDK (用于运行编译后的代码)

### 步骤 2: 构建 Aster 项目

在 Aster 项目根目录执行：

```bash
# 安装依赖
npm install

# 构建 TypeScript 代码和 LSP 服务器
npm run build

# 构建 CLI 工具
./gradlew :aster-lang-cli:installDist
```

验证构建成功：
```bash
# 检查 LSP 服务器
ls dist/src/lsp/server.js

# 检查 CLI
./aster-lang-cli/build/install/aster-lang-cli/bin/aster-lang-cli --help
```

### 步骤 3: 安装 VSCode 扩展

#### 方式 A: 开发模式（推荐用于测试）

1. 在 VSCode 中打开 `aster-vscode` 目录
2. 按 `F5` 启动扩展开发主机
3. 在新窗口中打开 Aster 项目

#### 方式 B: VSIX 安装（推荐用于日常使用）

```bash
cd aster-vscode
npm install
npm run package
code --install-extension aster-vscode-0.3.0.vsix
```

然后重启 VSCode。

### 步骤 4: 配置扩展（可选）

在项目根目录创建 `.vscode/settings.json`：

```json
{
  "aster.compiler": "typescript",
  "aster.output.directory": "build/aster-out",
  "aster.cli.path": "aster-lang-cli/build/install/aster-lang-cli/bin/aster-lang-cli",
  "aster.debug.enabled": false
}
```

### 步骤 5: 编写第一个 Aster 程序

创建文件 `hello.aster`：

```aster
This module is hello.

To main, produce Int:
  Return 42.

To greet with name: Text, produce Text:
  Return Text.concat("Hello, ", name, "!").
```

### 步骤 6: 使用扩展功能

#### 编译文件
1. 打开 `hello.aster`
2. 按 `Cmd+Shift+P` (macOS) 或 `Ctrl+Shift+P` (Windows/Linux)
3. 输入 "Aster: Compile File"
4. 查看输出面板

#### 打包为 JAR
1. 执行 "Aster: Package to JAR"
2. 等待完成，JAR 文件生成在 `build/aster-out/hello.jar`

#### 运行 JAR
```bash
java -jar build/aster-out/hello.jar
```

### 步骤 7: 享受 LSP 功能

- **自动补全**: 输入时按 `Ctrl+Space`
- **跳转定义**: 按 `F12` 或 `Cmd+点击`
- **查找引用**: 按 `Shift+F12`
- **重命名**: 按 `F2`
- **格式化**: 按 `Shift+Alt+F`
- **错误检查**: 实时显示红色波浪线

## 常用命令速查

| 命令 | 快捷键 | 说明 |
|------|--------|------|
| Aster: Compile File | `Cmd/Ctrl+Shift+P` | 编译当前文件 |
| Aster: Package to JAR | `Cmd/Ctrl+Shift+P` | 打包为 JAR |
| Aster: Debug File | `Cmd/Ctrl+Shift+P` | 启动调试 |
| Go to Definition | `F12` | 跳转定义 |
| Find All References | `Shift+F12` | 查找引用 |
| Rename Symbol | `F2` | 重命名符号 |
| Format Document | `Shift+Alt+F` | 格式化文档 |

## 切换编译器

### 使用 TypeScript 编译器（默认）
```json
{
  "aster.compiler": "typescript"
}
```

### 使用 Java 编译器
```json
{
  "aster.compiler": "java"
}
```

## 故障排除

### LSP 服务器未启动
**症状**: 没有代码补全、错误检查等功能

**解决**:
1. 检查输出面板：`View > Output` → 选择 "Aster Language Server"
2. 手动启动：`Cmd/Ctrl+Shift+P` → "Aster: Start Language Server"
3. 验证 LSP 服务器存在：`ls dist/src/lsp/server.js`

### 编译命令失败
**症状**: "Aster CLI 未找到"

**解决**:
```bash
# 重新构建 CLI
./gradlew :aster-lang-cli:installDist

# 验证 CLI 存在
ls aster-lang-cli/build/install/aster-lang-cli/bin/aster-lang-cli
```

### 输出目录不存在
**症状**: 编译后找不到输出文件

**解决**:
```bash
# 创建输出目录
mkdir -p build/aster-out

# 或者在配置中使用绝对路径
{
  "aster.output.directory": "/absolute/path/to/output"
}
```

## 下一步

- 阅读完整文档：[README.md](README.md)
- 查看测试指南：[TESTING.md](TESTING.md)
- 浏览示例代码：`test/cnl/programs/`
- 学习 LSP 功能：[LSP-FEATURES.md](LSP-FEATURES.md)

## 反馈与帮助

- 问题反馈：https://github.com/wontlost-ltd/aster-lang/issues
- 功能建议：提交 Issue 并标记 `enhancement`
