# 失败样例分析报告

- 日期：2025-10-22 00:32 NZST
- 执行者：Codex
- 数据来源：`./.claude/scripts/test-all-examples.sh` 解析日志（已导出 `/tmp/failures.txt`）
- 当前通过率：50 / 131（38.2%）

## 错误模式统计（按频次排序）

| 错误模式 | 文件数 | 代表样例 | 说明 |
|----------|--------|----------|------|
| `mismatched input ',' expecting {'.', '?', 'and'}` | 10 | `annotations_range.aster` | 结构/参数列表不接受逗号分隔，仍强制使用 and |
| `mismatched input ':' expecting '['` | 9 | `eff_caps_parse_bare.aster` | `It performs io:` 冒号语法缺失，仅支持 `It performs io [..]` |
| `extraneous input {operator}` | 11 | `test_gte.aster` | 运算符作为函数调用（`>=(a, b)`、`+(x, y)` 等）未正确解析 |
| `mismatched input '@' expecting {'(', TYPE_IDENT}` | 3 | `pii_type_basic.aster` | 顶层/返回类型注解语法缺失 |
| `mismatched input 'of' expecting {',', 'produce'}` | 3 | `stdlib_collections.aster` | 泛型 `List of Int` / `Result of` 语法缺失 |
| 其它零散错误 | 11 | `annotations_mixed.aster` 等 | 包含注解参数、Lambda return 缺少句点、枚举匹配等细项 |

> 注：同一文件可能触发多个错误模式，合计大于 81。

## 高频语法缺失 Top 5

| 排名 | 问题描述 | 影响文件数 | 预期提升率 | 修复复杂度 | 优先级 |
|------|----------|------------|------------|------------|--------|
| 1 | 运算符函数调用：`>=(a, b)` / `+(x, y)` 等前缀写法无法被识别 | 11 | ≈ +8.4%（上限，含与#2重叠） | 中 | 高 |
| 2 | 数据结构与调用实参仍需使用 `and`，逗号语法未落地 | 10 | ≈ +7.6% | 低 | 高 |
| 3 | 能力标注只支持 `It performs io [Caps]`，缺少冒号写法 | 9 | ≈ +6.9% | 低 | 高 |
| 4 | `@pii(L2, email) Text` 等类型级注解语法缺失 | 3 | ≈ +2.3% | 中 | 中 |
| 5 | 泛型 `List of Int` / `Result of Ok and Err` 未解析 | 3 | ≈ +2.3% | 中 | 中 |

## 观察与建议

- 逗号语法与能力冒号语法均为文档示例中的主流写法，修复成本低且覆盖率高，应立即落地。
- 运算符函数调用在多个测试与示例中复现，但当前语法看似已定义，对应实现需进一步排查（可能是 Lexer/AST 拼装约束）。
- 类型注解与 `Result of` 泛型为后续隐私与错误处理语法的前提，建议列为下一梯队处理。
- 其它零散问题（枚举匹配、注解参数、Lambda return 句点等）数量较低，可在主问题修复后集中处理。
