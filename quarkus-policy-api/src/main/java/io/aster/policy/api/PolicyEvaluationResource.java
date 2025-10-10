package io.aster.policy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * REST API for policy evaluation
 *
 * 提供策略评估的REST端点，支持动态调用编译后的Aster策略
 */
@Path("/api/policies")
@Tag(name = "Policy Evaluation", description = "Evaluate policies against context data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PolicyEvaluationResource {

    @Inject
    ObjectMapper objectMapper;

    /**
     * 评估策略
     *
     * @param request 包含策略名称和上下文数据的请求
     * @return 评估结果
     */
    @POST
    @Path("/evaluate")
    @Operation(
        summary = "Evaluate a policy",
        description = "Evaluates a specific policy against provided context data"
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
    public Response evaluatePolicy(PolicyEvaluationRequest request) {
        try {
            // 验证请求
            if (request == null || request.policyModule == null || request.policyFunction == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required fields: policyModule, policyFunction"))
                    .build();
            }

            // 动态加载策略类
            String className = request.policyModule + "." + request.policyFunction + "_fn";
            Class<?> policyClass = Class.forName(className);

            // 查找函数方法 - 使用函数名而不是"invoke"
            // Aster生成的函数类有静态方法,方法名与函数名相同
            Method functionMethod = null;
            for (Method m : policyClass.getDeclaredMethods()) {
                if (m.getName().equals(request.policyFunction) &&
                    java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    functionMethod = m;
                    break;
                }
            }

            if (functionMethod == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Policy method not found",
                                 "details", "No static method named: " + request.policyFunction))
                    .build();
            }

            // 准备参数 - 将JSON对象转换为正确的类型
            Parameter[] parameters = functionMethod.getParameters();
            Object[] args = new Object[parameters.length];

            if (request.context != null && parameters.length > 0) {
                List<Object> contextList = request.context;
                for (int i = 0; i < Math.min(parameters.length, contextList.size()); i++) {
                    Class<?> expectedType = parameters[i].getType();
                    Object contextObj = contextList.get(i);

                    // 将Map转换为目标类型
                    if (contextObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) contextObj;
                        args[i] = constructFromMap(expectedType, map);
                    } else {
                        args[i] = contextObj;
                    }
                }
            }

            // 调用策略
            long startTime = System.nanoTime();
            Object result = functionMethod.invoke(null, args);
            long durationNanos = System.nanoTime() - startTime;

            // 构建响应
            PolicyEvaluationResponse response = new PolicyEvaluationResponse();
            response.result = result;
            response.executionTimeMs = durationNanos / 1_000_000.0;
            response.policyModule = request.policyModule;
            response.policyFunction = request.policyFunction;
            response.timestamp = System.currentTimeMillis();

            return Response.ok(response).build();

        } catch (ClassNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Policy not found: " + request.policyModule + "." + request.policyFunction))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Policy evaluation failed", "details", e.getMessage()))
                .build();
        }
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

    /**
     * 从Map构造对象 - 使用反射查找匹配的构造函数
     */
    private Object constructFromMap(Class<?> targetClass, Map<String, Object> map) throws Exception {
        // 获取所有构造函数
        var constructors = targetClass.getConstructors();
        if (constructors.length == 0) {
            throw new IllegalArgumentException("No public constructors found for " + targetClass.getName());
        }

        // 使用第一个构造函数(Aster生成的类只有一个全参构造函数)
        var constructor = constructors[0];
        Parameter[] params = constructor.getParameters();
        Object[] args = new Object[params.length];

        // 获取类的所有字段,按声明顺序
        var fields = targetClass.getDeclaredFields();

        // 从Map中提取参数值(假设字段顺序与构造函数参数顺序一致)
        for (int i = 0; i < params.length && i < fields.length; i++) {
            String fieldName = fields[i].getName();
            Object value = map.get(fieldName);

            // 类型转换
            Class<?> paramType = params[i].getType();
            if (value != null) {
                if (paramType == int.class || paramType == Integer.class) {
                    args[i] = ((Number) value).intValue();
                } else if (paramType == long.class || paramType == Long.class) {
                    args[i] = ((Number) value).longValue();
                } else if (paramType == double.class || paramType == Double.class) {
                    args[i] = ((Number) value).doubleValue();
                } else if (paramType == String.class) {
                    args[i] = value.toString();
                } else {
                    args[i] = value;
                }
            } else {
                // 为null时提供默认值
                if (paramType == int.class) {
                    args[i] = 0;
                } else if (paramType == long.class) {
                    args[i] = 0L;
                } else if (paramType == double.class) {
                    args[i] = 0.0;
                } else if (paramType == boolean.class) {
                    args[i] = false;
                } else {
                    args[i] = null;
                }
            }
        }

        return constructor.newInstance(args);
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
}
