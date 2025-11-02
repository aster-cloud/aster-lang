# LSP 功能调试指南

## 问题描述

Type Check 和 Go to Definition 不工作。

## 验证结果

### ✅ 后端功能正常

**类型检查器**（命令行）：
```bash
$ node dist/scripts/typecheck-cli.js aster-vscode/examples/bad-types.aster
ERROR: Return type mismatch: expected Int, got Text
```
✅ **结论**：类型检查器本身工作正常

**LSP 集成测试**：
```bash
$ npm run test:integration:lsp
✓ Definition 功能正常（找到定义）
✓ References 功能正常（找到 3 个引用）
✓ Diagnostics 功能正常
```
✅ **结论**：LSP 协议实现正常

---

## 可能的问题点

### 问题 1: LSP 客户端未连接到服务器 🔴

**症状**：
- 没有代码补全
- 没有错误提示
- 跳转定义不工作

**检查方法**：
```
VSCode: View > Output > 选择 "Aster"
```

**应该看到的正常日志**：
```
[Info - 时间] Aster Language Server 已启动
[Info - 时间] Connection established
[Info - 时间] Client capabilities: {...}
[Info - 时间] Server initialized
```

**如果看不到日志**：
- LSP 服务器未启动
- 需要手动执行：`Cmd+Shift+P` → "Aster: Start Language Server"

**如果看到错误**：
```
[Error] Cannot find package 'vscode-languageserver'
```
→ 重新安装扩展（参考 TROUBLESHOOTING.md）

---

### 问题 2: 工作区未正确索引 🟡

**症状**：
- LSP 服务器已启动
- 但 Go to Definition 不工作
- Find References 不工作

**检查方法**：
查看 LSP 输出日志中是否有：
```
[Info] Workspace indexed: X files
[Info] Module 'hello' indexed
```

**如果没有索引日志**：

可能原因：
1. 工作区未打开（单独打开文件）
2. `.aster` 文件不在工作区根目录
3. 索引构建失败

**解决方案**：
```bash
# 确保打开的是工作区文件夹，而非单个文件
File > Open Folder > 选择包含 .aster 文件的文件夹
```

---

### 问题 3: 诊断消息未发送 🟡

**症状**：
- LSP 服务器已启动
- 工作区已索引
- 但 Problems 面板没有错误

**检查方法**：

1. **启用 LSP 追踪**：
   ```json
   // .vscode/settings.json
   {
     "aster.trace.server": "verbose"
   }
   ```

2. **查看追踪日志**：
   ```
   View > Output > 选择 "Aster Language Server - Trace"
   ```

3. **应该看到**：
   ```
   [Trace - 时间] Sending notification 'textDocument/didOpen'
   [Trace - 时间] Sending notification 'textDocument/didChange'
   [Trace - 时间] Received notification 'textDocument/publishDiagnostics'
     - uri: file:///path/to/file.aster
     - diagnostics: [...]
   ```

**如果没有 publishDiagnostics**：
- 诊断功能被禁用
- 类型检查器未运行
- 需要检查 LSP 服务器代码

---

### 问题 4: 定义位置解析错误 🟡

**症状**：
- LSP 服务器正常
- 工作区已索引
- 但 Go to Definition 跳转到错误位置或不跳转

**调试步骤**：

1. **检查索引内容**：
   ```bash
   # 运行索引测试
   npm run test:lsp-index:nobuild
   ```

2. **查看定义查询日志**：
   启用追踪后，执行 Go to Definition 应该看到：
   ```
   [Trace] Received request 'textDocument/definition - (ID)
     - textDocument: file:///path/to/hello.aster
     - position: { line: 6, character: 10 }
   [Trace] Sending response 'textDocument/definition - (ID)
     - result: [
         {
           uri: file:///path/to/hello.aster,
           range: { start: { line: 5, character: 3 }, end: { line: 5, character: 8 } }
         }
       ]
   ```

