package io.aster.ecommerce.rest;

import io.aster.policy.event.AuditEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试环境审计事件记录器，用于捕获异步事件以便断言。
 */
@ApplicationScoped
public class TestAuditEventRecorder {

    private final CopyOnWriteArrayList<AuditEvent> events = new CopyOnWriteArrayList<>();

    public void onAuditEvent(@ObservesAsync AuditEvent event) {
        events.add(event);
    }

    public void clear() {
        events.clear();
    }

    public List<AuditEvent> snapshot() {
        return List.copyOf(events);
    }

    public AuditEvent awaitLatest(int expectedCount, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (events.size() >= expectedCount) {
                return events.get(events.size() - 1);
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return events.isEmpty() ? null : events.get(events.size() - 1);
    }
}
