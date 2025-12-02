package io.aster.policy.api;

import io.aster.policy.api.PolicyEvaluationBenchmark.BatchState;
import io.aster.policy.api.model.PolicyEvaluationResult;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多线程批量评估吞吐基准，用于验证在多核下的扩展效率。
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PolicyEvaluationMultiThreadBenchmark {

    @Benchmark
    @Threads(2)
    public List<PolicyEvaluationResult> batchThroughputThreads2(BatchState state) {
        return state.evaluateBatch();
    }

    @Benchmark
    @Threads(4)
    public List<PolicyEvaluationResult> batchThroughputThreads4(BatchState state) {
        return state.evaluateBatch();
    }

    @Benchmark
    @Threads(8)
    public List<PolicyEvaluationResult> batchThroughputThreads8(BatchState state) {
        return state.evaluateBatch();
    }

    @Benchmark
    @Threads(16)
    public List<PolicyEvaluationResult> batchThroughputThreads16(BatchState state) {
        return state.evaluateBatch();
    }
}
