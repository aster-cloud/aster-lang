package io.aster.audit.scheduler;

import io.aster.audit.entity.AnomalyActionEntity;
import io.aster.audit.service.AnomalyActionExecutor;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 异常动作消费器定时任务（Phase 3.7）
 *
 * 每 5 分钟执行一次，消费 anomaly_actions 队列并执行验证/回滚动作。
 * 采用 outbox 模式，解耦检测和执行：
 * - 检测任务（AnomalyDetectionScheduler）负责提交动作到队列
 * - 本任务负责从队列拉取动作并执行
 *
 * 核心逻辑：
 * 1. 查询 status=PENDING 的动作（批次大小=5）
 * 2. 逐个执行动作：VERIFY_REPLAY 或 AUTO_ROLLBACK
 * 3. 更新动作状态：PENDING → RUNNING → DONE/FAILED
 * 4. 记录执行时间和错误信息
 *
 * 特性：
 * - 并发控制：SKIP 模式避免任务重叠
 * - 独立事务：每个动作独立提交，失败不影响其他动作
 * - 错误隔离：单个动作失败不阻止后续动作执行
 * - 性能限制：每次最多处理 5 个动作，避免长时间占用线程
 */
@ApplicationScoped
public class AnomalyActionScheduler {

    @Inject
    AnomalyActionExecutor executor;

    /**
     * 定时消费异常动作队列
     *
     * every = 5m：每 5 分钟执行一次
     * concurrentExecution = SKIP：如果前一次任务未完成，跳过本次执行
     */
    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void consumeActions() {
        Log.info("开始消费异常动作队列");

        try {
            // 1. 查询待处理动作（限制批次大小=5）
            List<AnomalyActionEntity> pendingActions = AnomalyActionEntity.findPendingActions(5);

            if (pendingActions.isEmpty()) {
                Log.debug("没有待处理的异常动作");
                return;
            }

            Log.infof("发现 %d 个待处理动作", pendingActions.size());

            // 2. 逐个执行动作（每个动作独立事务）
            for (AnomalyActionEntity action : pendingActions) {
                executeAction(action);
            }

            Log.infof("异常动作消费完成，处理了 %d 个动作", pendingActions.size());

        } catch (Exception e) {
            Log.errorf(e, "异常动作消费任务执行失败");
            // 不重新抛出异常，避免影响下次调度
        }
    }

    /**
     * 执行单个异常动作（独立事务）
     *
     * @param action 异常动作实体
     */
    @Transactional
    void executeAction(AnomalyActionEntity action) {
        Log.infof("开始执行动作 %d (type=%s, anomalyId=%d)",
            action.id, action.actionType, action.anomalyId);

        try {
            // 1. 更新状态为 RUNNING
            action.status = "RUNNING";
            action.startedAt = Instant.now();
            action.persist();

            // 2. 根据动作类型调用对应的执行器
            switch (action.actionType) {
                case "VERIFY_REPLAY":
                    executor.executeReplayVerification(action)
                        .await().indefinitely();
                    break;

                case "AUTO_ROLLBACK":
                    executor.executeAutoRollback(action)
                        .await().indefinitely();
                    break;

                default:
                    throw new IllegalArgumentException(
                        "未知的动作类型: " + action.actionType
                    );
            }

            // 3. 执行成功，更新状态为 DONE
            action.status = "DONE";
            action.completedAt = Instant.now();
            action.persist();

            Log.infof("动作 %d 执行成功", action.id);

        } catch (Exception e) {
            // 4. 执行失败，更新状态为 FAILED
            action.status = "FAILED";
            action.completedAt = Instant.now();
            action.errorMessage = e.getMessage();
            action.persist();

            Log.errorf(e, "动作 %d 执行失败", action.id);
        }
    }
}
