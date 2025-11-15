# GitHub Actions 工作流优化实施

- 日期：2025-11-15 21:56 NZDT
- 执行：Codex

## 主要变更
1. **复用组件**：新增 `.github/actions/setup-env` composite action，支持 checkout、Node/GraalVM 安装、npm ci、Gradle 权限与缓存策略参数化；新增 `.github/workflows/_reusable-build.yml`，统一 TypeScript 构建并产出 `build-artifacts`。
2. **CI 重构**：`ci.yml` 改为串联 reusable build job，并在所有 job 中使用 setup-env，移除重复的 checkout、Node/GraalVM 配置与 `chmod +x gradlew`，保持原有测试/发布矩阵。
3. **辅助 workflow 调整**：`canary.yml` 引入独立构建 job 并依赖产物完成测试与发布；`build-native.yml` 与 `e2e-tests.yml` 统一使用 setup-env，拆分 TypeScript 构建阶段，减少重复脚本。
4. **Lint 统一**：新增 `.yamllint`，自定义 line-length、brackets、truthy、document-start 规则以适配包含 shell 脚本的 workflow；所有新增/更新 workflow 已通过 lint。

## 关键实现细节
- setup-env 支持 `enable-node/java/gradle`、`java-provider`、`native-image-job-reports`、`registry-url`、`working-directory` 等输入，可在需要时自动切换 GraalVM / Temurin 并运行 `npm ci`。
- reusable TypeScript build 仅负责生成 `dist/` 并上传 artifact，CI、Canary、Native workflow 可直接 `needs` 此 job，避免重复执行 `npm run build`。
- CI 文件按 job 分组（Node 质量、Bench、安全、JVM、Cross-stack、Quarkus、Policy API、Truffle 等），所有 job 共享同一 setup step，可在未来通过调整 action 输入统一升级 Java/Node 版本。
- `canary.yml` 现先构建再发布，主 job 仅负责测试与 npm 发布，保留 PR 评论。
- `build-native.yml` 通过 `native-dist` artifact 替换 `npm run build` 步骤，Gradle 缓存交由 GraalVM/Setup-Java 内置机制处理。
- `e2e-tests.yml` 简化为使用 setup-env 进行 checkout，主要逻辑集中在 docker build/push 与 compose 运行。
- `.yamllint` 设置 `max: 200` 行长且允许不可拆分单词，关闭 document-start/truthy/brackets 规则，保证描述性命令/脚本不会触发误报。
