package io.aster.ecommerce.integration;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * 集成測試專用 Profile，使用 H2 + Flyway，並保留排程服務。
 */
public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
            Map.entry("quarkus.datasource.db-kind", "h2"),
            Map.entry("quarkus.datasource.jdbc.url", "jdbc:h2:mem:order-workflow-it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"),
            Map.entry("quarkus.datasource.username", "sa"),
            Map.entry("quarkus.datasource.password", ""),
            Map.entry("quarkus.hibernate-orm.database.generation", "none"),
            Map.entry("quarkus.hibernate-orm.sql-load-script", "no-file"),
            Map.entry("quarkus.flyway.migrate-at-start", "true"),
            Map.entry("quarkus.flyway.clean-at-start", "true"),
            Map.entry("quarkus.flyway.baseline-on-migrate", "true"),
            Map.entry("quarkus.flyway.baseline-version", "0"),
            Map.entry("quarkus.flyway.locations", "classpath:db/migration"),
            Map.entry("quarkus.cache.caffeine.\"policy-results\".expire-after-write", "3M"),
            Map.entry("quarkus.log.category.\"io.aster\".level", "DEBUG")
        );
    }
}
