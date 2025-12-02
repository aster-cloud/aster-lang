# Healthcare Policy Demos

HIPAA 合规的医疗策略模板示例。

## 文件说明

### patient-record.aster
**患者记录管理** - 演示 HIPAA 合规的 PHI（受保护健康信息）处理：

- PHI 数据结构（L2/L3 敏感级别标注）
- 同意验证函数（`verify_consent`）
- 最小必要原则（角色基础访问控制）
- 数据脱敏（`redact()` 函数）
- 审计日志记录（`log_phi_access`）

**关键函数**：
- `verify_consent` - 验证患者同意状态
- `get_patient_summary` - 根据角色返回不同字段（最小必要原则）
- `display_patient_safe` - 脱敏显示 PHI
- `access_phi_compliant` - 完整合规访问流程

**示例场景**：
- `demo_compliant_access` - 医生有同意访问（合规）
- `demo_non_compliant_access` - 管理员无同意访问（不合规）

### prescription-workflow.aster
**电子处方工作流** - 演示 HIPAA 合规的药物处理：

- 处方数据结构
- 药物相互作用检查（`check_drug_interactions`）
- 处方医生验证（`validate_prescriber`）
- 剂量验证（`verify_dosage`）
- 安全传输（脱敏患者信息）

**关键函数**：
- `verify_prescription` - 完整处方验证流程
- `check_drug_interactions` - 药物相互作用检测
- `transmit_prescription_safe` - 脱敏传输处方

**示例场景**：
- `demo_safe_prescription` - 无相互作用的处方（通过）
- `demo_interaction_prescription` - 华法林+阿司匹林相互作用（失败）

## 运行示例

```bash
# 编译（验证语法和类型）
npm run build

# 查看编译后的 Core IR
cat dist/examples/healthcare/patient-record.json
```

## HIPAA 合规要点

| 要求 | 实现方式 | 状态 |
|------|---------|------|
| §164.312(a) 访问控制 | `get_patient_summary` 角色检查 | ✅ |
| §164.312(b) 审计控制 | `log_phi_access` 函数 | ✅ |
| §164.312(e) 传输安全 | `redact()` 脱敏 | ✅ |
| §164.508 同意 | `verify_consent` 函数 | ✅ |

## 相关文档

- [PII 合规指南](/docs/guide/pii-compliance.md)
- [审计与合规指南](/docs/guide/audit-compliance.md)
