# aster.math

更新日期：2025-11-24 23:18 NZST（Codex）

## 模块概述
aster.math 提供基础算术工具，用于演示如何在 Aster 包中组织纯计算逻辑。当前示例实现了求和、范围裁剪与百分比换算，便于其他包在做文本、时间或网络处理时复用统一的计算方法。

## 目录结构
- `manifest.json`：符合 `manifest.schema.json` 的包描述，声明对 CPU 能力的读取权限。
- `src/calculator.aster`：包含 addNumbers、clampNumber、ratioInPercent 三个示例函数。
- `README.md`：当前说明文件。

## 使用场景
1. **数值归一化**：通过 `clampNumber` 控制阈值，避免依赖方处理异常输入。
2. **百分比呈现**：`ratioInPercent` 简化比值转百分比逻辑，可直接输出 UI 所需的整数或浮点。
3. **统一求和**：`addNumbers` 作为演示入口，方便其他包展示跨包调用写法。

## 构建提示
本包无额外依赖，执行 `npm run build:examples` 时会被自动打包到 `.aster/local-registry/aster.math/1.0.0.tar.gz`，供离线示例仓库引用。
