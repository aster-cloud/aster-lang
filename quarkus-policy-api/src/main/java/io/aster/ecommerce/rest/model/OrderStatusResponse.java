package io.aster.ecommerce.rest.model;

import aster.runtime.workflow.WorkflowEvent;

import java.util.List;

/**
 * 订单状态查询响应。
 *
 * 返回 workflow 当前状态、事件列表与最近更新时间。
 */
public record OrderStatusResponse(
    String orderId,
    String workflowId,
    String status,
    List<WorkflowEvent> events,
    long lastUpdated
) { }
