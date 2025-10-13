package editor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import editor.model.Policy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 策略历史版本管理：文件系统版
 * data/history/<policyId>/<timestamp>.json
 * data/history/<policyId>/.cursor -> 当前游标（整数，指向 versions 列表索引）
 */
@ApplicationScoped
public class HistoryService {
    private static final Path ROOT = Paths.get("data", "history");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneId.systemDefault());

    @Inject ObjectMapper objectMapper;

    public HistoryService() {
        try { Files.createDirectories(ROOT); } catch (IOException ignored) {}
    }

    public synchronized void snapshot(Policy p) {
        Path dir = ROOT.resolve(p.getId());
        try {
            Files.createDirectories(dir);
            String ts = TS.format(Instant.now());
            Path ver = dir.resolve(ts + ".json");
            Files.writeString(ver, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(p), StandardCharsets.UTF_8);
            // 更新游标到最新
            writeCursor(dir, listVersions(dir).size() - 1);
        } catch (IOException e) {
            throw new RuntimeException("写入历史失败", e);
        }
    }

    public synchronized List<Path> listVersions(Path policyDir) throws IOException {
        if (!Files.exists(policyDir)) return List.of();
        try (Stream<Path> s = Files.list(policyDir)) {
            return s.filter(f -> f.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .collect(Collectors.toList());
        }
    }

    public synchronized List<String> listVersionNames(String policyId) {
        Path dir = ROOT.resolve(policyId);
        try {
            return listVersions(dir).stream().map(p -> p.getFileName().toString().replaceFirst("\\.json$", ""))
                    .collect(Collectors.toList());
        } catch (IOException e) { return new ArrayList<>(); }
    }

    public synchronized String loadVersion(String policyId, String versionName) {
        Path file = ROOT.resolve(policyId).resolve(versionName + ".json");
        try { return Files.readString(file); } catch (IOException e) { throw new RuntimeException(e); }
    }

    public synchronized boolean undo(String policyId, Path policyFile) {
        Path dir = ROOT.resolve(policyId);
        try {
            List<Path> versions = listVersions(dir);
            if (versions.isEmpty()) return false;
            int cur = readCursor(dir, versions.size() - 1);
            if (cur <= 0) return false;
            cur -= 1;
            if (policyFile.getParent() != null) Files.createDirectories(policyFile.getParent());
            Files.copy(versions.get(cur), policyFile, StandardCopyOption.REPLACE_EXISTING);
            writeCursor(dir, cur);
            return true;
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public synchronized boolean redo(String policyId, Path policyFile) {
        Path dir = ROOT.resolve(policyId);
        try {
            List<Path> versions = listVersions(dir);
            if (versions.isEmpty()) return false;
            int cur = readCursor(dir, versions.size() - 1);
            if (cur >= versions.size() - 1) return false;
            cur += 1;
            if (policyFile.getParent() != null) Files.createDirectories(policyFile.getParent());
            Files.copy(versions.get(cur), policyFile, StandardCopyOption.REPLACE_EXISTING);
            writeCursor(dir, cur);
            return true;
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private int readCursor(Path dir, int def) {
        Path c = dir.resolve(".cursor");
        try { return Integer.parseInt(Files.readString(c)); } catch (Exception ignored) { return def; }
    }

    private void writeCursor(Path dir, int cur) {
        Path c = dir.resolve(".cursor");
        try { Files.writeString(c, Integer.toString(cur), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); }
        catch (IOException ignored) {}
    }
}
