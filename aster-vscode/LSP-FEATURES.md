# Aster LSP 功能详解

本文档详细说明Aster语言服务器协议（LSP）支持的所有功能及其使用方法。

---

## 📋 目录

1. [代码补全](#代码补全)
2. [跳转定义](#跳转定义)
3. [查找引用](#查找引用)
4. [重命名符号](#重命名符号)
5. [悬停提示](#悬停提示)
6. [错误诊断](#错误诊断)
7. [代码格式化](#代码格式化)
8. [工作区符号](#工作区符号)
9. [文档符号](#文档符号)
10. [代码操作](#代码操作)
11. [语义标记](#语义标记)
12. [其他功能](#其他功能)

---

## 代码补全

**快捷键**: `Ctrl+Space` (Windows/Linux) / `Cmd+Space` (macOS)

### 功能说明
智能提示关键字、类型、函数、变量等。

### 支持的补全类型

#### 1. 关键字补全
输入时自动提示Aster语言关键字：
```aster
this module is    # 输入 "th" → 提示 "this module is"
To                # 输入 "T" → 提示 "To"
Return            # 输入 "R" → 提示 "Return"
```

#### 2. 类型补全
```aster
# 在类型位置提示内置类型和自定义类型
To greet with name: T|    # 光标在 | 位置，按 Ctrl+Space
# 提示: Text, Int, Bool, User（如果已定义）
```

#### 3. 函数补全
```aster
# 在调用位置提示可用函数
Let result be g|    # 光标在 | 位置
# 提示: greet, getUserData（如果已定义）
```

#### 4. 详细信息查看
选中补全项后，右侧会显示详细信息：
- 函数签名
- 参数类型
- 返回类型
- 文档说明

---

## 跳转定义

**快捷键**: `F12` 或 `Ctrl+Click`

### 功能说明
跳转到符号（函数、类型、变量）的定义位置。

### 支持场景

#### 1. 跳转到函数定义
```aster
# 在调用处按 F12 跳转到函数定义
To main produce Text:
  Let greeting be greet("Alice").  # 在 greet 上按 F12
  Return greeting.

# ↓ 跳转到 ↓

To greet with name: Text, produce Text:
  Return "Hello, {name}".
```

#### 2. 跳转到类型定义
```aster
# 在类型引用处按 F12 跳转到类型定义
To process with user: User, produce Text:  # 在 User 上按 F12
  Return user.name.

# ↓ 跳转到 ↓

A User is a record of:
  It has name: Text.
  It has age: Int.
```

#### 3. 跳转到变量定义
```aster
To main produce Text:
  Let greeting be "Hello".
  Return greeting.    # 在 greeting 上按 F12 → 跳转到 Let 语句
```

#### 4. 跨文件跳转
```aster
# 文件 a.aster
This module is demo.utils.

To helper with x: Int, produce Int:
  Return x * 2.

# 文件 b.aster
This module is demo.main.

Import helper from demo.utils.

To main produce Int:
  Return helper(21).    # 在 helper 上按 F12 → 跳转到 a.aster
```

---

## 查找引用

**快捷键**: `Shift+F12` 或 右键菜单 → "Find All References"

### 功能说明
查找符号在整个工作区的所有使用位置（包括定义和引用）。

### ✨ 最新增强 (2025-10-14)
- ✅ 现在返回**所有真实引用**（不仅是定义）
- ✅ 支持跨文件查找
- ✅ 使用lexer精确定位
- ✅ 批量处理优化性能

### 使用场景

#### 1. 查找函数的所有调用位置
```aster
To greet with name: Text, produce Text:    # 在 greet 上按 Shift+F12
  Return "Hello, {name}".

# 结果显示:
# 1. greet.aster:3 - 定义位置
# 2. main.aster:10 - 调用 greet("Alice")
# 3. test.aster:5 - 调用 greet("Bob")
```

#### 2. 查找类型的所有使用
```aster
A User is a record of:    # 在 User 上按 Shift+F12
  It has name: Text.

# 结果显示所有使用 User 类型的位置
```

#### 3. 查找变量的所有引用
```aster
To main produce Text:
  Let greeting be "Hello".    # 在 greeting 上按 Shift+F12
  Let message be greeting + " World".
  Return message.

# 结果显示:
# 1. 定义位置（Let 语句）
# 2. 所有使用位置
```

### 快捷操作
- 双击结果项跳转到对应位置
- `Esc` 关闭引用面板
- 结果按文件分组显示

---

## 重命名符号

**快捷键**: `F2` 或 右键菜单 → "Rename Symbol"

### 功能说明
安全地重命名函数、类型、变量，自动更新所有引用。

### ✨ 最新增强 (2025-10-14)
- ✅ 完整实现 `prepareRename`
- ✅ 重命名前验证光标位置
- ✅ 显示精确的符号范围
- ✅ 支持重命名预览

### 使用步骤

#### 1. 重命名函数
```aster
To greet with name: Text, produce Text:    # 光标在 greet 上按 F2
  Return "Hello, {name}".

# 步骤:
# 1. 按 F2 → 弹出输入框，高亮显示 "greet"
# 2. 输入新名称 "sayHello"
# 3. 按 Enter 确认
# 4. 所有引用自动更新
```

#### 2. 重命名类型
```aster
A User is a record of:    # 光标在 User 上按 F2
  It has name: Text.

# 重命名为 "Person" → 所有 User 引用更新为 Person
```

#### 3. 重命名变量
```aster
To main produce Text:
  Let greeting be "Hello".    # 光标在 greeting 上按 F2
  Return greeting.

# 重命名为 "message" → 两处都更新
```

### 重命名范围

#### 文件内重命名
- 默认只更新当前文件

#### 工作区重命名
- 配置 `asterLanguageServer.rename.scope: "workspace"`
- 更新整个工作区的所有引用

### 限制条件
- 只能重命名符号（函数、类型、变量）
- 不能重命名关键字
- 光标必须在符号上（否则F2无效）

---

## 悬停提示

**快捷键**: 鼠标悬停 或 `Ctrl+K Ctrl+I`

### 功能说明
显示符号的类型信息、函数签名、文档说明。

### 提示内容

#### 1. 函数签名
```aster
To greet with name: Text, produce Text:
  Return "Hello, {name}".

# 悬停在 greet 上显示:
# ------------------------------
# greet(name: Text) -> Text
#
# 函数: greet
# 参数: name: Text
# 返回: Text
# ------------------------------
```

#### 2. 类型信息
```aster
Let user be User {
  name: "Alice",
  age: 30
}.

# 悬停在 user 上显示:
# ------------------------------
# user: User
#
# 类型: User { name: Text, age: Int }
# ------------------------------
```

#### 3. 效果声明
```aster
To fetch_data, produce Text. It performs io:
  Return Http.get("/api/data").

# 悬停在 fetch_data 上显示:
# ------------------------------
# fetch_data() -> Text [io]
#
# 效果: @io
# 允许的操作: Http, Db, Files
# ------------------------------
```

---

## 错误诊断

**实时提示**: 自动在问题面板和编辑器中显示错误

### 功能说明
实时类型检查、能力验证、语法错误检查。

### 诊断类型

#### 1. 类型错误
```aster
To add with a: Int, b: Int, produce Int:
  Return a + "hello".    # ❌ 类型错误: Text 不能与 Int 相加
```

#### 2. 能力违规
```aster
# 声明为纯函数但调用了 IO 操作
To process produce Text:
  Return Http.get("/api").    # ❌ 能力错误: 纯函数不能执行 IO
```

#### 3. 未定义符号
```aster
To main produce Text:
  Return unknownFunction().    # ❌ 错误: 未定义的函数
```

#### 4. 语法错误
```aster
To greet with name: Text produce Text    # ❌ 语法错误: 缺少逗号
  Return "Hello".
```

### 诊断级别
- 🔴 **错误** (Error): 必须修复才能编译
- 🟡 **警告** (Warning): 建议修复但不影响编译
- 🔵 **信息** (Info): 提示性信息

### 快捷操作
- `F8` 跳转到下一个错误
- `Shift+F8` 跳转到上一个错误
- 点击灯泡图标查看快速修复建议

---

## 代码格式化

**快捷键**: `Shift+Alt+F` (Windows/Linux) / `Shift+Option+F` (macOS)

### 功能说明
自动格式化代码，保持一致的代码风格。

### 格式化模式

#### 1. Lossless 模式（默认）
保留所有注释和空行，只调整缩进和空格。

**配置**:
```json
{
  "asterLanguageServer.format.mode": "lossless",
  "asterLanguageServer.format.reflow": true
}
```

#### 2. Normalize 模式
标准化代码结构，可能移除多余空行。

**配置**:
```json
{
  "asterLanguageServer.format.mode": "normalize"
}
```

### 格式化示例

**格式化前**:
```aster
To greet with name:Text,produce Text:
Return "Hello, "+name.
```

**格式化后**:
```aster
To greet with name: Text, produce Text:
  Return "Hello, " + name.
```

---

## 工作区符号

**快捷键**: `Ctrl+T` (Windows/Linux) / `Cmd+T` (macOS)

### 功能说明
跨整个工作区搜索符号（函数、类型等）。

### 使用方式
1. 按 `Ctrl+T` 打开符号搜索框
2. 输入符号名称（支持模糊匹配）
3. 选择结果跳转

### 搜索示例
```
输入: "greet"
结果:
  📦 greet (demo.app)           - 函数
  📦 greeting (demo.utils)       - 变量
  📄 GreetingService (services)  - 类型
```

---

## 文档符号

**快捷键**: `Ctrl+Shift+O` (Windows/Linux) / `Cmd+Shift+O` (macOS)

### 功能说明
显示当前文件的所有符号（函数、类型、变量）。

### 符号层级
```
📄 demo.app.aster
  📦 greet (函数)
    📌 name (参数)
  📦 User (类型)
    📌 name (字段)
    📌 age (字段)
  📦 main (函数)
    📌 greeting (变量)
```

---

## 代码操作

**快捷键**: `Ctrl+.` (Windows/Linux) / `Cmd+.` (macOS)

### 功能说明
快速修复建议和代码优化。

### 支持的操作

#### 1. 添加效果声明
```aster
To fetch_data produce Text:
  Return Http.get("/api").    # 💡 快速修复: 添加 "It performs io"
```

#### 2. 修复数值歧义
```aster
Let x be 42.    # 💡 快速修复: 明确指定 Int 或 Float
```

#### 3. 修复能力清单
```aster
# 如果启用了能力检查
To process produce Text. It performs io:
  Return Files.read("data.txt").    # 💡 快速修复: 更新 capabilities.json
```

---

## 语义标记

### 功能说明
高级语法高亮，基于语义而非正则表达式。

### 标记类型
- **关键字**: `This`, `To`, `Return` 等
- **函数名**: 定义和调用
- **类型名**: `Text`, `Int`, `User` 等
- **变量**: 参数、局部变量
- **效果标记**: `@io`, `@cpu`, `@pure`
- **能力前缀**: `Http`, `Db`, `Files` 等

### 配置主题
语义标记遵循VSCode主题配置，可在 `settings.json` 中自定义：

```json
{
  "editor.semanticTokenColorCustomizations": {
    "rules": {
      "function": "#4EC9B0",
      "type": "#4FC1FF"
    }
  }
}
```

---

## 其他功能

### 1. 文档链接

点击模块引用跳转到对应文件：
```aster
Import helper from demo.utils.    # Ctrl+Click 跳转到 utils.aster
```

### 2. Inlay Hints

显示推断的类型信息：
```aster
Let greeting be "Hello".    # 显示: greeting: Text
```

**配置**:
```json
{
  "editor.inlayHints.enabled": "on"
}
```

### 3. 文档高亮

高亮显示相同符号的所有出现位置（光标移动时自动触发）。

### 4. 签名帮助

函数调用时显示参数提示：
```aster
Let result be greet(|    # 光标在 | 位置时显示: greet(name: Text)
```

---

## 性能优化建议

### 大型项目优化

#### 1. 启用索引持久化
```json
{
  "asterLanguageServer.index.persist": true
}
```

#### 2. 限制工作区诊断
```json
{
  "asterLanguageServer.diagnostics.workspace": false
}
```

#### 3. 调整Hover延迟
```json
{
  "editor.hover.delay": 500
}
```

---

## 故障排除

### LSP 响应慢
**解决方案**:
1. 检查文件数量（>100个文件可能较慢）
2. 启用索引持久化
3. 关闭不必要的工作区诊断

### References 返回不完整
**解决方案**:
1. 等待工作区索引完成
2. 手动触发索引重建：重启LSP服务器
3. 检查文件是否在工作区内

### Rename 失败
**解决方案**:
1. 确认光标在符号上（不在空白处）
2. 检查符号是否可重命名（不能重命名关键字）
3. 如果跨文件重命名失败，检查文件权限

---

## 键盘快捷键速查

| 功能 | Windows/Linux | macOS |
|------|--------------|-------|
| 代码补全 | `Ctrl+Space` | `Cmd+Space` |
| 跳转定义 | `F12` | `F12` |
| 查找引用 | `Shift+F12` | `Shift+F12` |
| 重命名 | `F2` | `F2` |
| 悬停提示 | 鼠标悬停 | 鼠标悬停 |
| 格式化 | `Shift+Alt+F` | `Shift+Option+F` |
| 工作区符号 | `Ctrl+T` | `Cmd+T` |
| 文档符号 | `Ctrl+Shift+O` | `Cmd+Shift+O` |
| 快速修复 | `Ctrl+.` | `Cmd+.` |
| 下一个错误 | `F8` | `F8` |

---

## 相关链接

- [主 README](./README.md)
- [LSP 改进计划](../.claude/lsp-improvement-plan.md)
- [LSP 分析报告](../.claude/lsp-analysis-report.md)
- [GitHub Issues](https://github.com/wontlost-ltd/aster-lang/issues)

---

**最后更新**: 2025-10-14
**版本**: 0.2.0
