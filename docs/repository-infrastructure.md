# 仓库基础设施指南

> 更新时间：2025-11-25 09:15 NZST · 执行者：Codex

本指南描述 Aster 语言代码库在 GitHub 组织、命名与版本策略、发布流水线、CI/CD 以及质量标准方面的约束，供所有贡献者与维护者统一遵循。

## GitHub 组织结构

- **组织与团队**：源码托管在 `wontlost-ltd` 组织；按职责划分 `lang-core`、`cli-experience`、`infra-release` 与 `docs` 四个团队，分别负责语言核心、CLI/包管理、发布与平台工程，以及文档与站点维护。
- **仓库分层**：核心语言仓库 `aster-lang` 是单一真实来源，辅助仓库（如示例项目、官网）通过 GitHub Actions 的 `repository_dispatch` 与主仓库保持同步。
- **权限模型**：默认通过 `CODEOWNERS` 控制评审权限；主分支只允许 `lang-core` 与 `infra-release` 拥有写权限，其余团队以 PR 方式合并。
- **议题标签**：统一使用 `area/*`（模块）、`severity/*`（影响）、`status/*`（进度）三组标签，CI 会校验缺失标签的 PR，避免未分类议题进入主干。
- **自动化配置**：Dependabot、Release Drafter 与 branch protection 配置存放在 `.github/`，修改后需同步更新 `docs/repository-infrastructure.md` 并通知 `infra-release` 团队。

## 包命名策略

- 所有可发布包遵循 `aster.<domain>.<name>` 形式（仅允许小写字母、数字和连字符），确保在 registry 与 CLI 中具备唯一性。
- 领域 `domain` 建议使用两级命名（如 `core.math`、`workflow.crm`），帮助读者快速识别功能范畴。
- CLI 会在安装阶段验证命名规则与 `manifest.schema.json` 中的正则表达式，失败时以 `[M003] Invalid package name` 报错。
- 示例包与教程使用 `examples.<topic>` 命名，避免与正式包冲突；所有示例均应发布在 `.aster/local-registry` 中，便于离线验证。

## 版本管理

- **SemVer 约定**：使用 `MAJOR.MINOR.PATCH`，禁止附加 build metadata；breaking change 才能提升 MAJOR，新增功能但兼容时提升 MINOR，补丁或文档/构建更新使用 PATCH。
- **高频分支**：`main` 作为可发布分支，`release/x.y` 分支用于准备稳定版本；hotfix 基于最新稳定分支创建 `hotfix/x.y.z-*`，完成后先合并回 `release/x.y` 再回 `main`。
- **标签策略**：每次发布在 `main` 和相应 release 分支打 annotated tag（如 `v1.4.0`），并在 Release Drafter 中引用，方便 CLI 与 docs 对齐版本号。
- **版本来源**：包管理器、CLI 与示例 manifest 均从 `package.json` 的 `version` 字段读取版本。修改版本后须同步更新 `docs/guide/package-management/manifest-reference.md` 中的对照表。

## 发布流程

1. 完成 P0/P1 缺陷修复并确认 CI 全绿。
2. `infra-release` 团队创建/更新 `release/x.y`，同步 cherry-pick 计划进入版本的 PR，执行 `npm run build && npm run test` 验证。
3. 在 `release/x.y` 上更新版本号、变更日志与 docs 发布说明，确保 `docs/publishing-guide.md` 清单全部完成。
4. 使用 Release Drafter 生成候选说明，人工校对后触发 `Release` workflow，生成 npm 包、GitHub Release 资产与 docs 静态站点。
5. 将 release tag 合并回 `main`，保证两条分支的版本与文档一致。

## CI/CD 管线

- **Checks**：主仓库在 PR 上运行 `npm run lint`, `npm run typecheck`, `npm test`, `npm run test:cli`, `npm run docs:build` 以及 JVM 子模块的 Gradle 测试。所有管线均要求在 20 分钟内完成，以便频繁提交。
- **分布式缓存**：Node 与 Gradle 构建使用 GitHub Actions cache + S3 artifact 组合，加速 PEG 生成、示例包 tarball 与 JVM class 输出。
- **分支保护**：`main` 与 `release/*` 分支启用「必须所有检查通过」及「至少一位代码所有者评审」策略，并禁止强推。
- **部署目标**：`docs:build` 产物自动部署到 Cloudflare Pages；`npm publish`、GitHub Release 与示例 registry 更新由 `infra-release` workflow 串联完成。

## 质量标准

- **代码评审**：所有代码均需两名 reviewer 批准，其中至少一位来自对应模块的代码所有者。
- **测试覆盖**：CLI 模块语句覆盖率需 ≥85%，核心编译链 golden 测试必须保持 100% 通过。任何下降必须在 PR 描述中注明原因。
- **文档同步**：功能合并前必须更新用户文档、教程与 release note；未同步的 PR 将在 `docs-sync` 检查中失败。
- **运行基线**：性能与内存基线由 `npm run bench` 与 JVM benchmark 提供，若回归超过 3% 必须先修复后才能发布。
- **可观测性**：新增包或 CLI 行为必须搭配日志/诊断输出，确保后续问题可复现；相关说明写入 `docs/guide/package-management/overview.md`。
