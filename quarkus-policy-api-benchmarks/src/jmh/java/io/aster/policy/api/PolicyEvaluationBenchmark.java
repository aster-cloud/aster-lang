package io.aster.policy.api;

import io.aster.policy.api.model.BatchRequest;
import io.aster.policy.api.model.PolicyEvaluationResult;
import io.smallrye.mutiny.Uni;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * PolicyEvaluationService 的JMH性能基准，覆盖冷启动、热启动、缓存命中与批量评估吞吐量。
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
public class PolicyEvaluationBenchmark {

    @Benchmark
    public PolicyEvaluationResult coldStart(ColdStartState state) {
        return state.evaluateScenario();
    }

    @Benchmark
    public PolicyEvaluationResult hotStart(HotStartState state) {
        return state.evaluateScenario();
    }

    @Benchmark
    public PolicyEvaluationResult cachedEvaluation(CachedState state) {
        return state.evaluateScenario();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public List<PolicyEvaluationResult> batchThroughput(BatchState state) {
        return state.evaluateBatch();
    }

    @State(Scope.Benchmark)
    public abstract static class AbstractPolicyState implements AutoCloseable {

        protected static final String DEFAULT_TENANT = "default";

        @Param({"loan", "creditcard", "fraud"})
        public String policyType = "loan";

        protected BenchmarkBootstrap bootstrap;
        protected PolicyEvaluationService service;
        protected Map<String, PolicyScenario> scenarios;
        protected List<BatchRequest> batchRequests;

        @Setup(Level.Trial)
        public void setUp() {
            bootstrap = new BenchmarkBootstrap();
            service = bootstrap.service();
            scenarios = Map.of(
                "loan", new PolicyScenario(DEFAULT_TENANT, "aster.finance.loan", "evaluateLoanEligibility", buildLoanContext()),
                "creditcard", new PolicyScenario(DEFAULT_TENANT, "aster.finance.creditcard", "evaluateCreditCardApplication", buildCreditCardContext()),
                "fraud", new PolicyScenario(DEFAULT_TENANT, "aster.finance.fraud", "detectFraud", buildFraudContext())
            );
            batchRequests = buildBatchRequests();
        }

        protected PolicyScenario scenario() {
            return scenarios.getOrDefault(policyType, scenarios.get("loan"));
        }

        public PolicyEvaluationResult evaluateScenario() {
            PolicyScenario current = scenario();
            Uni<PolicyEvaluationResult> uni = service.evaluatePolicy(
                current.tenantId(),
                current.policyModule(),
                current.policyFunction(),
                current.context()
            );
            return uni.await().indefinitely();
        }

        public List<PolicyEvaluationResult> evaluateBatch() {
            return service.evaluateBatch(batchRequests).await().indefinitely();
        }

        protected void resetColdState() {
            bootstrap.resetAllCachesAndMetadata();
        }

        protected void ensureMetadataForScenario(String scenarioKey) {
            if (bootstrap.markMetadataPrimed(scenarioKey)) {
                // 冷启动预热一次以填充反射/类型缓存
                evaluateScenario();
            }
        }

        protected void ensureCachePrimed(String scenarioKey) {
            if (bootstrap.markCachePrimed(scenarioKey)) {
                evaluateScenario();
            }
        }

        protected void invalidateScenarioCache() {
            PolicyScenario current = scenario();
            service.invalidateCache(current.tenantId(), current.policyModule(), current.policyFunction())
                .await().indefinitely();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            close();
        }

        @Override
        public void close() {
            if (bootstrap != null) {
                bootstrap.close();
            }
        }

        private List<BatchRequest> buildBatchRequests() {
            List<BatchRequest> requests = new ArrayList<>(100);
            PolicyScenario loan = scenarios.get("loan");
            PolicyScenario credit = scenarios.get("creditcard");
            PolicyScenario fraud = scenarios.get("fraud");

            for (int i = 0; i < 70; i++) {
                requests.add(loan.toBatchRequest(mutateLoanContext(loan.context(), i)));
            }
            for (int i = 0; i < 20; i++) {
                requests.add(credit.toBatchRequest(mutateCreditContext(credit.context(), i)));
            }
            for (int i = 0; i < 10; i++) {
                requests.add(fraud.toBatchRequest(mutateFraudContext(fraud.context(), i)));
            }
            return Collections.unmodifiableList(requests);
        }
    }

    public static class ColdStartState extends AbstractPolicyState {

        @Setup(Level.Invocation)
        public void coldReset() {
            resetColdState();
        }
    }

    public static class HotStartState extends AbstractPolicyState {

        @Setup(Level.Invocation)
        public void warmMetadata() {
            PolicyScenario current = scenario();
            ensureMetadataForScenario(current.key());
            invalidateScenarioCache();
        }
    }

    public static class CachedState extends AbstractPolicyState {

        @Setup(Level.Invocation)
        public void warmCache() {
            ensureCachePrimed(scenario().key());
        }
    }

    public static class BatchState extends AbstractPolicyState {
    }

    private static Object[] buildLoanContext() {
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("applicantId", "APP-9001");
        application.put("amount", 250_000);
        application.put("termMonths", 84);
        application.put("purpose", "home");

        Map<String, Object> applicant = new LinkedHashMap<>();
        applicant.put("age", 37);
        applicant.put("creditScore", 760);
        applicant.put("annualIncome", 520_000);
        applicant.put("monthlyDebt", 6_500);
        applicant.put("yearsEmployed", 7);

        return new Object[]{application, applicant};
    }

    private static Object[] buildCreditCardContext() {
        Map<String, Object> applicant = new LinkedHashMap<>();
        applicant.put("applicantId", "CARD-1001");
        applicant.put("age", 34);
        applicant.put("annualIncome", 180_000);
        applicant.put("creditScore", 730);
        applicant.put("existingCreditCards", 2);
        applicant.put("monthlyRent", 2_600);
        applicant.put("employmentStatus", "Full-time");
        applicant.put("yearsAtCurrentJob", 5);

        Map<String, Object> history = new LinkedHashMap<>();
        history.put("bankruptcyCount", 0);
        history.put("latePayments", 1);
        history.put("utilization", 32);
        history.put("accountAge", 96);
        history.put("hardInquiries", 2);

        Map<String, Object> offer = new LinkedHashMap<>();
        offer.put("productType", "Premium");
        offer.put("requestedLimit", 20_000);
        offer.put("hasRewards", true);
        offer.put("annualFee", 199);

        return new Object[]{applicant, history, offer};
    }

    private static Object[] buildFraudContext() {
        Map<String, Object> transaction = new LinkedHashMap<>();
        transaction.put("transactionId", "TX-991001");
        transaction.put("accountId", "ACCT-4401");
        transaction.put("amount", 95_000);
        transaction.put("timestamp", 1_700_000_000);

        Map<String, Object> history = new LinkedHashMap<>();
        history.put("accountId", "ACCT-4401");
        history.put("averageAmount", 25_000);
        history.put("suspiciousCount", 1);
        history.put("accountAge", 240);
        history.put("lastTimestamp", 1_699_999_500);

        return new Object[]{transaction, history};
    }

    private static Object[] mutateLoanContext(Object[] base, int index) {
        Object[] copy = copyContext(base);
        Map<String, Object> application = asMap(copy[0]);
        Map<String, Object> applicant = asMap(copy[1]);
        application.put("amount", 200_000 + (index * 2_000));
        application.put("termMonths", 60 + (index % 4) * 6);
        applicant.put("creditScore", 700 - (index % 5) * 10);
        applicant.put("monthlyDebt", 5_500 + (index % 3) * 400);
        return copy;
    }

    private static Object[] mutateCreditContext(Object[] base, int index) {
        Object[] copy = copyContext(base);
        Map<String, Object> applicant = asMap(copy[0]);
        Map<String, Object> history = asMap(copy[1]);
        Map<String, Object> offer = asMap(copy[2]);
        applicant.put("creditScore", 710 - (index % 6) * 8);
        applicant.put("yearsAtCurrentJob", 3 + (index % 4));
        history.put("utilization", 30 + (index % 5) * 10);
        offer.put("requestedLimit", 15_000 + (index % 4) * 2_500);
        return copy;
    }

    private static Object[] mutateFraudContext(Object[] base, int index) {
        Object[] copy = copyContext(base);
        Map<String, Object> transaction = asMap(copy[0]);
        Map<String, Object> history = asMap(copy[1]);
        transaction.put("amount", 50_000 + (index * 5_000));
        transaction.put("timestamp", 1_700_000_000 + index * 15);
        history.put("suspiciousCount", 1 + (index % 3));
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Object[] copyContext(Object[] base) {
        Object[] copy = new Object[base.length];
        for (int i = 0; i < base.length; i++) {
            Object entry = base[i];
            if (entry instanceof Map<?, ?> map) {
                copy[i] = new LinkedHashMap<>((Map<String, Object>) map);
            } else {
                copy[i] = entry;
            }
        }
        return copy;
    }

    private record PolicyScenario(String tenantId,
                                  String policyModule,
                                  String policyFunction,
                                  Object[] context) {

        String key() {
            return tenantId + "::" + policyModule + "::" + policyFunction;
        }

        BatchRequest toBatchRequest(Object[] ctx) {
            return new BatchRequest(tenantId, policyModule, policyFunction, ctx);
        }
    }
}
