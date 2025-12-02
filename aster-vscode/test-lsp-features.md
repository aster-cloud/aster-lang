# LSP 功能测试指南

## 测试文件：examples/hello.aster

```aster
This module is hello.

To main, produce Int:
  Return 42.

To greet with name: Text, produce Text:
  Return Text.concat("Hello, ", name, "!").
```

---

## 测试清单

### 1. Type Check（类型检查）

**测试步骤**：
1. 打开 `examples/hello.aster`
2. 修改第 4 行为：`Return "not an int".`（类型错误）
3. 保存文件

**预期结果**：
- ✅ 应该在 Problems 面板看到类型错误
- ✅ 错误信息：Type mismatch: expected Int, got Text

**实际结果**：
- [ ] 是否看到错误？
- [ ] 错误信息是否正确？

**调试**：
```bash
# 查看 LSP 输出日志
VSCode: View > Output > 选择 "Aster"
# 应该看到 diagnostics 消息
```

---

### 2. Go to Definition（跳转定义）

**测试步骤 1：跳转到内置类型**
1. 打开 `examples/hello.aster`
2. 将光标放在第 3 行的 `Int` 上
3. 按 F12 或右键 "Go to Definition"

**预期结果**：
- ✅ 应该跳转到 Text 类型的定义位置
- ✅ 或显示 "No definition found"（如果内置类型没有源码定义）

**实际结果**：
- [ ] 是否跳转？
- [ ] 跳转到哪里？

---

**测试步骤 2：跳转到本地函数**
1. 在文件末尾添加：
   ```aster
   To test_call, produce Text:
     Return greet("World").
   ```
2. 将光标放在 `greet` 上
3. 按 F12

**预期结果**：
- ✅ 应该跳转到第 6 行的 `greet` 函数定义

**实际结果**：
- [ ] 是否跳转？
- [ ] 跳转位置是否正确？

---

**测试步骤 3：跳转到模块引用**
1. 将光标放在第 7 行的 `Text.concat` 的 `Text` 部分
2. 按 F12

**预期结果**：
- ✅ 应该跳转到 Text 模块或类型定义
- ✅ 或显示 peek definition 窗口

**实际结果**：
- [ ] 是否跳转？
- [ ] 显示什么内容？

---

### 3. Code Completion（代码补全）

**测试步骤**：
1. 在文件末尾新建一行
2. 输入 `To test, produce T`
3. 按 Ctrl+Space

**预期结果**：
- ✅ 应该显示补全列表，包含 `Text`、`Int` 等类型

**实际结果**：
- [ ] 是否显示补全？
- [ ] 补全项是否包含正确的类型？

---

### 4. Find References（查找引用）

**测试步骤**：
1. 将光标放在第 6 行的 `greet` 函数名上
2. 按 Shift+F12 或右键 "Find All References"

**预期结果**：
- ✅ 应该显示所有引用位置（定义 + 调用）

**实际结果**：
- [ ] 是否显示引用列表？
- [ ] 引用数量是否正确？

---

## 常见问题诊断

### 问题 1: LSP 服务器未启动

**检查方法**：
```
View > Output > 选择 "Aster"
```

**应该看到**：
```
[Info] Aster Language Server 已启动
[Info] Connection initialized
[Info] Workspace indexed: X files
```

**如果看不到**：
1. 手动执行：Cmd+Shift+P → "Aster: Start Language Server"
2. 查看错误日志
3. 运行诊断脚本：`./scripts/diagnose.sh`

---

### 问题 2: Type Check 不工作

**可能原因**：
1. ✅ 诊断功能被禁用
2. ✅ 工作区未正确索引
3. ✅ 类型检查器实现问题

**调试步骤**：
```bash
# 1. 检查 LSP 配置
cat .vscode/settings.json | grep aster

# 2. 手动运行类型检查
cd /Users/rpang/IdeaProjects/aster-lang
node dist/scripts/typecheck-cli.js aster-vscode/examples/hello.aster

# 3. 查看是否有类型错误输出
```

**LSP 设置检查**：
```json
{
  "asterLanguageServer.diagnostics.workspace": true  // 应该是 true
}
```

---

### 问题 3: Go to Definition 不工作

**可能原因**：
1. ✅ 索引未构建
2. ✅ 定义位置解析错误
3. ✅ 符号引用解析失败

**调试步骤**：
```bash
# 1. 测试 LSP 定义查找
npm run test:integration:lsp 2>&1 | grep -A 5 "Definition"

# 应该看到：
# ✓ Definition 功能正常（找到定义）

# 2. 手动测试索引
npm run test:lsp-index:nobuild
```

**LSP 日志关键信息**：
```
[Trace] textDocument/definition
  - file: hello.aster
  - position: line 6, character 10
  - result: [定义位置]
```

---

## LSP 功能实现检查清单

### 已实现的功能

从测试结果看，以下功能应该正常工作：

1. ✅ **Diagnostics**（诊断/类型检查）
   - 文件：`dist/src/lsp/diagnostics.js`
   - 测试：`test:lsp:diagnostics`

2. ✅ **Completion**（代码补全）
   - 文件：`dist/src/lsp/completion.js`
   - 测试：`test:lsp:completion`

3. ✅ **Definition**（跳转定义）
   - 文件：`dist/src/lsp/navigation.js`
   - 测试：通过（找到定义）

4. ✅ **References**（查找引用）
   - 文件：`dist/src/lsp/navigation.js`
   - 测试：通过（找到 3 个引用）

5. ✅ **Rename**（重命名）
   - 文件：`dist/src/lsp/navigation.js`
   - 测试：通过（2 个修改）

6. ✅ **Hover**（悬停提示）
   - 文件：`dist/src/lsp/navigation.js`
   - 测试：通过（返回内容）

---

## 收集诊断信息

如果功能仍然不工作，请提供以下信息：

### 1. LSP 服务器日志
```
View > Output > 选择 "Aster"
复制所有输出
```

### 2. VSCode 开发者工具 Console
```
Help > Toggle Developer Tools
查看 Console 中的错误
```

### 3. 测试类型检查命令行工具
```bash
cd /Users/rpang/IdeaProjects/aster-lang
node dist/scripts/typecheck-cli.js aster-vscode/examples/hello.aster
```

应该看到：
```json
{
  "file": "hello.aster",
  "errors": [],
  "warnings": []
}
```

### 4. 测试 LSP 索引
```bash
npm run test:lsp-index:nobuild
```

应该看到：
```
✓ Index 构建正常
✓ 符号查找正常
```

---

## 手动测试 LSP 协议

如果需要更底层的调试，可以直接测试 LSP 协议：

```bash
# 启动 LSP 服务器（stdio 模式）
node dist/src/lsp/server.js --stdio

# 发送初始化请求（JSON-RPC）
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"capabilities":{}}}

# 发送定义查询
{"jsonrpc":"2.0","id":2,"method":"textDocument/definition","params":{"textDocument":{"uri":"file:///path/to/hello.aster"},"position":{"line":6,"character":10}}}
```

---

## 下一步

请按照上述测试清单进行测试，并报告：

1. 哪些功能正常？
2. 哪些功能不工作？
3. 具体的错误信息是什么？
4. LSP 输出日志内容

这将帮助我们精确定位问题所在。
