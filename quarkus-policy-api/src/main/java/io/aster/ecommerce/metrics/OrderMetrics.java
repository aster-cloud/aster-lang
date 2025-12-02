package io.aster.ecommerce.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;

/**
 * 订单 API 指标收集器。
 *
 * 负责记录订单提交与状态查询的耗时与调用次数，便于监控履约链路。
 */
@ApplicationScoped
public class OrderMetrics {

    @Inject
    MeterRegistry registry;

    private static final String SUBMISSION_TIMER = "order_submission_duration_ms";
    private static final String SUBMISSION_COUNTER = "order_submission_total";
    private static final String STATUS_TIMER = "order_status_query_duration_ms";
    private static final String STATUS_COUNTER = "order_status_query_total";

    @PostConstruct
    void initMeters() {
        // 预注册基础指标，避免首次请求时延迟创建
        registry.timer(SUBMISSION_TIMER);
        registry.timer(STATUS_TIMER);
    }

    /**
     * 记录订单提交耗时与成功率。
     *
     * @param tenantId  租户标识
     * @param durationMs 耗时（毫秒）
     * @param success   是否提交成功
     */
    public void recordOrderSubmission(String tenantId, long durationMs, boolean success) {
        recordDuration(SUBMISSION_TIMER, "submit", tenantId, durationMs, success);
        recordCounter(SUBMISSION_COUNTER, "submit", tenantId, success);
    }

    /**
     * 记录订单状态查询耗时。
     *
     * @param tenantId  租户标识
     * @param durationMs 耗时（毫秒）
     * @param success   查询是否成功
     */
    public void recordOrderStatusQuery(String tenantId, long durationMs, boolean success) {
        recordDuration(STATUS_TIMER, "status", tenantId, durationMs, success);
        recordCounter(STATUS_COUNTER, "status", tenantId, success);
    }

    private void recordDuration(String timerName, String operation, String tenantId, long durationMs, boolean success) {
        Timer.builder(timerName)
            .description("订单 API 耗时")
            .tag("operation", operation)
            .tag("status", success ? "success" : "error")
            .tag("tenant", normalizeTenant(tenantId))
            .register(registry)
            .record(Duration.ofMillis(Math.max(durationMs, 0)));
    }

    private void recordCounter(String counterName, String operation, String tenantId, boolean success) {
        Counter.builder(counterName)
            .description("订单 API 调用次数")
            .tag("operation", operation)
            .tag("status", success ? "success" : "error")
            .tag("tenant", normalizeTenant(tenantId))
            .register(registry)
            .increment();
    }

    private String normalizeTenant(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }
}
