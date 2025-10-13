package editor;

import editor.model.EditorSettings;
import editor.service.SettingsService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SettingsServiceTest {

    @Inject
    SettingsService settingsService;

    @Test
    void defaultSettingsPresent() {
        EditorSettings s = settingsService.load();
        assertNotNull(s.getGraphqlEndpoint());
        assertTrue(s.getTimeoutMillis() >= 0);
        assertNotNull(s.getRemoteRepoDir());
        assertNotNull(s.getUserName());
    }
}

