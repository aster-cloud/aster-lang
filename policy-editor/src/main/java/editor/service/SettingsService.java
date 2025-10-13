package editor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import editor.model.EditorSettings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 设置管理服务：负责加载/保存前端设置（GraphQL 端点/超时/压缩）。
 * 采用简单的本地 JSON 文件存储（相对可执行目录 data/editor-settings.json）。
 */
@ApplicationScoped
public class SettingsService {

    private static final Path SETTINGS_PATH = Paths.get("data", "editor-settings.json");

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Config config;

    public SettingsService() {
        try {
            Files.createDirectories(SETTINGS_PATH.getParent());
        } catch (IOException ignored) {}
    }

    public synchronized EditorSettings load() {
        if (Files.exists(SETTINGS_PATH)) {
            try {
                return objectMapper.readValue(SETTINGS_PATH.toFile(), EditorSettings.class);
            } catch (Exception ignored) {}
        }
        // 默认值：指向本服务 /graphql，超时 5000ms，开启压缩
        int port = 8081;
        try {
            port = config.getOptionalValue("quarkus.http.port", Integer.class).orElse(8081);
        } catch (Exception ignored) {}
        EditorSettings def = new EditorSettings("http://localhost:" + port + "/graphql", 5000, true);
        def.setCacheTtlMillis(3000);
        def.setRemoteRepoDir("data/remote-policies");
        def.setUserName("admin");
        return def;
    }

    public synchronized void save(EditorSettings s) {
        try {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(SETTINGS_PATH.toFile(), s);
        } catch (IOException e) {
            throw new RuntimeException("保存设置失败: " + SETTINGS_PATH, e);
        }
    }
}
