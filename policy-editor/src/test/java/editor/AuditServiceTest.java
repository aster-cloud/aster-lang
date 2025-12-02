package editor;

import editor.service.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditServiceTest {
    private final AuditService auditService = new AuditService();

    @AfterEach
    void cleanup() {
        // 清理，避免污染后续测试
        auditService.clear();
    }

    @Test
    void recordAndQueryWithFilter() {
        auditService.recordCtx("tester","create","p1","ok","t1","127.0.0.1","JUnit");
        auditService.recordCtx("tester","update","p1","changed","t1","127.0.0.1","JUnit");

        List<AuditService.AuditEntry> page = auditService.query(0, 10, "update");
        assertEquals(1, page.size());
        AuditService.AuditEntry e = page.get(0);
        assertEquals("tester", e.actor);
        assertEquals("update", e.action);
        assertEquals("p1", e.target);
        assertEquals("t1", e.tenant);
        assertEquals("127.0.0.1", e.ip);
        assertEquals("JUnit", e.ua);
    }
}
