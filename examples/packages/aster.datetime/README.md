# aster.datetime

更新日期：2025-11-24 23:18 NZST（Codex）

## 模块概述
aster.datetime 通过 `DateParts` 数据结构展示了日期时间格式化的典型需求，并复用 aster.strings 中的装饰函数，让输出结构既具备语义又包含长度提示。

## 目录结构
- `manifest.json`：声明对 `aster.strings` 依赖，并请求 `Cpu` 与 `Time` 能力。
- `src/formatter.aster`：示例函数 `formatLocalDatetime`、`describeWindow` 负责组合日期与时间文本。
- `README.md`：当前说明文档。

## 使用场景
1. **本地化格式化**：`formatLocalDatetime` 将结构化 `DateParts` 转换为人类可读字符串。
2. **窗口描述**：`describeWindow` 融合开始与结束时间，并调用 aster.strings 输出长度说明，方便日志或 UI 展示。

## 构建提示
确保 `aster.strings` 已由 `npm run build:examples` 打包后再引用本包，从而在 `.aster/local-registry/aster.datetime/1.0.0.tar.gz` 中写入正确的依赖拓扑。
