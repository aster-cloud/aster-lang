# aster.strings

更新日期：2025-11-24 23:18 NZST（Codex）

## 模块概述
aster.strings 展示了如何在字符串处理逻辑中依赖 aster.math，提供文本长度摘要与包装输出的示例函数，用于演示跨包调用与依赖关系声明。

## 目录结构
- `manifest.json`：声明对 `aster.math` 的依赖并请求 CPU 能力。
- `src/utils.aster`：包含 `describeLength`、`surroundWithBars` 等示例函数，内部会调用 aster.math 中的计算工具。
- `README.md`：当前模块说明。

## 使用场景
1. **文本长度提示**：`describeLength` 根据原始文本长度叠加额外 padding，输出中文提示语句。
2. **可视化包裹**：`surroundWithBars` 将文本配上等级标签，便于 CLI 或日志中定位关键信息。

## 构建提示
运行 `npm run build:examples` 时会读取本目录并生成 tarball，供本地包管理实验使用。确保 `aster.math` 已存在于 `.aster/local-registry/aster.math/1.0.0.tar.gz` 中，以满足依赖关系。
