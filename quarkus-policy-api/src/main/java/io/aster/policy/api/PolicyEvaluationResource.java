package io.aster.policy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.*;

/**
 * REST API for policy evaluation (Reactive)
 *
 * 提供策略评估的REST端点，支持动态调用编译后的Aster策略
 * 使用Mutiny的Uni实现reactive REST endpoints
 */
@Path("/api/policies")
@Tag(name = "Policy Evaluation", description = "Evaluate policies against context data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PolicyEvaluationResource {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    PolicyEvaluationService policyEvaluationService;

    /**
     * 评估策略（带缓存，reactive版本）
     *
     * @param request 包含策略名称和上下文数据的请求
     * @return Uni包装的评估结果
     */
    @POST
    @Path("/evaluate")
    @Operation(
        summary = "Evaluate a policy",
        description = "Evaluates a specific policy against provided context data (reactive)"
    )
    @APIResponse(
        responseCode = "200",
        description = "Policy evaluation successful",
        content = @Content(schema = @Schema(implementation = PolicyEvaluationResponse.class))
    )
    @APIResponse(
        responseCode = "400",
        description = "Invalid request"
    )
    @APIResponse(
        responseCode = "404",
        description = "Policy not found"
    )
    @APIResponse(
        responseCode = "500",
        description = "Policy evaluation failed"
    )
    public Uni<Response> evaluatePolicy(PolicyEvaluationRequest request) {
        // 验证请求
        if (request == null || request.policyModule == null || request.policyFunction == null) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required fields: policyModule, policyFunction"))
                    .build()
            );
        }

        // 准备上下文参数数组
        Object[] contextArray = request.context != null
            ? request.context.toArray()
            : new Object[0];

        long startTime = System.nanoTime();

        // 使用缓存服务评估策略（reactive）
        return policyEvaluationService.evaluatePolicy(
                request.policyModule,
                request.policyFunction,
                contextArray
            )
            .onItem().transform(evalResult -> {
                long totalDurationNanos = System.nanoTime() - startTime;

                // 构建响应
                PolicyEvaluationResponse response = new PolicyEvaluationResponse();
                response.result = evalResult.getResult();
                response.executionTimeMs = totalDurationNanos / 1_000_000.0;
                response.policyModule = request.policyModule;
                response.policyFunction = request.policyFunction;
                response.timestamp = System.currentTimeMillis();
                response.fromCache = evalResult.isFromCache();
                response.policyExecutionTimeMs = evalResult.getExecutionTimeMs();

                return Response.ok(response).build();
            })
            .onFailure(throwable -> {
                // Check if the root cause is ClassNotFoundException
                Throwable cause = throwable;
                while (cause != null) {
                    if (cause instanceof ClassNotFoundException) {
                        return true;
                    }
                    cause = cause.getCause();
                }
                return false;
            }).recoverWithItem(e ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Policy not found: " + request.policyModule + "." + request.policyFunction))
                    .build()
            )
            .onFailure().recoverWithItem(e ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Policy evaluation failed", "details", e.getMessage()))
                    .build()
            );
    }

    /**
     * 列出所有可用策略
     *
     * @return 策略列表
     */
    @GET
    @Path("/list")
    @Operation(
        summary = "List available policies",
        description = "Returns a list of all available compiled policies"
    )
    @APIResponse(
        responseCode = "200",
        description = "Policy list retrieved successfully"
    )
    public Response listPolicies() {
        // 返回已知策略列表（aster-finance库）
        List<PolicyInfo> policies = Arrays.asList(
            new PolicyInfo("aster.finance.loan", "evaluateLoanEligibility", "Evaluate loan application eligibility"),
            new PolicyInfo("aster.finance.loan", "determineInterestRateBps", "Determine interest rate based on credit score"),
            new PolicyInfo("aster.finance.fraud", "detectFraud", "Detect fraudulent transactions based on amount, history and account age"),
            new PolicyInfo("aster.finance.risk", "assessRisk", "Assess financial risk based on credit score, income and debt")
        );

        return Response.ok(Map.of(
            "policies", policies,
            "count", policies.size(),
            "timestamp", System.currentTimeMillis()
        )).build();
    }

    /**
     * 健康检查
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Health check",
        description = "Check if policy evaluation service is healthy"
    )
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "aster-policy-api",
            "timestamp", System.currentTimeMillis()
        )).build();
    }

    // 内部数据类
    public static class PolicyEvaluationRequest {
        public String policyModule;
        public String policyFunction;
        public List<Object> context;
    }

    public static class PolicyEvaluationResponse {
        public Object result;
        public double executionTimeMs;
        public String policyModule;
        public String policyFunction;
        public long timestamp;
        public boolean fromCache;
        public double policyExecutionTimeMs;
    }

    public static class PolicyInfo {
        public String module;
        public String function;
        public String description;

        public PolicyInfo(String module, String function, String description) {
            this.module = module;
            this.function = function;
            this.description = description;
        }
    }

    /**
     * 批量评估多个策略（reactive并行执行）
     *
     * @param batchRequest 包含多个策略评估请求的批次
     * @return Uni包装的批量评估结果
     */
    @POST
    @Path("/evaluate/batch")
    @Operation(
        summary = "Batch evaluate policies",
        description = "Evaluates multiple policies in parallel (reactive)"
    )
    @APIResponse(
        responseCode = "200",
        description = "Batch evaluation successful"
    )
    @APIResponse(
        responseCode = "400",
        description = "Invalid batch request"
    )
    public Uni<Response> evaluateBatch(BatchEvaluationRequest batchRequest) {
        if (batchRequest == null || batchRequest.requests == null || batchRequest.requests.isEmpty()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Batch request cannot be empty"))
                    .build()
            );
        }

        long startTime = System.nanoTime();

        // 转换为服务层的BatchRequest列表
        java.util.List<PolicyEvaluationService.BatchRequest> serviceRequests = batchRequest.requests.stream()
            .map(req -> new PolicyEvaluationService.BatchRequest(
                req.policyModule,
                req.policyFunction,
                req.context != null ? req.context.toArray() : new Object[0]
            ))
            .collect(java.util.stream.Collectors.toList());

        return policyEvaluationService.evaluateBatch(serviceRequests)
            .onItem().transform(results -> {
                long totalDurationNanos = System.nanoTime() - startTime;

                // 构建批量响应
                java.util.List<PolicyEvaluationResponse> responses = new java.util.ArrayList<>();
                for (int i = 0; i < results.size(); i++) {
                    var evalResult = results.get(i);
                    var originalReq = batchRequest.requests.get(i);

                    PolicyEvaluationResponse response = new PolicyEvaluationResponse();
                    response.result = evalResult.getResult();
                    response.executionTimeMs = evalResult.getExecutionTimeMs();
                    response.policyModule = originalReq.policyModule;
                    response.policyFunction = originalReq.policyFunction;
                    response.timestamp = System.currentTimeMillis();
                    response.fromCache = evalResult.isFromCache();
                    response.policyExecutionTimeMs = evalResult.getExecutionTimeMs();

                    responses.add(response);
                }

                return Response.ok(Map.of(
                    "results", responses,
                    "count", responses.size(),
                    "totalExecutionTimeMs", totalDurationNanos / 1_000_000.0,
                    "timestamp", System.currentTimeMillis()
                )).build();
            })
            .onFailure().recoverWithItem(e ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Batch evaluation failed", "details", e.getMessage()))
                    .build()
            );
    }

    /**
     * 清空策略缓存（reactive版本）
     *
     * @return Uni包装的操作结果
     */
    @DELETE
    @Path("/cache")
    @Operation(
        summary = "Clear policy cache",
        description = "Clears all cached policy evaluation results (reactive)"
    )
    @APIResponse(
        responseCode = "200",
        description = "Cache cleared successfully"
    )
    public Uni<Response> clearCache() {
        return policyEvaluationService.clearAllCache()
            .onItem().transform(v -> Response.ok(Map.of(
                "status", "success",
                "message", "Policy cache cleared",
                "timestamp", System.currentTimeMillis()
            )).build());
    }

    /**
     * 使特定策略的缓存失效（reactive版本）
     *
     * @param request 包含策略名称和上下文的请求
     * @return Uni包装的操作结果
     */
    @DELETE
    @Path("/cache/invalidate")
    @Operation(
        summary = "Invalidate specific policy cache",
        description = "Invalidates cache for a specific policy and context (reactive)"
    )
    @APIResponse(
        responseCode = "200",
        description = "Cache invalidated successfully"
    )
    public Uni<Response> invalidateCache(PolicyEvaluationRequest request) {
        if (request == null || request.policyModule == null || request.policyFunction == null) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required fields: policyModule, policyFunction"))
                    .build()
            );
        }

        Object[] contextArray = request.context != null
            ? request.context.toArray()
            : new Object[0];

        return policyEvaluationService.invalidateCache(
                request.policyModule,
                request.policyFunction,
                contextArray
            )
            .onItem().transform(v -> Response.ok(Map.of(
                "status", "success",
                "message", "Cache invalidated for " + request.policyModule + "." + request.policyFunction,
                "timestamp", System.currentTimeMillis()
            )).build());
    }

    /**
     * 批量评估请求数据类
     */
    public static class BatchEvaluationRequest {
        public List<PolicyEvaluationRequest> requests;
    }
}
