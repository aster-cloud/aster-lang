> 更新时间：2025-10-08 14:57（NZDT）  
> 执行者：Codex

# 部署指南

## 1. 环境要求
- Node.js ≥ 22.0.0（`package.json:engines` 指定，CI 统一使用 Node 22）
- npm ≥ 10，支持 `npm ci` 与 workspaces 缓存
- Java 21（推荐安装 GraalVM 21，GitHub Actions release/canary 流程使用该版本）
- 操作系统：Ubuntu 22.04 LTS 或 macOS 14+（CI 运行在 Ubuntu，兼容 Linux 部署；macOS 验证本地开发）
- 内存：≥ 8 GB（CI 跑满 `npm run ci`、Gradle/Truffle 测试时需要额外缓冲）
- 磁盘空间：≥ 5 GB（包含 `node_modules`、`dist`、Gradle 缓存与构建产物）

> **提示**：Java 需要设置 `JAVA_HOME`，Gradle 相关脚本会自动回退到 `~/.gradle`，若使用容器部署建议将缓存挂载到单独卷。

## 2. 依赖安装步骤
1. 同步仓库代码并切换到目标 tag 或 commit。
2. 安装 Node 依赖（保持 lockfile 一致）：
   ```bash
   npm ci
   ```
3. 可选：若在离线环境或需重新生成 lockfile，可执行 `npm install --include=optional`（与 docs 工作流一致）。
4. 构建 TypeScript 与 PEG 语法产物：
   ```bash
   npm run build
   ```
5. 运行能力/安全相关基线检查：
   ```bash
   npm run audit      # 依赖安全扫描
   npm run typecheck  # 编译期类型校验
   npm run test       # Golden + property + LSP 测试
   ```

## 3. 构建流程说明
1. `npm run build`：执行 `tsc`，生成 `dist` 目录（含 CLI、LSP、脚本）并运行 `dist/scripts/build-peg.js` 构建语法文件。
2. 可选 `npm run docs:build`：构建 VitePress 文档（GitHub Pages 同步使用该命令）。
3. 若需要 JVM 产物，可执行：
   ```bash
   npm run emit:class -- cnl/examples/greet.aster
   npm run javap:verify
   ```
4. 确认能力校验默认开启：`src/config/runtime.ts` 将 `ASTER_CAP_EFFECTS_ENFORCE` 视作默认 true，仅当值为 `0` 时关闭。
5. 结构化日志：运行 CLI/LSP 时自动输出 JSON 日志，`LOG_LEVEL` 默认为 `INFO`。

## 4. 发布流程
### 4.1 CI 驱动（推荐）
- `main` 分支合并后，`.github/workflows/release.yml` 自动运行：
  1. 安装 Node 22 + GraalVM 21。
  2. `npm run build` 与 `npm run ci`（非阻断）。
  3. 使用 Changesets 生成 “Version PR”。
- 标签发布：`github-release.yml` 会在 `v*` tag 或 `Release published` 事件下生成 GitHub Release。

### 4.2 手动发布
1. 运行预发布校验：
   ```bash
   npm run prepublishOnly
   ```
2. 版本提升（若需要）：
   ```bash
   npm run version-packages
   ```
3. 发布到 npm（需要 `NPM_TOKEN`，并在环境变量中导出 `NODE_AUTH_TOKEN`）：
   ```bash
   npm run release
   ```
4. 推送 tag 并触发 GitHub Release：
   ```bash
   git tag v0.x.y
   git push origin v0.x.y
   ```

## 5. 健康检查与上线验证
1. 运行脚本：
   ```bash
   NODE_ENV=production node scripts/health-check.ts
   ```
2. 期望输出：
   ```json
   {
     "status": "ok",
     "details": { "NODE_ENV": "production" },
     "missingRequired": [],
     "warnings": []
   }
   ```
   - 若 `ASTER_CAP_EFFECTS_ENFORCE` 被设为 `0`，脚本会给出警告，请在生产环境恢复默认。
3. 执行 `npm run ci` 验证核心流程。
4. 验证结构化日志与错误 ID：
   ```bash
   LOG_LEVEL=DEBUG node dist/scripts/cli.js cnl/examples/greet.aster
   ```
   - 输出的 JSON 日志会包含 `timestamp`、`component`、`message` 等字段。

## 6. 生产部署 Checklist
- [ ] 已设置 `NODE_ENV=production`、`JAVA_HOME` 指向 Java 21、`ASTER_CAP_EFFECTS_ENFORCE` 未设为 `0`
- [ ] `npm run build`、`npm run ci`、`npm run audit` 都已通过
- [ ] 确认 `docs/operations/configuration.md` 中所有必需环境变量已配置
- [ ] 检查结构化日志采集管道（`LOG_LEVEL` 设为 `INFO` 或更高；若需调试再临时降级）
- [ ] 确认错误 ID 指标告警已配置（`src/utils/errors.ts` 中 `E1xxx/E2xxx...`）
- [ ] 依赖安全扫描（`npm run audit` / `npx audit-ci --high`）无高危漏洞
- [ ] 更新 capability manifest 并确保 `ASTER_CAPS` 指向最新版本
- [ ] GitHub Actions Release/Docs 工作流均处于绿色状态
