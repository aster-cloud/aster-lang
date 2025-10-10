package io.aster.policy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
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
    @Schema(description = "策略评估请求 / Policy evaluation request")
    public static class PolicyEvaluationRequest {
        @Schema(
            description = "策略模块名称 (Java package name) / Policy module name",
            required = true
        )
        public String policyModule;

        @Schema(
            description = "策略函数名称 / Policy function name",
            required = true
        )
        public String policyFunction;

        @Schema(
            description = "上下文参数列表，将按顺序传递给策略函数 / Context parameters passed to policy function"
        )
        public List<Object> context;
    }

    @Schema(description = "策略评估响应 / Policy evaluation response")
    public static class PolicyEvaluationResponse {
        @Schema(description = "策略执行结果 / Policy execution result")
        public Object result;

        @Schema(description = "总执行时间（毫秒），包含缓存查找 / Total execution time in ms including cache lookup")
        public double executionTimeMs;

        @Schema(description = "策略模块名称 / Policy module name")
        public String policyModule;

        @Schema(description = "策略函数名称 / Policy function name")
        public String policyFunction;

        @Schema(description = "响应时间戳 / Response timestamp")
        public long timestamp;

        @Schema(description = "结果是否来自缓存 / Whether result came from cache")
        public boolean fromCache;

        @Schema(description = "策略实际执行时间（毫秒） / Actual policy execution time in ms")
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
     * 批量评估多个策略（reactive并行执行，任一失败则全部失败）
     *
     * @param batchRequest 包含多个策略评估请求的批次
     * @return Uni包装的批量评估结果
     */
    @POST
    @Path("/evaluate/batch")
    @Operation(
        summary = "Batch evaluate policies (fail-fast)",
        description = "Evaluates multiple policies in parallel. If any fails, the entire batch fails (reactive)"
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
     * 批量评估多个策略（收集部分成功结果，不会因为单个失败而中断）
     *
     * @param batchRequest 包含多个策略评估请求的批次
     * @return Uni包装的批量评估结果（包含成功和失败的结果）
     */
    @POST
    @Path("/evaluate/batch/partial")
    @Operation(
        summary = "Batch evaluate policies with partial results",
        description = "Evaluates multiple policies in parallel. Collects both successful and failed results. " +
                      "Unlike the fail-fast batch endpoint, this continues execution even if some policies fail."
    )
    @RequestBody(
        description = "Batch evaluation request with multiple policy requests",
        required = true,
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = BatchEvaluationRequest.class),
            examples = @ExampleObject(
                name = "Evaluate multiple policies",
                value = """
                {
                  "requests": [
                    {
                      "policyModule": "aster.finance.loan",
                      "policyFunction": "evaluateLoanEligibility",
                      "context": [750, 50000.0, 3.5]
                    },
                    {
                      "policyModule": "aster.finance.fraud",
                      "policyFunction": "detectFraud",
                      "context": [10000.0, 2, 1.5]
                    },
                    {
                      "policyModule": "aster.finance.risk",
                      "policyFunction": "assessRisk",
                      "context": [650, 40000.0, 15000.0]
                    }
                  ]
                }
                """
            )
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Batch evaluation completed (may contain failures)",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(
                value = """
                {
                  "successes": [
                    {
                      "index": 0,
                      "policyModule": "aster.finance.loan",
                      "policyFunction": "evaluateLoanEligibility",
                      "result": {
                        "result": true,
                        "executionTimeMs": 5.2,
                        "fromCache": false
                      },
                      "error": null
                    },
                    {
                      "index": 1,
                      "policyModule": "aster.finance.fraud",
                      "policyFunction": "detectFraud",
                      "result": {
                        "result": false,
                        "executionTimeMs": 3.8,
                        "fromCache": false
                      },
                      "error": null
                    }
                  ],
                  "failures": [
                    {
                      "index": 2,
                      "policyModule": "aster.finance.risk",
                      "policyFunction": "assessRisk",
                      "result": null,
                      "error": "Policy execution failed: Invalid parameters"
                    }
                  ],
                  "successCount": 2,
                  "failureCount": 1,
                  "totalCount": 3,
                  "totalExecutionTimeMs": 15.3,
                  "timestamp": 1704067200000
                }
                """
            )
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Invalid batch request"
    )
    public Uni<Response> evaluateBatchPartial(BatchEvaluationRequest batchRequest) {
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

        return policyEvaluationService.evaluateBatchWithFailures(serviceRequests)
            .onItem().transform(batchResult -> {
                long totalDurationNanos = System.nanoTime() - startTime;

                return Response.ok(Map.of(
                    "successes", batchResult.getSuccesses(),
                    "failures", batchResult.getFailures(),
                    "successCount", batchResult.getSuccessCount(),
                    "failureCount", batchResult.getFailureCount(),
                    "totalCount", batchResult.getTotalCount(),
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
    @Schema(description = "批量策略评估请求 / Batch policy evaluation request")
    public static class BatchEvaluationRequest {
        @Schema(
            description = "要评估的策略请求列表 / List of policy requests to evaluate",
            required = true
        )
        public List<PolicyEvaluationRequest> requests;
    }

    /**
     * 策略组合执行（顺序执行多个策略）
     *
     * @param compositionRequest 包含策略步骤和初始上下文的请求
     * @return Uni包装的组合执行结果
     */
    @POST
    @Path("/evaluate/composition")
    @Operation(
        summary = "Compose and execute policies in sequence",
        description = "Executes multiple policies in sequence, optionally using the result of one as input to the next. " +
                      "Useful for complex business logic that requires multiple policy evaluations with data flowing between them."
    )
    @RequestBody(
        description = "Policy composition request with sequential steps",
        required = true,
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PolicyCompositionRequest.class),
            examples = @ExampleObject(
                name = "Risk assessment followed by fraud detection",
                value = """
                {
                  "steps": [
                    {
                      "policyModule": "aster.finance.risk",
                      "policyFunction": "assessRisk",
                      "useResultAsInput": false
                    },
                    {
                      "policyModule": "aster.finance.fraud",
                      "policyFunction": "detectFraud",
                      "useResultAsInput": false
                    }
                  ],
                  "initialContext": [750, 50000.0, 5]
                }
                """
            )
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Composition evaluation successful",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(
                value = """
                {
                  "finalResult": false,
                  "stepResults": [
                    {
                      "policyModule": "aster.finance.risk",
                      "policyFunction": "assessRisk",
                      "result": "HIGH",
                      "executionTimeMs": 5.2,
                      "stepIndex": 0
                    },
                    {
                      "policyModule": "aster.finance.fraud",
                      "policyFunction": "detectFraud",
                      "result": false,
                      "executionTimeMs": 3.8,
                      "stepIndex": 1
                    }
                  ],
                  "totalExecutionTimeMs": 12.5,
                  "timestamp": 1704067200000
                }
                """
            )
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Invalid composition request"
    )
    public Uni<Response> evaluateComposition(PolicyCompositionRequest compositionRequest) {
        if (compositionRequest == null || compositionRequest.steps == null || compositionRequest.steps.isEmpty()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Composition steps cannot be empty"))
                    .build()
            );
        }

        // 转换为服务层的CompositionStep列表
        java.util.List<PolicyEvaluationService.CompositionStep> serviceSteps = compositionRequest.steps.stream()
            .map(step -> new PolicyEvaluationService.CompositionStep(
                step.policyModule,
                step.policyFunction,
                step.useResultAsInput != null && step.useResultAsInput
            ))
            .collect(java.util.stream.Collectors.toList());

        Object[] initialContext = compositionRequest.initialContext != null
            ? compositionRequest.initialContext.toArray()
            : new Object[0];

        long startTime = System.nanoTime();

        return policyEvaluationService.evaluateComposition(serviceSteps, initialContext)
            .onItem().transform(result -> {
                long totalDurationNanos = System.nanoTime() - startTime;

                return Response.ok(Map.of(
                    "finalResult", result.getFinalResult(),
                    "stepResults", result.getStepResults(),
                    "totalExecutionTimeMs", totalDurationNanos / 1_000_000.0,
                    "timestamp", System.currentTimeMillis()
                )).build();
            })
            .onFailure().recoverWithItem(e ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Composition evaluation failed", "details", e.getMessage()))
                    .build()
            );
    }

    /**
     * 验证策略是否存在并可以加载
     *
     * @param validationRequest 包含策略模块和函数名的请求
     * @return Uni包装的验证结果
     */
    @POST
    @Path("/validate")
    @Operation(
        summary = "Validate a policy",
        description = "Checks if a policy exists, can be loaded, and returns its signature information including parameters and return type. " +
                      "Useful for validating policy availability before evaluation."
    )
    @RequestBody(
        description = "Policy validation request with module and function name",
        required = true,
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PolicyValidationRequest.class),
            examples = @ExampleObject(
                name = "Validate loan eligibility policy",
                value = """
                {
                  "policyModule": "aster.finance.loan",
                  "policyFunction": "evaluateLoanEligibility"
                }
                """
            )
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Policy validation completed",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(
                value = """
                {
                  "valid": true,
                  "message": "Policy exists and is valid",
                  "policyModule": "aster.finance.loan",
                  "policyFunction": "evaluateLoanEligibility",
                  "parameters": [
                    {
                      "name": "creditScore",
                      "type": "int",
                      "fullTypeName": "int"
                    },
                    {
                      "name": "loanAmount",
                      "type": "double",
                      "fullTypeName": "double"
                    },
                    {
                      "name": "employmentYears",
                      "type": "double",
                      "fullTypeName": "double"
                    }
                  ],
                  "returnType": "boolean",
                  "returnTypeFullName": "boolean"
                }
                """
            )
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Invalid validation request"
    )
    public Uni<Response> validatePolicy(PolicyValidationRequest validationRequest) {
        if (validationRequest == null || validationRequest.policyModule == null ||
            validationRequest.policyFunction == null) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required fields: policyModule, policyFunction"))
                    .build()
            );
        }

        return policyEvaluationService.validatePolicy(
                validationRequest.policyModule,
                validationRequest.policyFunction
            )
            .onItem().transform(result -> Response.ok(result).build());
    }

    /**
     * 策略组合请求数据类
     */
    @Schema(description = "策略组合请求 / Policy composition request")
    public static class PolicyCompositionRequest {
        @Schema(
            description = "要顺序执行的策略步骤列表 / List of policy steps to execute in sequence",
            required = true
        )
        public List<CompositionStepRequest> steps;

        @Schema(
            description = "初始上下文参数，传递给第一个策略 / Initial context parameters for first policy"
        )
        public List<Object> initialContext;
    }

    /**
     * 组合步骤请求
     */
    @Schema(description = "策略组合中的单个步骤 / Single step in policy composition")
    public static class CompositionStepRequest {
        @Schema(
            description = "策略模块名称 / Policy module name",
            required = true
        )
        public String policyModule;

        @Schema(
            description = "策略函数名称 / Policy function name",
            required = true
        )
        public String policyFunction;

        @Schema(
            description = "是否使用前一步的结果作为此步骤的输入 / Whether to use previous step's result as input",
            defaultValue = "false"
        )
        public Boolean useResultAsInput;
    }

    /**
     * 策略验证请求数据类
     */
    @Schema(description = "策略验证请求 / Policy validation request")
    public static class PolicyValidationRequest {
        @Schema(
            description = "策略模块名称 / Policy module name",
            required = true
        )
        public String policyModule;

        @Schema(
            description = "策略函数名称 / Policy function name",
            required = true
        )
        public String policyFunction;
    }
}
