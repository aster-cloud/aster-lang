package io.aster.workflow;

import aster.runtime.workflow.ExecutionHandle;
import aster.runtime.workflow.WorkflowEvent;
import aster.runtime.workflow.WorkflowMetadata;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runtime 层集成测试：验证 DAG 并发执行、事件序列一致性与补偿逻辑。
 */
@QuarkusTest
class WorkflowConcurrencyIntegrationTest {

    @Inject
    PostgresWorkflowRuntime workflowRuntime;

    @Inject
    PostgresEventStore eventStore;

    @Inject
    WorkflowSchedulerService schedulerService;

    @Inject
    SagaCompensationExecutor compensationExecutor;

    @BeforeEach
    @Transactional
    void cleanWorkflowTables() {
        WorkflowEventEntity.deleteAll();
        WorkflowStateEntity.deleteAll();
        WorkflowTimerEntity.deleteAll();
    }

    @Test
    void shouldHandleParallelFanOutAndScheduleLifoCompensation() throws Exception {
        String workflowId = UUID.randomUUID().toString();
        ExecutionHandle handle = workflowRuntime.schedule(
                workflowId,
                "fanout-" + workflowId,
                fanOutMetadata()
        );

        appendStepStarted(workflowId, "stepA", List.of());
        appendStepCompleted(workflowId, "stepA", List.of(), "a-ok", false, Instant.now());

        Map<String, Instant> parallelStartTimes = new ConcurrentHashMap<>();
        Map<String, Instant> parallelCompletedTimes = new ConcurrentHashMap<>();
        runParallelBranches(workflowId, parallelStartTimes, parallelCompletedTimes);

        appendStepStarted(workflowId, "stepD", List.of("stepB", "stepC"));
        appendStepFailed(workflowId, "stepD", List.of("stepB", "stepC"), "inventory mismatch");

        schedulerService.processWorkflow(workflowId);

        List<WorkflowEvent> events = eventStore.getEvents(workflowId, 0);
        assertFanOutExecution(parallelStartTimes, parallelCompletedTimes);
        assertDagTopologicalOrder(events);
        assertSequenceIntegrity(workflowId);
        assertCompensationScheduling(events, workflowId);

        // 补偿执行完成后再调度一次，验证补偿 workflow 完成且结果 future 已完成
        executeCompensations(workflowId, events);
        schedulerService.processWorkflow(workflowId);

        WorkflowStateEntity compensated = findState(workflowId);
        assertThat(compensated.status).isEqualTo("COMPENSATED");
        assertThat(handle.getResult().get(3, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void shouldRemainCompatibleWithSerialWorkflow() throws Exception {
        String workflowId = UUID.randomUUID().toString();
        ExecutionHandle handle = workflowRuntime.schedule(
                workflowId,
                "serial-" + workflowId,
                new WorkflowMetadata()
        );

        appendStepStarted(workflowId, "legacyStep1", List.of());
        appendStepCompleted(workflowId, "legacyStep1", List.of(), "init", false, Instant.now());
        appendStepStarted(workflowId, "legacyStep2", List.of("legacyStep1"));
        appendStepCompleted(workflowId, "legacyStep2", List.of("legacyStep1"), "done", false, Instant.now());
        appendWorkflowCompleted(workflowId, Map.of("result", "legacy-ok"));

        schedulerService.processWorkflow(workflowId);

        WorkflowStateEntity state = findState(workflowId);
        assertThat(state.status).isEqualTo("COMPLETED");
        assertThat(handle.getResult().get(3, TimeUnit.SECONDS)).isInstanceOf(Map.class);
    }

    private WorkflowMetadata fanOutMetadata() {
        WorkflowMetadata metadata = new WorkflowMetadata();
        metadata.set(WorkflowMetadata.Keys.ENABLE_COMPENSATION, true);
        metadata.set("description", "fan-out-in test workflow");
        return metadata;
    }

    private void runParallelBranches(String workflowId,
                                     Map<String, Instant> startTimes,
                                     Map<String, Instant> completionTimes) throws InterruptedException {
        List<String> branches = List.of("stepB", "stepC");
        CountDownLatch ready = new CountDownLatch(branches.size());
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(branches.size());

        try {
            for (String branch : branches) {
                executor.submit(() -> {
                    try {
                        ready.countDown();
                        go.await(1, TimeUnit.SECONDS);

                        Instant started = Instant.now();
                        startTimes.put(branch, started);
                        appendStepStarted(workflowId, branch, List.of("stepA"));

                        // stepB 故意多等待，确保 completedAt 有明显差值验证 LIFO
                        long sleepMs = "stepB".equals(branch) ? 140L : 40L;
                        TimeUnit.MILLISECONDS.sleep(sleepMs);

                        Instant completed = Instant.now();
                        completionTimes.put(branch, completed);
                        appendStepCompleted(workflowId, branch, List.of("stepA"), branch + "-result", true, completed);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            ready.await(1, TimeUnit.SECONDS);
            go.countDown();
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void assertFanOutExecution(Map<String, Instant> startTimes, Map<String, Instant> completedTimes) {
        assertThat(startTimes).hasSize(2);
        assertThat(completedTimes).hasSize(2);

        Instant earliestStart = startTimes.values().stream().min(Instant::compareTo).orElseThrow();
        Instant latestStart = startTimes.values().stream().max(Instant::compareTo).orElseThrow();
        Instant earliestComplete = completedTimes.values().stream().min(Instant::compareTo).orElseThrow();
        Instant latestComplete = completedTimes.values().stream().max(Instant::compareTo).orElseThrow();

        // 并发步骤的启动间隔需足够小，证明 fan-out 不是串行执行
        assertThat(Duration.between(earliestStart, latestStart).toMillis()).isLessThan(80);
        // 总执行时间明显短于串行（140ms + 40ms），证明存在重叠
        assertThat(Duration.between(earliestStart, latestComplete).toMillis()).isLessThan(250);
        // 至少有一步在另一支线完成前已经结束，表示重叠
        assertThat(earliestComplete).isBefore(latestComplete);
    }

    private void assertDagTopologicalOrder(List<WorkflowEvent> events) {
        long stepBCompletedSeq = sequenceOf(events, WorkflowEvent.Type.STEP_COMPLETED, "stepB");
        long stepCCompletedSeq = sequenceOf(events, WorkflowEvent.Type.STEP_COMPLETED, "stepC");
        long stepDStartedSeq = sequenceOf(events, WorkflowEvent.Type.STEP_STARTED, "stepD");

        assertThat(stepDStartedSeq).isGreaterThan(stepBCompletedSeq);
        assertThat(stepDStartedSeq).isGreaterThan(stepCCompletedSeq);
    }

    private void assertSequenceIntegrity(String workflowId) {
        List<Long> sequences = findEventSequences(workflowId);
        assertThat(sequences).isNotEmpty();

        for (int i = 1; i < sequences.size(); i++) {
            long previous = sequences.get(i - 1);
            long current = sequences.get(i);
            assertThat(current).as("sequence[%s]", i).isEqualTo(previous + 1);
        }
    }

    private void assertCompensationScheduling(List<WorkflowEvent> events, String workflowId) {
        List<WorkflowEvent> compensationEvents = events.stream()
                .filter(e -> WorkflowEvent.Type.COMPENSATION_SCHEDULED.equals(e.getEventType()))
                .toList();

        assertThat(compensationEvents).hasSize(2);
        List<String> scheduledSteps = compensationEvents.stream()
                .map(event -> {
                    Map<?, ?> payload = (Map<?, ?>) event.getPayload();
                    return Objects.toString(payload.get("stepId"));
                })
                .toList();

        // LIFO 顺序：stepB 完成时间晚于 stepC，应首先补偿
        assertThat(scheduledSteps).containsExactly("stepB", "stepC");

        WorkflowStateEntity state = findState(workflowId);
        assertThat(state.status).isEqualTo("COMPENSATING");
    }

    private void executeCompensations(String workflowId, List<WorkflowEvent> events) {
        List<String> scheduledSteps = events.stream()
                .filter(e -> WorkflowEvent.Type.COMPENSATION_SCHEDULED.equals(e.getEventType()))
                .map(e -> Objects.toString(((Map<?, ?>) e.getPayload()).get("stepId")))
                .toList();

        scheduledSteps.forEach(step ->
                compensationExecutor.executeCompensation(workflowId, step, null)
        );
    }

    private long sequenceOf(List<WorkflowEvent> events, String eventType, String stepId) {
        return events.stream()
                .filter(e -> eventType.equals(e.getEventType()))
                .filter(e -> {
                    Map<?, ?> payload = (Map<?, ?>) e.getPayload();
                    return stepId.equals(payload.get("stepId"));
                })
                .map(WorkflowEvent::getSequence)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Event not found: " + eventType + "/" + stepId));
    }

    @Transactional
    List<Long> findEventSequences(String workflowId) {
        UUID wfId = UUID.fromString(workflowId);
        return WorkflowEventEntity.findByWorkflowId(wfId, 0)
                .stream()
                .sorted(Comparator.comparingLong(entity -> entity.sequence))
                .map(entity -> entity.sequence)
                .toList();
    }

    @Transactional
    WorkflowStateEntity findState(String workflowId) {
        UUID wfId = UUID.fromString(workflowId);
        return WorkflowStateEntity.findByWorkflowId(wfId)
                .orElseThrow(() -> new IllegalStateException("workflow state missing: " + workflowId));
    }

    private void appendStepStarted(String workflowId, String stepId, List<String> dependencies) {
        Map<String, Object> payload = Map.of(
                "stepId", stepId,
                "dependencies", new ArrayList<>(dependencies),
                "status", "STARTED",
                "startedAt", Instant.now().toString()
        );
        eventStore.appendEvent(workflowId, WorkflowEvent.Type.STEP_STARTED, payload);
    }

    private void appendStepCompleted(String workflowId,
                                     String stepId,
                                     List<String> dependencies,
                                     Object result,
                                     boolean hasCompensation,
                                     Instant completedAt) {
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("stepId", stepId);
        payload.put("dependencies", new ArrayList<>(dependencies));
        payload.put("status", "COMPLETED");
        payload.put("completedAt", completedAt.toString());
        payload.put("hasCompensation", hasCompensation);
        if (result != null) {
            payload.put("result", result);
        }
        eventStore.appendEvent(workflowId, WorkflowEvent.Type.STEP_COMPLETED, payload);
    }

    private void appendStepFailed(String workflowId,
                                  String stepId,
                                  List<String> dependencies,
                                  String error) {
        Map<String, Object> payload = Map.of(
                "stepId", stepId,
                "dependencies", new ArrayList<>(dependencies),
                "status", "FAILED",
                "completedAt", Instant.now().toString(),
                "error", error
        );
        eventStore.appendEvent(workflowId, WorkflowEvent.Type.STEP_FAILED, payload);
    }

    private void appendWorkflowCompleted(String workflowId, Object result) {
        eventStore.appendEvent(workflowId, WorkflowEvent.Type.WORKFLOW_COMPLETED, result);
    }
}
