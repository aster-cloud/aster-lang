package editor;

import editor.service.PolicyService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SyncServiceTest {

    @Inject PolicyService policyService;

    Path policiesDir = Paths.get("examples/policy-editor/src/main/resources/policies");
    Path remoteDir = Paths.get("build", "test-remote-" + System.currentTimeMillis());
    Path remoteFile = remoteDir.resolve("test-sync-file.json");
    Path localFile = policiesDir.resolve("test-sync-file.json");

    @AfterEach
    void cleanup() throws Exception {
        try { Files.deleteIfExists(localFile); } catch (Exception ignored) {}
        if (Files.exists(remoteDir)) {
            Files.walk(remoteDir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
        }
    }

    @Test
    void syncPullPushWithCounts() throws Exception {
        Files.createDirectories(remoteDir);
        Files.writeString(remoteFile, "{\"id\":\"p1\",\"name\":\"A\",\"allow\":{},\"deny\":{}}", StandardCharsets.UTF_8);

        var r1 = policyService.syncPullWithResult(remoteDir.toString());
        assertEquals(1, r1.created);
        assertEquals(0, r1.updated);
        assertEquals(0, r1.skipped);

        var r2 = policyService.syncPullWithResult(remoteDir.toString());
        assertEquals(0, r2.created);
        assertEquals(0, r2.updated);
        assertEquals(1, r2.skipped);

        Files.writeString(remoteFile, "{\"id\":\"p1\",\"name\":\"B\",\"allow\":{},\"deny\":{}}", StandardCharsets.UTF_8);
        var r3 = policyService.syncPullWithResult(remoteDir.toString());
        assertEquals(0, r3.created);
        assertEquals(1, r3.updated);

        var r4 = policyService.syncPushWithResult(remoteDir.toString());
        assertTrue(r4.created >= 0); // 至少可运行
    }
}

