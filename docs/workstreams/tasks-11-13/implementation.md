# Tasks 11-13 实施记录

- **日期**：2025-11-13 08:43 NZST  
- **执行者**：Codex

## JMH 性能基准
- 新建 `quarkus-policy-api-benchmarks` 模块（Gradle + JMH），托管 `PolicyEvaluationBenchmark` 与自定义缓存引导器。
- 结果输出到 `build/reports/jmh/policy-evaluation.log`（含 p50/p95/p99 以及批量吞吐量数据）。
- 运行命令：`SKIP_GENERATE_ASTER_JAR=1 ./gradlew --no-configuration-cache :quarkus-policy-api-benchmarks:jmh`。

## Gatling 负载测试
- 在 `quarkus-policy-api` 中启用 Gatling 插件，新增 `PolicyEvaluationSimulation` 覆盖 3 种负载曲线 + GraphQL 场景。
- 运行方式：`./gradlew :quarkus-policy-api:gatlingRun -Dgatling.simulationClass=io.aster.policy.simulation.PolicyEvaluationSimulation`，可通过 `POLICY_API_BASE_URL`、`POLICY_TENANT` 调整目标。

## Redis 分布式缓存
- 引入 `io.quarkus:quarkus-redis-cache`，新增 `quarkus.redis.hosts` 配置，docker-compose 中为 API 服务配置 redis host。
- `PolicyCacheManager` 增加 Redis Pub/Sub 通知机制，使用 `policy-cache:invalidate` 渠道同步跨节点失效事件，并在 `docker-compose.yml` 中确保实例依赖 `redis`。
- 通过 `RedisDataSource` 自动订阅，消息格式：`{tenantId, policyModule, policyFunction, hash}`，可对单键或整租户进行失效广播。

## 输出产物
- `quarkus-policy-api-benchmarks/` 模块及中央 JMH 报告。
- Gatling 脚本 `src/gatling/scala/io/aster/policy/simulation/PolicyEvaluationSimulation.scala`。
- `.claude/tasks-11-13-performance-report.md` 综合性能报告。
