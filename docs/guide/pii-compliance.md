# PII 合规最佳实践指南

> **状态**: 用户指南
> **版本**: 1.0
> **最后更新**: 2025-11-27

## 概述

Aster 提供内置的 PII（个人身份信息）合规检查功能，帮助开发者在编码阶段发现和修复隐私合规问题。本指南介绍如何使用这些功能来满足 GDPR、HIPAA 等法规要求。

## 快速开始

### 1. PII 类型标注

使用 `@pii` 注解标记敏感数据：

```
Define User with
  email: @pii(L2, email) Text,
  ssn: @pii(L3, ssn) Text,
  name: @pii(L1, name) Text.
```

**敏感级别**：
| 级别 | 说明 | 示例 | 法规要求 |
|------|------|------|---------|
| L1 | 低敏感 | 姓名、偏好 | GDPR: 记录处理目的 |
| L2 | 中敏感 | 邮箱、电话、地址 | GDPR Art. 6: 需合法依据 |
| L3 | 高敏感 | SSN、护照、生物特征 | GDPR Art. 9: 需明确同意 |

### 2. LSP 实时检测

安装 VS Code 扩展后，Aster LSP 自动检测以下问题：

#### HTTP 传输 PII (E400)
```
// ⚠️ 警告: PII data transmitted over HTTP without encryption
To send_email with email: @pii(L2, email) Text:
  Return Http.post("http://api.example.com", email).
```

**修复方案**：
```
// ✅ 使用 HTTPS
Return Http.post("https://api.example.com", email).

// 或使用 redact() 脱敏
Return Http.post("http://api.example.com", redact(email)).
```

#### 日志泄露 PII (W074)
```
// ⚠️ 警告: PII data may be exposed in logs
To log_user with ssn: @pii(L3, ssn) Text:
  Log.info("User SSN: " + ssn).
```

**修复方案**：
```
// ✅ 使用 redact() 脱敏
Log.info("User SSN: " + redact(ssn)).

// 或移除敏感数据
Log.info("Processing user request").
```

#### 缺失同意检查 (E403)
```
// ⚠️ 警告: Function processes PII data without consent check (GDPR Art. 6)
To process_user with data: @pii(L2, email) Text:
  Return store(data).
```

**修复方案**：
```
// ✅ 方案 1: 添加 @consent_required 注解
@consent_required
To process_user with data: @pii(L2, email) Text:
  Return store(data).

// ✅ 方案 2: 调用同意检查函数
To process_user with data: @pii(L2, email) Text:
  checkConsent().
  Return store(data).
```

## Hover 提示

将鼠标悬停在 PII 参数上，查看合规提示：

```
To send_notification with email: @pii(L2, email) Text:
                              ↑
                    悬停显示合规信息
```

**悬停内容示例**：
```
Parameter **email**: @pii(L2, email) Text

⚠️ **PII Data** (Level: L2)
- 🟠 Medium sensitivity: email, phone, address
- GDPR: Lawful basis required (Art. 6)
- Consider encryption at rest

*Use `redact()` or `tokenize()` before external transmission*
```

## Quick Fix 操作

LSP 提供以下 Quick Fix：

| 问题 | Quick Fix | 操作 |
|------|-----------|------|
| HTTP PII 传输 | `Fix: Wrap with redact()` | 自动包装 `redact()` |
| 缺失同意检查 | `Fix: Add @consent_required` | 自动添加注解 |
| Console PII | `Hint: Remove PII from logs` | 提示手动修复 |
| Database PII | `Hint: Encrypt before storage` | GDPR Art. 32 提示 |

## 最佳实践清单

### 数据收集阶段 ✅
- [ ] 仅收集必要的 PII
- [ ] 为所有 PII 字段添加 `@pii` 标注
- [ ] 记录数据收集的合法依据

