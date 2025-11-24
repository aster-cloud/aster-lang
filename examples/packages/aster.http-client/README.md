# aster.http-client

更新日期：2025-11-24 23:18 NZST（Codex）

## 模块概述
aster.http-client 用于演示请求描述与构建流程，函数会生成可读的调用摘要，并依赖 aster.strings 统一格式化输出，manifest 中启用了 Http 能力以匹配网络调用场景。

## 目录结构
- `manifest.json`：声明对 `aster.strings` 的依赖，允许 Http 能力。
- `src/client.aster`：包含 `callHealthcheck` 与 `buildJsonBody`，体现如何组合字符串与长度信息。
- `README.md`：模块说明文件。

## 使用场景
1. **健康检查描述**：`callHealthcheck` 输出 GET 请求摘要，可作为 CLI 打印或日志记录。
2. **请求体注释**：`buildJsonBody` 结合字符串长度说明，方便调试压缩级别与内容大小。

## 构建提示
执行 `npm run build:examples` 即可在 `.aster/local-registry/aster.http-client/1.0.0.tar.gz` 中生成当前包的分发包；由于依赖 `aster.strings`，请先确保字符串包已完成打包。
