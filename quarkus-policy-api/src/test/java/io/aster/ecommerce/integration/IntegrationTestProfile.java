package io.aster.ecommerce.integration;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.List;
import java.util.Map;

/**
 * 集成測試專用 Profile，使用 PostgreSQL Testcontainers。
 */
public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
            Map.entry("quarkus.cache.caffeine.\"policy-results\".expire-after-write", "3M"),
            Map.entry("quarkus.log.category.\"io.aster\".level", "DEBUG")
        );
    }

    @Override
    public List<TestResourceEntry> testResources() {
        // 使用 PostgresTestResource 提供数据库
        return List.of(new TestResourceEntry(io.aster.test.PostgresTestResource.class));
    }
}
