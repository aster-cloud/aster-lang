# 故障排查指南

## 问题：command 'aster.startLanguageServer' not found

### 症状
- 从命令面板执行任何 Aster 命令时显示 "command not found"
- 扩展似乎没有被激活

### 根本原因
扩展未被 VSCode 正确加载或激活。可能的原因：
1. 扩展未正确安装
2. VSCode 缓存了旧版本
3. 扩展激活条件未满足
4. 扩展加载时出错

### 解决步骤

#### 步骤 1: 检查扩展是否已安装
```bash
# 列出已安装的扩展
code --list-extensions --show-versions | grep aster

# 预期输出：
# wontlost.aster-vscode@0.3.0
```

如果没有输出，说明扩展未安装，请执行步骤 2。

#### 步骤 2: 完全卸载旧版本
```bash
# 卸载所有旧版本
code --uninstall-extension wontlost.aster-vscode

# 清理 VSCode 扩展缓存（可选但推荐）
# macOS/Linux:
rm -rf ~/.vscode/extensions/wontlost.aster-vscode-*

# Windows:
# rmdir /s %USERPROFILE%\.vscode\extensions\wontlost.aster-vscode-*
```

#### 步骤 3: 安装最新版本
```bash
cd /Users/rpang/IdeaProjects/aster-lang/aster-vscode
code --install-extension aster-vscode-0.3.0.vsix --force
```

**重要**：使用 `--force` 参数确保覆盖安装。

#### 步骤 4: 完全重启 VSCode
不要使用 "Reload Window"，而是完全退出并重新启动 VSCode：
1. `Cmd+Q` (macOS) 或 `Alt+F4` (Windows) 完全退出
2. 重新启动 VSCode
3. 打开包含 `.aster` 文件的工作区

#### 步骤 5: 检查扩展激活状态
1. 打开开发者工具：`Help > Toggle Developer Tools`
2. 在 Console 中查找扩展加载错误
3. 搜索关键词：`aster`、`activation`、`error`

#### 步骤 6: 验证激活事件
打开任意 `.aster` 文件或工作区包含 `.aster` 文件，扩展应自动激活。

检查方法：
1. 打开命令面板 (`Cmd+Shift+P` / `Ctrl+Shift+P`)
2. 输入 "Aster"
3. 应该能看到以下命令：
   - ✅ Aster: Start Language Server
   - ✅ Aster: Compile File
   - ✅ Aster: Debug File
   - ✅ Aster: Build Native Executable
   - ✅ Aster: Package to JAR

#### 步骤 7: 手动触发激活
如果以上步骤都完成但命令仍未找到，尝试手动触发激活：

1. 创建一个测试文件 `test.aster`：
   ```
   This module is test.

   To hello, produce Text:
     Return "Hello".
   ```

2. 保存文件
3. VSCode 应该自动激活扩展（通过 `onLanguage:aster` 事件）

#### 步骤 8: 检查输出日志
1. 打开输出面板：`View > Output`
2. 从下拉菜单选择 "Aster"
3. 查看是否有错误消息

常见错误消息：
```
[ERROR] 资源类型: LSP
资源路径: /path/to/dist/src/lsp/server.js
错误消息: Aster LSP 服务器未找到
```

如果看到此错误，说明内置资源未正确打包。请检查：
```bash
unzip -l aster-vscode-0.3.0.vsix | grep server.js
# 应该看到：extension/dist/src/lsp/server.js
```

### 高级诊断

#### 检查扩展安装目录
```bash
# macOS/Linux
ls -la ~/.vscode/extensions/wontlost.aster-vscode-*/

# 应该包含：
# - package.json
# - out/extension.js
# - dist/src/lsp/server.js
# - dist/scripts/aster.js
```

#### 检查 Node.js 版本
```bash
node --version
# 需要 >= 16.0.0
```

#### 检查 VSCode 版本
扩展要求 VSCode >= 1.85.0

```bash
code --version
```

### 已知问题

#### 问题 1: 在开发模式下运行（按 F5）
如果您是从源码调试扩展：
1. 确保先运行 `npm run build:all`
2. 检查 `dist/` 目录是否存在
3. 检查 `out/` 目录是否包含编译后的 .js 文件

#### 问题 2: VSIX 安装失败
错误消息：`Extension install failed: unable to extract`

解决方案：
```bash
# 重新打包
npm run package

# 验证 VSIX 完整性
unzip -t aster-vscode-0.3.0.vsix
```

#### 问题 3: 多个工作区冲突
如果您同时打开了多个工作区，VSCode 可能加载了错误的扩展实例。

解决方案：
1. 关闭所有 VSCode 窗口
2. 仅打开一个包含 `.aster` 文件的工作区
3. 验证扩展激活

### 仍然无法解决？

如果以上步骤都无效，请提供以下信息：

1. **VSCode 版本**：`code --version`
2. **Node.js 版本**：`node --version`
3. **操作系统**：macOS / Windows / Linux
4. **扩展列表**：`code --list-extensions`
5. **开发者工具 Console 输出**（截图）
6. **输出面板 "Aster" 日志**（截图）

提交 Issue：https://github.com/wontlost-ltd/aster-lang/issues

---

## 其他常见问题

### LSP 服务器未自动启动

**症状**：扩展已激活，但没有代码补全、跳转定义等 LSP 功能。

**检查**：
1. 打开输出面板 "Aster"
2. 查找 "Aster Language Server 已启动" 消息
3. 如果没有，手动执行 "Aster: Start Language Server" 命令

**可能原因**：
- 工作区未包含 `.aster` 文件
- `dist/src/lsp/server.js` 文件缺失或损坏

### 编译命令失败

**症状**：执行 "Aster: Compile File" 报错。

**检查**：
```bash
# 检查 CLI 是否存在
ls -la ~/.vscode/extensions/wontlost.aster-vscode-*/dist/scripts/aster.js

# 检查 Node.js 版本
node --version  # 需要 >= 16
```

### 配置不生效

**症状**：修改了 `aster.langServer.path` 等配置但没有效果。

**解决**：
1. 配置修改后需要重新加载窗口：`Developer: Reload Window`
2. 确保配置语法正确（JSON 格式）
3. 留空配置使用内置资源（推荐）
