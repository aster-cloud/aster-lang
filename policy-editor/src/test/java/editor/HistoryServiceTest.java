package editor;

import com.fasterxml.jackson.databind.ObjectMapper;
import editor.model.Policy;
import editor.model.PolicyRuleSet;
import editor.service.HistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryServiceTest {
    private final HistoryService historyService = new HistoryService();
    private final ObjectMapper mapper = new ObjectMapper();

    private String testId() { return "test-hist-" + System.currentTimeMillis(); }

    @AfterEach
    void cleanup() throws Exception {
        // 清理残留测试文件（容错处理）
        Files.walk(Paths.get("data/history")).filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().startsWith("test-hist-")).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
        Files.walk(Paths.get("examples/policy-editor/src/main/resources/policies")).filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().startsWith("test-hist-")).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
    }

    @Test
    void snapshotUndoRedo() throws Exception {
        // 手动注入 ObjectMapper
        var f = HistoryService.class.getDeclaredField("objectMapper");
        f.setAccessible(true);
        f.set(historyService, mapper);
        String id = testId();
        Policy v1 = new Policy(id, "v1", new PolicyRuleSet(java.util.Map.of("io", List.of("*"))), new PolicyRuleSet(java.util.Map.of()));
        Policy v2 = new Policy(id, "v2", new PolicyRuleSet(java.util.Map.of("io", List.of("/tmp/*"))), new PolicyRuleSet(java.util.Map.of()));

        historyService.snapshot(v1);
        Thread.sleep(5); // 保证时间戳不同
        historyService.snapshot(v2);

        Path file = Paths.get("examples/policy-editor/src/main/resources/policies", id + ".json");
        // 执行撤销：应写入 v1
        assertTrue(historyService.undo(id, file));
        String s1 = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(s1.contains("\"name\": \"v1\"") || s1.contains("v1"));
        // 执行重做：应写入 v2
        assertTrue(historyService.redo(id, file));
        String s2 = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(s2.contains("\"name\": \"v2\"") || s2.contains("v2"));
    }
}
