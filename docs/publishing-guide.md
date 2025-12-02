# 发布指南

> 更新时间：2025-11-25 09:18 NZST · 执行者：Codex

本文档提供发布前检查、构建、打包、发布与验证的端到端流程，用于确保 CLI、包管理与文档版本保持一致。

## 发布前检查清单

- 确认 `main` 或 `release/x.y` 分支无未解决的 P0/P1 缺陷，CI 最近一次全绿。
- `CHANGELOG.md`、网站新闻与 `docs/repository-infrastructure.md` 均已更新对应版本信息。
- 所有 CLI 与语言示例在最新 `npm run build`、`npm run test:cli`、`npm run docs:build` 中通过，且 docs 站点已本地预览。
- 发行版本号已在 `package.json`、`manifest.schema.json`、样例 manifest 及 Release Drafter 草稿中同步。
- 发布所需密钥（npm token、GitHub PAT、Cloudflare API Token）均配置在 GitHub Actions Secrets，并完成一次干跑（dry-run）。

## 构建流程

1. 清理工作树：`git clean -fdx`，确保无陈旧 dist/lockfile 干扰。
2. 执行 `npm run build && npm run typecheck && npm run lint`，确认 TypeScript 与 CLI 模块可在干净环境构建。
3. 运行 `npm run test` 与 `npm run test:cli:coverage`，记录覆盖率结果并附在发布公告中。
4. 对 JVM 子模块执行 `./gradlew :quarkus-policy-api:test :aster-truffle:test`，确保跨栈示例依旧可运行。
5. 运行 `npm run docs:build`，确认文档导航、示例代码与包管理教程均显示最新版本号。

## 打包规则

- `npm pack` 输出的 tarball 必须包含 dist、schema、manifest 与 README，禁止包含 `.aster/local-registry` 等大体积目录。
- 示例包通过 `npm run build:examples` 生成 `.aster/local-registry/<pkg>/<version>.tar.gz`，需与主版本保持一致。
- GitHub Release 资产包括：CLI 可执行文件（压缩）、manifest schema、示例包集合 ZIP 以及 docs 构建摘要。
- 在 `scripts/release/` 中记录 tarball 的 SHA256，供客户现场验证。

## 发布步骤

1. 切换到 `release/x.y` 分支并确认版本号与 changelog。执行 `git tag -a vx.y.z -m "Aster vx.y.z"`。
2. 使用 `npm publish --provenance` 发布 CLI 到 npm。若 CLI 依赖示例包，需先发布示例包，再发布主 CLI。
3. 触发 `Release` GitHub Actions workflow，它会构建 docs、上传 release 资产并将文档部署在 Cloudflare Pages。
4. 使用 `scripts/release/post_publish.sh` 更新 `.aster/local-registry`、docs 下载链接以及内部镜像（若存在）。
5. 将 tag 合并回 `main`，并在 `#release` 频道同步发布说明与安装指引。

## 发布后验证

- `npm info aster-lang version` 与 `node dist/scripts/aster.js --version` 输出须匹配新版本。
- 在干净目录执行 `npx aster-lang@latest init demo`（或安装示例包）验证 CLI 是否可从 npm 拉取并生成锁文件。
- 运行 `npm run docs:preview` 或访问生产站点检查导航、示例命令与下载链接。
- 对 `.aster/local-registry` 进行一次 `tar -tzf` 抽查，确保 tarball 未损坏。
- 在 `docs/testing.md` 与 `.claude/operations-log.md` 记录本次发布验证情况，便于后续审计。
