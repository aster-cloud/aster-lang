package io.aster.policy.event;

/**
 * 审计事件类型定义。
 */
public enum EventType {
    POLICY_EVALUATION,
    POLICY_ROLLBACK,
    POLICY_CREATED,
    ORDER_SUBMITTED,
    ORDER_STATUS_QUERIED
}
