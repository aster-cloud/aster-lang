package io.aster.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

/**
 * WorkflowSchedulerService.processWorkflow() 性能基准测试
 *
 * 测试目标：验证 P0-2 验收标准 - p99 延迟 < 100ms
 *
 * 测试场景：
 * - Simple Workflow: 1 个 task，RUNNING 状态，最小事件集
 *
 * 技术约束：
 * - JMH 运行在独立 JVM，需手动初始化所有依赖
 * - 使用 Testcontainers 提供 PostgreSQL 环境
 * - 使用反射绕过字段注入限制
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class WorkflowSchedulingBenchmark {

    /**
     * Benchmark 方法：测量 processWorkflow 延迟
     *
     * JMH 自动收集 p50, p95, p99 百分位数据
     */
    @Benchmark
    public void processSimpleWorkflow(SimpleWorkflowState state) {
        state.schedulerService.processWorkflow(state.workflowId);
    }

    /**
     * Benchmark 状态类：管理测试环境生命周期
     *
     * Level.Trial: 所有 iterations 共享同一实例（避免重复启动 PostgreSQL）
     */
    @State(Scope.Benchmark)
    public static class SimpleWorkflowState {

        // HikariDataSource (Task 3)
        private HikariDataSource dataSource;

        // JPA EntityManager (Task 5)
        private EntityManagerFactory entityManagerFactory;
        private EntityManager entityManager;

        // 服务依赖 (Task 5)
        private WorkflowSchedulerService schedulerService;

        // 测试数据 (Task 6)
        private String workflowId;

        /**
         * 初始化测试环境
         *
         * 执行顺序：
         * 1. 启动 PostgreSQL (Testcontainers)
         * 2. 创建 DataSource
         * 3. 运行 Flyway 迁移
         * 4. 手动装配服务依赖（使用反射设置字段）
         * 5. 准备测试数据
         */
        @Setup(Level.Trial)
        public void setUp() throws Exception {
            // Task 3: 使用已运行的 PostgreSQL (避免 Testcontainers 环境问题)
            // 连接到本地 PostgreSQL 实例（端口 36197）
            String jdbcUrl = System.getProperty("benchmark.db.url",
                "jdbc:postgresql://localhost:36197/test");
            String username = System.getProperty("benchmark.db.username", "test");
            String password = System.getProperty("benchmark.db.password", "test");

            // 创建 HikariDataSource
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(5);
            dataSource = new HikariDataSource(config);

            // 验证数据库连接（Task 3 验收标准）
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    throw new IllegalStateException("PostgreSQL 连接验证失败");
                }
            }

            // Task 4: 运行 Flyway 数据库迁移
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)  // benchmark 环境允许 clean
                .load();
            flyway.migrate();

            // 验证关键表已创建（Task 4 验收标准）
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT table_name FROM information_schema.tables " +
                     "WHERE table_schema = 'public' AND table_name IN " +
                     "('workflow_state', 'workflow_events', 'workflow_timers')")) {
                int tableCount = 0;
                while (rs.next()) {
                    tableCount++;
                }
                if (tableCount != 3) {
                    throw new IllegalStateException(
                        "Flyway 迁移失败：预期3个表，实际找到 " + tableCount + " 个");
                }
            }

            // Task 5: 手动装配服务依赖
            // 1. 创建 EntityManagerFactory 和 EntityManager
            Map<String, Object> props = new HashMap<>();
            props.put("jakarta.persistence.jdbc.url", jdbcUrl);
            props.put("jakarta.persistence.jdbc.user", username);
            props.put("jakarta.persistence.jdbc.password", password);
            props.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("hibernate.hbm2ddl.auto", "none");  // 使用 Flyway 管理 schema
            props.put("jakarta.persistence.schema-generation.database.action", "none");

            // 简化的 EntityManagerFactory 创建（不使用 persistence.xml）
            entityManagerFactory = jakarta.persistence.Persistence
                .createEntityManagerFactory("workflow-benchmark-pu", props);
            entityManager = entityManagerFactory.createEntityManager();

            // 2. 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            // 3. 装配 PostgresEventStore（使用反射）
            PostgresEventStore eventStore = new PostgresEventStore();
            setField(eventStore, "em", entityManager);
            setField(eventStore, "objectMapper", objectMapper);
            setField(eventStore, "projectionBuilder",
                mock(io.aster.workflow.WorkflowQueryProjectionBuilder.class));
            setField(eventStore, "dbKind", "postgresql");
            setField(eventStore, "snapshotInterval", 100);
            setField(eventStore, "snapshotEnabled", true);

            // 4. 装配 PostgresWorkflowRuntime（使用反射）
            PostgresWorkflowRuntime runtime = new PostgresWorkflowRuntime();
            setField(runtime, "eventStore", eventStore);
            setField(runtime, "objectMapper", objectMapper);
            setField(runtime, "metrics", mock(io.aster.workflow.WorkflowMetrics.class));
            setField(runtime, "policyVersionService",
                mock(io.aster.policy.service.PolicyVersionService.class));
            setField(runtime, "tenantContext", mock(io.aster.policy.tenant.TenantContext.class));
            setField(runtime, "ttlHours", 24);

            // 5. 装配 WorkflowSchedulerService（使用反射）
            schedulerService = new WorkflowSchedulerService();
            setField(schedulerService, "eventStore", eventStore);
            setField(schedulerService, "workflowRuntime", runtime);
            setField(schedulerService, "entityManager", entityManager);
            setField(schedulerService, "objectMapper", objectMapper);
            setField(schedulerService, "compensationExecutor",
                mock(io.aster.workflow.SagaCompensationExecutor.class));
            setField(schedulerService, "businessMetrics",
                mock(io.aster.monitoring.BusinessMetrics.class));
            setField(schedulerService, "dbKind", "postgresql");

            // Task 6: 准备简单 workflow 测试数据
            workflowId = java.util.UUID.randomUUID().toString();

            // 插入 workflow_state 记录（状态为 RUNNING）
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO workflow_state (workflow_id, status, last_event_seq, created_at, updated_at) " +
                     "VALUES (?::uuid, 'RUNNING', 0, NOW(), NOW())")) {
                ps.setString(1, workflowId);
                int inserted = ps.executeUpdate();
                if (inserted != 1) {
                    throw new IllegalStateException(
                        "Failed to insert workflow_state: expected 1 row, got " + inserted);
                }
            }

            // 插入初始事件（简单 task completed 事件）
            String eventPayload = "{\"type\":\"TaskCompleted\",\"taskId\":\"task-1\",\"result\":\"success\"}";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO workflow_events (workflow_id, sequence, event_type, payload, occurred_at) " +
                     "VALUES (?::uuid, 1, 'TaskCompleted', ?::jsonb, NOW())")) {
                ps.setString(1, workflowId);
                ps.setString(2, eventPayload);
                int inserted = ps.executeUpdate();
                if (inserted != 1) {
                    throw new IllegalStateException(
                        "Failed to insert workflow_events: expected 1 row, got " + inserted);
                }
            }

            // 验证数据已插入（Task 6 验收标准）
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM workflow_state WHERE workflow_id = ?::uuid")) {
                ps.setString(1, workflowId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 1) {
                        throw new IllegalStateException(
                            "Verification failed: workflow_state record not found");
                    }
                }
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM workflow_events WHERE workflow_id = ?::uuid")) {
                ps.setString(1, workflowId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 1) {
                        throw new IllegalStateException(
                            "Verification failed: workflow_events record not found");
                    }
                }
            }
        }

        /**
         * 清理测试环境
         *
         * 释放资源：DataSource, PostgreSQL 容器
         */
        @TearDown(Level.Trial)
        public void tearDown() {
            // Task 5: 清理 EntityManager
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
            }

            // Task 3: 清理 DataSource
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        }

        /**
         * 反射工具方法：设置私有字段
         *
         * 用于绕过 @Inject 字段注入，手动装配依赖
         */
        private void setField(Object target, String fieldName, Object value) throws Exception {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
    }
}
