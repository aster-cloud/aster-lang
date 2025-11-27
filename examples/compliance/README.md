# Compliance Policy Demos

SOC2 和 HIPAA 合规验证示例。

## 文件说明

### soc2-audit-demo.aster
**SOC2 审计链验证** - 演示防篡改审计日志（CC7.2 系统操作监控）：

- SHA-256 哈希链结构
- 双重验证：记录完整性 + 链接完整性
- Genesis 记录创建
- 链式追加记录
- 篡改检测

**哈希计算规则**：
```
content = prevHash + eventType + timestamp + tenantId + policyModule + policyFunction + success
currentHash = SHA256(content)
```

**验证流程**：
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Record 1   │    │  Record 2   │    │  Record 3   │
│ prev: ""    │───▶│ prev: hash1 │───▶│ prev: hash2 │
│ curr: hash1 │    │ curr: hash2 │    │ curr: hash3 │
└─────────────┘    └─────────────┘    └─────────────┘
     Genesis          ↑ 检查           ↑ 检查
                  prev == hash1    prev == hash2
```

**关键函数**：
- `verify_record_integrity` - 验证单条记录哈希
- `verify_chain_link` - 验证 prev_hash 与前一条 current_hash 匹配
- `verify_audit_chain` - 完整链验证（双重检查）
- `generate_soc2_report` - 生成合规报告

**示例场景**：
- `demo_valid_chain` - 有效链验证（通过）
- `demo_tampered_chain` - 篡改链检测（失败）

### hipaa-validation-demo.aster
**HIPAA 合规验证** - 演示 PHI 访问控制验证（§164.312）：

- 服务端权限计算（不信任客户端传入）
- 访问级别定义（physician/nurse/billing/admin）
- PHI 类别分类（demographics/medical/financial/psychotherapy）
- 三项验证检查

**关键设计**：
```
// 请求不包含 access_level，由服务端根据 user_role 计算
Define PHIAccessRequest with
  user_id: Text,
  user_role: Text,        // 角色标识
  phi_category_name: Text,
  purpose: Text,
  has_consent: Bool.

// 服务端权威函数计算权限
To validate_access_control:
  Let level be get_access_level(request.user_role).  // 重新计算
```

**验证检查**：
1. §164.312(a) 访问控制 - 基于服务端计算的角色权限
2. §164.508 同意验证 - 检查 L3 数据的同意状态
3. §164.512 使用目的 - 验证合法使用目的

**示例场景**：
- `demo_compliant_access` - 医生合规访问（通过）
- `demo_non_compliant_access` - 账单人员越权访问（失败）
- `demo_spoofed_access_blocked` - 权限伪造被阻止（失败）

## 运行示例

```bash
# 编译（验证语法和类型）
npm run build

# 查看编译后的 Core IR
cat dist/examples/compliance/soc2-audit-demo.json
cat dist/examples/compliance/hipaa-validation-demo.json
```

## SOC2 Trust Service Criteria 覆盖

| Criteria | 描述 | Demo 覆盖 | 实现方式 |
|----------|------|----------|---------|
| CC7.2 | 系统操作监控 | ✅ | `verify_audit_chain` 双重验证 |

## HIPAA Security Rule 覆盖

| Section | 要求 | Demo 覆盖 | 实现方式 |
|---------|------|----------|---------|
| §164.312(a) | 访问控制 | ✅ | `get_access_level` 服务端计算 |
| §164.508 | 同意要求 | ✅ | `validate_consent` |
| §164.512 | 使用限制 | ✅ | `validate_purpose` |

## 相关文档

- [审计与合规指南](/docs/guide/audit-compliance.md)
- [PII 合规指南](/docs/guide/pii-compliance.md)