### 数据处理阶段 ✅
- [ ] 处理 PII 的函数添加 `@consent_required` 注解
- [ ] 使用 `checkConsent()` 验证用户同意
- [ ] L3 数据处理前获取明确同意

### 数据传输阶段 ✅
- [ ] 使用 HTTPS 传输 PII
- [ ] 传输前考虑使用 `redact()` 或 `tokenize()`
- [ ] 向第三方传输时审查数据处理协议

### 数据存储阶段 ✅
- [ ] L3 数据使用 `Crypto.hash()` 或 `Crypto.encrypt()`
- [ ] 实施适当的访问控制
- [ ] 定期审计 PII 访问日志

### 日志记录阶段 ✅
- [ ] 不记录 L2/L3 级别 PII
- [ ] 使用 `redact()` 脱敏后记录
- [ ] 使用结构化日志并过滤敏感字段

## 同意检查函数

Aster 识别以下同意检查函数：

```
// 标准函数名
checkConsent()
requireConsent()
verifyConsent()

// 命名空间形式
Consent.check()
Consent.require()
Consent.verify()
GDPR.checkConsent()
GDPR.requireConsent()

// 检查函数
hasConsent()
isConsentGiven()
```

**自定义同意检查**：
```
// 使用 @consent 注解标记自定义函数
@consent
To my_consent_check with user_id: Text, produce Bool:
  Return ConsentDb.check(user_id).
```

## 配置选项

在 `aster.config.json` 中配置 PII 检查：

```json
{
  "pii": {
    "enabled": true,
    "strictMode": false,
    "sensitivity": {
      "minLevel": "L2"
    },
    "allowedDomains": [
      "internal-api.company.com",
      "secure.company.com"
    ],
    "sanitizers": [
      "Crypto.hash",
      "Crypto.encrypt",
      "redact",
      "tokenize"
    ]
  }
}
```

| 选项 | 说明 | 默认值 |
|------|------|--------|
| `enabled` | 启用 PII 检查 | `true` |
| `strictMode` | 严格模式（警告变错误） | `false` |
| `minLevel` | 最低报告级别 | `"L1"` |
| `allowedDomains` | HTTP 传输白名单 | `[]` |
| `sanitizers` | 净化函数列表 | 内置列表 |

## 法规参考

### GDPR（通用数据保护条例）

| 条款 | 要求 | Aster 支持 |
|------|------|-----------|
| Art. 6 | 处理需合法依据 | `@consent_required` 注解 |
| Art. 9 | 特殊类别数据需明确同意 | L3 级别检查 |
| Art. 32 | 实施适当安全措施 | 加密/脱敏提示 |
| Art. 33 | 泄露通知义务 | Sink 检测警告 |

### HIPAA（健康保险可移植性和责任法）

| 规则 | 要求 | Aster 支持 |
|------|------|-----------|
| Privacy Rule | 限制 PHI 使用/披露 | PII 类型标注 |
| Security Rule | 技术保障措施 | 加密要求提示 |
| Breach Notification | 泄露通知 | HTTP/日志检测 |

## 常见问题

### Q: 如何禁用特定警告？

使用 `@safe-pii` 注解标记已审计的代码：

```
// @safe-pii: 已通过安全审计 2025-01-15
To send_verified with data: @pii(L2, email) Text:
  Return Http.post("https://verified-api.com", data).
```

### Q: 误报如何处理？

1. 使用配置白名单减少误报
2. 为净化函数添加到 `sanitizers` 列表
3. 使用 `@safe-pii` 标记特例

### Q: 如何测试 PII 检查？

运行 PII 诊断测试：

```bash
npm run test:pii-default
```

## 相关文档

- [PII 污点分析算法设计](/docs/reference/pii-taint-analysis.md)
- [LSP Code Actions 指南](/docs/guide/lsp-code-actions.md)
- [类型系统参考](/docs/reference/types.md)

---

**注意**：本指南提供技术实现指导，不构成法律建议。请咨询专业法律顾问以确保完整合规。
