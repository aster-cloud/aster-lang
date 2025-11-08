package io.aster.policy.rest;

import io.aster.policy.entity.AuditLog;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

/**
 * 审计日志查询 API
 *
 * 提供审计日志的查询功能，用于合规审计和事后调查：
 * - 按租户查询
 * - 按事件类型查询
 * - 按策略查询
 * - 按时间范围查询
 *
 * 注意：所有API都通过 X-Tenant-Id 实现多租户隔离
 */
@Path("/api/audit")
@Produces(MediaType.APPLICATION_JSON)
public class AuditLogResource {

    private static final Logger LOG = Logger.getLogger(AuditLogResource.class);

    @Context
    RoutingContext routingContext;

    /**
     * 查询指定租户的所有审计日志
     *
     * GET /api/audit
     * Headers: X-Tenant-Id (optional, defaults to "default")
     */
    @GET
    @io.smallrye.common.annotation.Blocking
    public Uni<List<AuditLog>> getAllLogs() {
        String tenantId = tenantId();
        LOG.infof("Fetching all audit logs for tenant: %s", tenantId);

        return Uni.createFrom().item(() -> AuditLog.findByTenant(tenantId));
    }

    /**
     * 查询指定事件类型的审计日志
     *
     * GET /api/audit/type/{eventType}
     * Headers: X-Tenant-Id (optional, defaults to "default")
     */
    @GET
    @Path("/type/{eventType}")
    @io.smallrye.common.annotation.Blocking
    public Uni<List<AuditLog>> getLogsByEventType(@PathParam("eventType") String eventType) {
        String tenantId = tenantId();
        LOG.infof("Fetching audit logs by event type: %s for tenant: %s", eventType, tenantId);

        return Uni.createFrom().item(() -> AuditLog.findByEventType(eventType, tenantId));
    }

    /**
     * 查询指定策略的审计日志
     *
     * GET /api/audit/policy/{policyModule}/{policyFunction}
     * Headers: X-Tenant-Id (optional, defaults to "default")
     */
    @GET
    @Path("/policy/{policyModule}/{policyFunction}")
    @io.smallrye.common.annotation.Blocking
    public Uni<List<AuditLog>> getLogsByPolicy(
        @PathParam("policyModule") String policyModule,
        @PathParam("policyFunction") String policyFunction
    ) {
        String tenantId = tenantId();
        LOG.infof("Fetching audit logs for policy: %s.%s (tenant: %s)",
            policyModule, policyFunction, tenantId);

        return Uni.createFrom().item(() ->
            AuditLog.findByPolicy(policyModule, policyFunction, tenantId));
    }

    /**
     * 查询指定时间范围的审计日志
     *
     * GET /api/audit/range?startTime={ISO8601}&endTime={ISO8601}
     * Headers: X-Tenant-Id (optional, defaults to "default")
     */
    @GET
    @Path("/range")
    @io.smallrye.common.annotation.Blocking
    public Uni<List<AuditLog>> getLogsByTimeRange(
        @QueryParam("startTime") String startTimeStr,
        @QueryParam("endTime") String endTimeStr
    ) {
        String tenantId = tenantId();
        LOG.infof("Fetching audit logs for time range: %s to %s (tenant: %s)",
            startTimeStr, endTimeStr, tenantId);

        try {
            Instant startTime = Instant.parse(startTimeStr);
            Instant endTime = Instant.parse(endTimeStr);

            return Uni.createFrom().item(() ->
                AuditLog.findByTimeRange(startTime, endTime, tenantId));
        } catch (Exception e) {
            LOG.errorf(e, "Invalid time format: startTime=%s, endTime=%s", startTimeStr, endTimeStr);
            throw new BadRequestException("Invalid time format. Use ISO8601 format (e.g., 2024-01-01T00:00:00Z)");
        }
    }

    /**
     * 提取租户ID
     *
     * 从 X-Tenant-Id 请求头提取租户ID，如果不存在则返回 "default"
     */
    private String tenantId() {
        if (routingContext == null || routingContext.request() == null) {
            return "default";
        }
        String tenant = routingContext.request().getHeader("X-Tenant-Id");
        return tenant == null || tenant.isBlank() ? "default" : tenant.trim();
    }
}
