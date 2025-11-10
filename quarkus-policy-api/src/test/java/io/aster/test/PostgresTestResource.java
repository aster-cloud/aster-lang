package io.aster.test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.util.Map;

/**
 * PostgreSQL Testcontainers 测试资源（Phase 3.4）
 *
 * 提供生产环境一致的 PostgreSQL 测试环境，解决 H2 兼容性问题。
 * 使用容器复用（reuse=true）加速重复测试。
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true)  // 共享容器，加速测试
        .withStartupTimeout(Duration.ofSeconds(60));  // 容器启动超时（拉取镜像时需要）

    @Override
    public Map<String, String> start() {
        postgres.start();
        return Map.of(
            "quarkus.datasource.jdbc.url", postgres.getJdbcUrl(),
            "quarkus.datasource.username", postgres.getUsername(),
            "quarkus.datasource.password", postgres.getPassword(),
            "quarkus.datasource.db-kind", "postgresql"
        );
    }

    @Override
    public void stop() {
        // 容器自动停止（reuse=true 时跳过）
    }
}