3. **手动测试定义查找**：
   ```bash
   # 创建测试脚本
   cat > test-definition.js << 'EOF'
   import { parse } from './dist/parser.js';
   import { buildIdIndex } from './dist/src/lsp/utils.js';

   const code = `
   This module is test.

   To greet with name: Text, produce Text:
     Return Text.concat("Hello, ", name).
   `;

   const ast = parse(code);
   const index = buildIdIndex(ast);

   console.log('Index:', JSON.stringify(index, null, 2));
   EOF

   node test-definition.js
   ```

---

## 启用详细日志

### 方法 1: VSCode 设置

在工作区 `.vscode/settings.json` 中添加：

```json
{
  "aster.trace.server": "verbose",
  "asterLanguageServer.diagnostics.workspace": true,
  "asterLanguageServer.index.persist": false  // 禁用持久化，强制重建索引
}
```

### 方法 2: 环境变量

在启动 VSCode 前设置：

```bash
export ASTER_DEBUG=true
export LSP_LOG_LEVEL=debug
code .
```

---

## 收集完整诊断信息

请执行以下步骤并提供输出：

### 1. 运行诊断脚本
```bash
cd /Users/rpang/IdeaProjects/aster-lang/aster-vscode
./scripts/diagnose.sh > diagnostic-report.txt
```

### 2. 检查 LSP 服务器日志
```
1. 打开 VSCode
2. View > Output > 选择 "Aster"
3. 复制所有日志内容
```

### 3. 检查 LSP 追踪日志
```
1. 添加 "aster.trace.server": "verbose" 到 settings.json
2. 重新加载窗口（Developer: Reload Window）
3. View > Output > 选择 "Aster Language Server - Trace"
4. 打开一个 .aster 文件
5. 尝试 Go to Definition
6. 复制所有追踪日志
```

### 4. 测试命令行工具
```bash
# 类型检查
node dist/scripts/typecheck-cli.js aster-vscode/examples/bad-types.aster

# LSP 索引
npm run test:lsp-index:nobuild

# LSP 集成测试
npm run test:integration:lsp 2>&1 | grep -E "(Definition|Diagnostics|✓|✗)"
```

### 5. 开发者工具 Console
```
1. Help > Toggle Developer Tools
2. 切换到 Console 标签
3. 过滤: aster 或 language
4. 复制所有相关日志
```

---

## 临时解决方案

### 如果 Type Check 不工作

**手动检查类型**：
```bash
cd /Users/rpang/IdeaProjects/aster-lang
node dist/scripts/typecheck-cli.js <your-file.aster>
```

### 如果 Go to Definition 不工作

**手动查找定义**：
```bash
# 使用 grep 查找函数定义
grep -n "^To greet" <your-file.aster>

# 或查找类型定义
grep -n "^A User" <your-file.aster>
```

### 如果 Code Completion 不工作

**参考文档**：
- 内置类型：Int, Text, Bool, Unit
- 内置模块：Text, List, Map, Result, Option
- 查看 `LSP-FEATURES.md` 了解完整类型系统

---

## 已知问题和限制

### 1. 跨模块定义跳转

**状态**：部分支持

- ✅ 同模块内的函数/类型定义
- ✅ 内置类型（Int, Text 等）
- ⚠️ 跨模块引用（可能需要完整工作区索引）

### 2. 泛型类型推断

**状态**：实验性

- ✅ 基础泛型定义
- ⚠️ 复杂泛型推断（可能有限制）

### 3. 效果系统类型检查

**状态**：完整支持

- ✅ @io, @cpu, @pure 效果声明
- ✅ 效果传播检查
- ✅ 能力清单验证

---

## 下一步

请提供以上诊断信息，包括：

1. ✅ 诊断脚本输出
2. ✅ LSP 服务器日志
3. ✅ LSP 追踪日志（如果可以启用）
4. ✅ 开发者工具 Console 输出
5. ✅ 具体的复现步骤

这将帮助我们精确定位是：
- 🔴 LSP 连接问题
- 🟡 索引构建问题
- 🟡 诊断消息发送问题
- 🟡 定义解析问题
- 🟢 LSP 实现问题

根据诊断结果，我们可以：
1. 修复扩展配置
2. 修复 LSP 服务器代码
3. 更新文档说明限制
4. 提供临时解决方案
