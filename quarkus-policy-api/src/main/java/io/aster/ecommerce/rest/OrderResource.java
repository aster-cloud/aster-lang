package io.aster.ecommerce.rest;

import aster.runtime.workflow.WorkflowEvent;
import aster.runtime.workflow.WorkflowMetadata;
import aster.runtime.workflow.WorkflowState;
import io.aster.ecommerce.metrics.OrderMetrics;
import io.aster.ecommerce.rest.model.OrderRequest;
import io.aster.ecommerce.rest.model.OrderResponse;
import io.aster.ecommerce.rest.model.OrderStatusResponse;
import io.aster.policy.event.AuditEvent;
import io.aster.workflow.PostgresEventStore;
import io.aster.workflow.PostgresWorkflowRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 订单 REST 资源。
 *
 * 提供订单提交与状态查询接口，封装 workflow 调度、多租户、审计与指标逻辑。
 */
@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    private static final Logger LOG = Logger.getLogger(OrderResource.class);
    private static final String WORKFLOW_MODULE = "io.aster.ecommerce.order_fulfillment";
    private static final String WORKFLOW_FUNCTION = "fulfillOrder";
    private static final Set<String> AUDIT_METADATA_WHITELIST = Set.of(
        "channel",
        "campaignId",
        "couponCode",
        "priority",
        "shippingMethod",
        "notes",
        "itemAttributes"
    );
    private static final Set<String> AUDIT_METADATA_RESERVED = Set.of(
        "tenantId",
        "status",
        "workflowId",
        "performedBy",
        "orderId"
    );

    @Inject
    PostgresWorkflowRuntime workflowRuntime;

    @Inject
    PostgresEventStore eventStore;

    @Inject
    OrderMetrics orderMetrics;

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    Event<AuditEvent> auditEventPublisher;

    @Context
    RoutingContext routingContext;

    /**
     * 提交订单并调度履约 workflow。
     */
    @POST
    @Blocking
    public Uni<OrderResponse> submitOrder(@Valid OrderRequest request) {
        final String tenantId = tenantId();
        final String performedBy = performedBy();
        final String workflowId = resolveWorkflowId(tenantId, request.orderId());
        final long startTime = System.currentTimeMillis();
        final WorkflowMetadata workflowMetadata = buildWorkflowMetadata(tenantId, performedBy, request);

        LOG.infof("租户 %s 提交订单 %s，准备调度 workflow %s", tenantId, request.orderId(), workflowId);

        return Uni.createFrom().item(() ->
            workflowRuntime.schedule(workflowId, request.orderId(), workflowMetadata)
        )
        .onItem().transform(handle -> {
            long duration = System.currentTimeMillis() - startTime;
            orderMetrics.recordOrderSubmission(tenantId, duration, true);
            incrementApiCounter("submit", "success");
            publishOrderSubmissionEvent(tenantId, performedBy, request, workflowId, duration, null);
            LOG.infof("订单 %s 调度完成，workflowId=%s", request.orderId(), workflowId);
            return OrderResponse.success(request.orderId(), workflowId);
        })
        .onFailure().recoverWithItem(throwable -> {
            long duration = System.currentTimeMillis() - startTime;
            orderMetrics.recordOrderSubmission(tenantId, duration, false);
            incrementApiCounter("submit", "error");
            publishOrderSubmissionEvent(tenantId, performedBy, request, workflowId, duration, throwable.getMessage());
            LOG.errorf(throwable, "订单 %s 提交失败（租户 %s）", request.orderId(), tenantId);
            return OrderResponse.error(request.orderId(), throwable.getMessage());
        });
    }

    /**
     * 查询订单当前状态。
     */
    @GET
    @Path("/{orderId}/status")
    @Blocking
    public Uni<OrderStatusResponse> getOrderStatus(@PathParam("orderId") String orderId) {
        final String tenantId = tenantId();
        final String performedBy = performedBy();
        final String workflowId = resolveWorkflowId(tenantId, orderId);
        final long startTime = System.currentTimeMillis();

        return Uni.createFrom().item(() -> {
            WorkflowState state = eventStore.getState(workflowId)
                .orElseThrow(() -> new NotFoundException("未找到订单: " + orderId));
            List<WorkflowEvent> events = eventStore.getEvents(workflowId, 0);
            return new OrderStatusAggregate(state, events);
        })
        .onItem().transform(aggregate -> {
            long duration = System.currentTimeMillis() - startTime;
            orderMetrics.recordOrderStatusQuery(tenantId, duration, true);
            incrementApiCounter("status", "success");
            publishOrderStatusEvent(
                tenantId,
                performedBy,
                orderId,
                workflowId,
                aggregate.state().getStatus().name(),
                duration,
                null,
                aggregate.events().size()
            );

            long lastUpdated = aggregate.state().getUpdatedAt() == null
                ? System.currentTimeMillis()
                : aggregate.state().getUpdatedAt().toEpochMilli();

            return new OrderStatusResponse(
                orderId,
                workflowId,
                aggregate.state().getStatus().name(),
                List.copyOf(aggregate.events()),
                lastUpdated
            );
        })
        .onFailure().invoke(throwable -> {
            long duration = System.currentTimeMillis() - startTime;
            orderMetrics.recordOrderStatusQuery(tenantId, duration, false);
            incrementApiCounter("status", "error");
            publishOrderStatusEvent(
                tenantId,
                performedBy,
                orderId,
                workflowId,
                "UNKNOWN",
                duration,
                throwable.getMessage(),
                0
            );
        });
    }

    private WorkflowMetadata buildWorkflowMetadata(String tenantId, String performedBy, OrderRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("workflowModule", WORKFLOW_MODULE);
        metadata.put("workflowFunction", WORKFLOW_FUNCTION);
        metadata.put("tenantId", tenantId);
        metadata.put("performedBy", performedBy);
        metadata.put("orderId", request.orderId());
        metadata.put("customerId", request.customerId());
        metadata.put("itemCount", request.items().size());
        metadata.put("items", request.items());
        metadata.put("submittedAt", Instant.now().toString());
        metadata.put("metadata", request.metadata() == null ? Collections.emptyMap() : request.metadata());
        return new WorkflowMetadata(metadata);
    }

    private void publishOrderSubmissionEvent(
        String tenantId,
        String performedBy,
        OrderRequest request,
        String workflowId,
        long duration,
        String error
    ) {
        if (auditEventPublisher == null) {
            return;
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customerId", request.customerId());
        metadata.put("itemCount", request.items().size());
        metadata.put("tenantId", tenantId);
        metadata.put("status", error == null ? "scheduled" : "failed");
        Map<String, Object> businessMetadata = extractAuditMetadata(request);
        businessMetadata.forEach((key, value) -> {
            if (!AUDIT_METADATA_RESERVED.contains(key) && !metadata.containsKey(key)) {
                metadata.put(key, value);
            }
        });
        auditEventPublisher.fireAsync(
            AuditEvent.orderSubmission(
                tenantId,
                WORKFLOW_MODULE,
                WORKFLOW_FUNCTION,
                request.orderId(),
                workflowId,
                performedBy,
                error == null,
                duration,
                error,
                metadata
            )
        );
    }

    private void publishOrderStatusEvent(
        String tenantId,
        String performedBy,
        String orderId,
        String workflowId,
        String workflowStatus,
        long duration,
        String error,
        int eventCount
    ) {
        if (auditEventPublisher == null) {
            return;
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("workflowStatus", workflowStatus);
        metadata.put("eventCount", eventCount);
        metadata.put("tenantId", tenantId);
        auditEventPublisher.fireAsync(
            AuditEvent.orderStatusQuery(
                tenantId,
                WORKFLOW_MODULE,
                WORKFLOW_FUNCTION,
                orderId,
                workflowId,
                performedBy,
                workflowStatus,
                error == null,
                duration,
                error,
                metadata
            )
        );
    }

    private void incrementApiCounter(String operation, String status) {
        meterRegistry.counter("order_api_requests_total", "operation", operation, "status", status)
            .increment();
    }

    private Map<String, Object> extractAuditMetadata(OrderRequest request) {
        Map<String, Object> requestMetadata = request.metadata();
        if (requestMetadata == null || requestMetadata.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> sanitized = new HashMap<>();
        requestMetadata.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            boolean allowedKey = AUDIT_METADATA_WHITELIST.contains(key) || key.startsWith("item.");
            if (allowedKey) {
                sanitized.put(key, value);
            }
        });
        return sanitized.isEmpty() ? Collections.emptyMap() : sanitized;
    }

    private String tenantId() {
        if (routingContext == null || routingContext.request() == null) {
            return "default";
        }
        String tenant = routingContext.request().getHeader("X-Tenant-Id");
        return tenant == null || tenant.isBlank() ? "default" : tenant.trim();
    }

    private String performedBy() {
        if (routingContext == null || routingContext.request() == null) {
            return "anonymous";
        }
        String user = routingContext.request().getHeader("X-User-Id");
        return user == null || user.isBlank() ? "anonymous" : user.trim();
    }

    private String resolveWorkflowId(String tenantId, String orderId) {
        String source = (tenantId == null ? "default" : tenantId) + ":" + (orderId == null ? "" : orderId);
        UUID uuid = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
        return uuid.toString();
    }

    private record OrderStatusAggregate(WorkflowState state, List<WorkflowEvent> events) { }
}
