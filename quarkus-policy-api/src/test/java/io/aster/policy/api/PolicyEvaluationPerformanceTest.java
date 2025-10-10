package io.aster.policy.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * 策略评估性能测试
 *
 * 测试场景：
 * 1. 冷启动性能（首次调用，包含反射加载）
 * 2. 热路径性能（重复调用，利用缓存）
 * 3. 批量评估性能（并行执行多个策略）
 * 4. 缓存命中率和性能
 */
@QuarkusTest
public class PolicyEvaluationPerformanceTest {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 1000;

    @Test
    @DisplayName("Performance: Cold Start - First policy evaluation with metadata loading")
    public void testColdStartPerformance() {
        // 清空所有缓存，模拟冷启动
        given()
            .when().delete("/api/policies/cache")
            .then()
            .statusCode(200);

        // 首次调用（包含类加载和反射元数据构建）
        long startTime = System.nanoTime();

        String response = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "policyModule", "aster.finance.loan",
                "policyFunction", "determineInterestRateBps",
                "context", List.of(Map.of("creditScore", 750))
            ))
            .when().post("/api/policies/evaluate")
            .then()
            .statusCode(200)
            .extract().asString();

        long coldStartTime = System.nanoTime() - startTime;
        double coldStartMs = coldStartTime / 1_000_000.0;

        System.out.println("=== Cold Start Performance ===");
        System.out.println("First call (with metadata loading): " + String.format("%.3f ms", coldStartMs));

        // 冷启动应该在合理范围内（<50ms）
        assertThat("Cold start should complete within 50ms", coldStartMs, lessThan(50.0));
    }

    @Test
    @DisplayName("Performance: Warm Path - Cached metadata with repeated calls")
    public void testWarmPathPerformance() {
        // Warmup - 让JIT编译器优化代码
        System.out.println("\n=== Warm Path Performance ===");
        System.out.println("Warming up with " + WARMUP_ITERATIONS + " iterations...");

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "policyModule", "aster.finance.loan",
                    "policyFunction", "determineInterestRateBps",
                    "context", List.of(Map.of("creditScore", 700 + i % 100))
                ))
                .when().post("/api/policies/evaluate")
                .then()
                .statusCode(200);
        }

        // Benchmark - 测量实际性能
        System.out.println("Running benchmark with " + BENCHMARK_ITERATIONS + " iterations...");
        List<Long> timings = new ArrayList<>();

        long totalStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();

            given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "policyModule", "aster.finance.loan",
                    "policyFunction", "determineInterestRateBps",
                    "context", List.of(Map.of("creditScore", 700 + i % 100))
                ))
                .when().post("/api/policies/evaluate")
                .then()
                .statusCode(200);

            timings.add(System.nanoTime() - start);
        }
        long totalTime = System.nanoTime() - totalStart;

        // 计算统计数据
        timings.sort(Long::compareTo);
        long min = timings.get(0);
        long max = timings.get(timings.size() - 1);
        long p50 = timings.get(timings.size() / 2);
        long p95 = timings.get((int) (timings.size() * 0.95));
        long p99 = timings.get((int) (timings.size() * 0.99));
        double avg = timings.stream().mapToLong(Long::longValue).average().orElse(0);
        double throughput = BENCHMARK_ITERATIONS / (totalTime / 1_000_000_000.0);

        System.out.println("\nResults:");
        System.out.println("  Min:        " + String.format("%.3f ms", min / 1_000_000.0));
        System.out.println("  Average:    " + String.format("%.3f ms", avg / 1_000_000.0));
        System.out.println("  Median(p50):" + String.format("%.3f ms", p50 / 1_000_000.0));
        System.out.println("  p95:        " + String.format("%.3f ms", p95 / 1_000_000.0));
        System.out.println("  p99:        " + String.format("%.3f ms", p99 / 1_000_000.0));
        System.out.println("  Max:        " + String.format("%.3f ms", max / 1_000_000.0));
        System.out.println("  Throughput: " + String.format("%.0f ops/sec", throughput));

        // 验证性能目标
        assertThat("P95 latency should be under 10ms", p95 / 1_000_000.0, lessThan(10.0));
        assertThat("Average latency should be under 5ms", avg / 1_000_000.0, lessThan(5.0));
    }

    @Test
    @DisplayName("Performance: Batch Evaluation - Parallel processing")
    public void testBatchEvaluationPerformance() {
        System.out.println("\n=== Batch Evaluation Performance ===");

        // 创建批量请求
        int[] batchSizes = {5, 10, 20, 50};

        for (int batchSize : batchSizes) {
            List<Map<String, Object>> requests = new ArrayList<>();
            for (int i = 0; i < batchSize; i++) {
                requests.add(Map.of(
                    "policyModule", "aster.finance.loan",
                    "policyFunction", "determineInterestRateBps",
                    "context", List.of(Map.of("creditScore", 700 + i * 5))
                ));
            }

            // 测量批量执行时间
            long start = System.nanoTime();

            given()
                .contentType(ContentType.JSON)
                .body(Map.of("requests", requests))
                .when().post("/api/policies/evaluate/batch")
                .then()
                .statusCode(200)
                .body("count", equalTo(batchSize));

            long batchTime = System.nanoTime() - start;
            double batchTimeMs = batchTime / 1_000_000.0;
            double perPolicyMs = batchTimeMs / batchSize;

            System.out.println("\nBatch size " + batchSize + ":");
            System.out.println("  Total time:      " + String.format("%.3f ms", batchTimeMs));
            System.out.println("  Per policy:      " + String.format("%.3f ms", perPolicyMs));
            System.out.println("  Effective throughput: " + String.format("%.0f policies/sec", batchSize / (batchTimeMs / 1000.0)));
        }
    }

    @Test
    @DisplayName("Performance: Cache Hit Rate - Verify caching effectiveness")
    public void testCacheEffectiveness() {
        System.out.println("\n=== Cache Effectiveness Test ===");

        Map<String, Object> request = Map.of(
            "policyModule", "aster.finance.loan",
            "policyFunction", "determineInterestRateBps",
            "context", List.of(Map.of("creditScore", 800))
        );

        // 首次调用（缓存未命中）
        long firstCallStart = System.nanoTime();
        String firstResponse = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when().post("/api/policies/evaluate")
            .then()
            .statusCode(200)
            .body("fromCache", equalTo(false))
            .extract().asString();
        long firstCallTime = System.nanoTime() - firstCallStart;

        // 第二次调用相同参数（应该从缓存获取）
        long secondCallStart = System.nanoTime();
        String secondResponse = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when().post("/api/policies/evaluate")
            .then()
            .statusCode(200)
            .body("fromCache", equalTo(true))
            .extract().asString();
        long secondCallTime = System.nanoTime() - secondCallStart;

        double firstCallMs = firstCallTime / 1_000_000.0;
        double secondCallMs = secondCallTime / 1_000_000.0;
        double speedup = firstCallMs / secondCallMs;

        System.out.println("First call (cache miss):  " + String.format("%.3f ms", firstCallMs));
        System.out.println("Second call (cache hit):  " + String.format("%.3f ms", secondCallMs));
        System.out.println("Speedup from caching:     " + String.format("%.1fx", speedup));

        // 缓存命中应该显著更快
        assertThat("Cache hit should be at least 5x faster", speedup, greaterThan(5.0));
    }

    @Test
    @DisplayName("Performance: Concurrent Load - Stress test with multiple threads")
    public void testConcurrentLoad() throws InterruptedException {
        System.out.println("\n=== Concurrent Load Test ===");

        int numThreads = 10;
        int requestsPerThread = 100;

        List<Thread> threads = new ArrayList<>();
        List<Long> allTimings = new ArrayList<>();
        Object lock = new Object();

        long testStart = System.nanoTime();

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    long start = System.nanoTime();

                    given()
                        .contentType(ContentType.JSON)
                        .body(Map.of(
                            "policyModule", "aster.finance.loan",
                            "policyFunction", "determineInterestRateBps",
                            "context", List.of(Map.of("creditScore", 700 + (threadId * 100 + i) % 300))
                        ))
                        .when().post("/api/policies/evaluate")
                        .then()
                        .statusCode(200);

                    long duration = System.nanoTime() - start;
                    synchronized (lock) {
                        allTimings.add(duration);
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        long totalTime = System.nanoTime() - testStart;

        // 计算统计
        allTimings.sort(Long::compareTo);
        double avg = allTimings.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = allTimings.get((int) (allTimings.size() * 0.95));
        long p99 = allTimings.get((int) (allTimings.size() * 0.99));
        int totalRequests = numThreads * requestsPerThread;
        double throughput = totalRequests / (totalTime / 1_000_000_000.0);

        System.out.println("Threads: " + numThreads);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Total time: " + String.format("%.3f sec", totalTime / 1_000_000_000.0));
        System.out.println("\nLatency:");
        System.out.println("  Average: " + String.format("%.3f ms", avg / 1_000_000.0));
        System.out.println("  p95:     " + String.format("%.3f ms", p95 / 1_000_000.0));
        System.out.println("  p99:     " + String.format("%.3f ms", p99 / 1_000_000.0));
        System.out.println("\nThroughput: " + String.format("%.0f requests/sec", throughput));

        // 验证并发性能
        assertThat("Concurrent p99 should be under 50ms", p99 / 1_000_000.0, lessThan(50.0));
        assertThat("Throughput should be over 100 req/sec", throughput, greaterThan(100.0));
    }
}
