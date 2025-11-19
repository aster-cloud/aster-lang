## 2025-11-19 11:30 NZST - Codex
- 调整 `aster-finance` 任务依赖：为 `generateFinanceDtos` 增加 `quarkus-policy-api:syncPolicyClasses`、`syncPolicyJar` 依赖，修复 Gradle 校验
- 更新 `quarkus-policy-api` 构建依赖：将 `build/aster-out/aster.jar` 降级为 `runtimeOnly`，避免旧版 `AsyncTaskRegistry` 覆盖源码
- 修正 `WorkflowRetryIntegrationTest` 使用新的确定性上下文 API：通过自定义构造传入 `DeterminismContext` 并使用 `DeterminismSnapshot` 重放
- 执行 `./gradlew :quarkus-policy-api:compileTestJava --no-build-cache` 验证编译通过（存在已知 MicroProfile Config 警告）
