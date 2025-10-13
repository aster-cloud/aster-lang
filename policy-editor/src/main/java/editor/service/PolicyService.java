package editor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import editor.model.Policy;
import editor.service.HistoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 策略管理服务，负责策略的 CRUD 操作
 */
@ApplicationScoped
public class PolicyService {

    private static final String POLICIES_DIR = "examples/policy-editor/src/main/resources/policies";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AuditService auditService;

    @Inject
    HistoryService historyService;

    @Inject
    AuthService authService;

    @Inject
    RequestContextService requestContext;

    public PolicyService() {
        // 确保目录存在
        try {
            Files.createDirectories(Paths.get(POLICIES_DIR));
        } catch (IOException e) {
            throw new RuntimeException("无法创建策略目录: " + POLICIES_DIR, e);
        }
    }

    /**
     * 获取所有策略
     */
    public List<Policy> getAllPolicies() {
        try (Stream<Path> paths = Files.walk(Paths.get(POLICIES_DIR))) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .map(this::loadPolicy)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("读取策略列表失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 根据 ID 获取策略
     */
    public Optional<Policy> getPolicyById(String id) {
        Path policyPath = getPolicyPath(id);
        return loadPolicy(policyPath);
    }

    /**
     * 创建新策略
     */
    public Policy createPolicy(Policy policy) {
        Path policyPath = getPolicyPath(policy.getId());
        Policy created = savePolicy(policyPath, policy);
        historyService.snapshot(created);
        String actor = authService.currentUser();
        auditService.recordCtx(actor, "create", created.getId(), created.getName(), requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
        return created;
    }

    /**
     * 更新现有策略
     */
    public Optional<Policy> updatePolicy(String id, Policy policy) {
        Path policyPath = getPolicyPath(id);
        if (!Files.exists(policyPath)) {
            return Optional.empty();
        }
        Policy updatedPolicy = new Policy(id, policy.getName(), policy.getAllow(), policy.getDeny());
        Policy out = savePolicy(policyPath, updatedPolicy);
        historyService.snapshot(out);
        String actor = authService.currentUser();
        auditService.recordCtx(actor, "update", id, policy.getName(), requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
        return Optional.of(out);
    }

    /**
     * 删除策略
     */
    public boolean deletePolicy(String id) {
        Path policyPath = getPolicyPath(id);
        try {
            boolean ok = Files.deleteIfExists(policyPath);
            if (ok) {
                String actor = authService.currentUser();
                auditService.recordCtx(actor, "delete", id, "", requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
            }
            return ok;
        } catch (IOException e) {
            System.err.println("删除策略失败: " + e.getMessage());
            return false;
        }
    }

    private Path getPolicyPath(String id) {
        return Paths.get(POLICIES_DIR, id + ".json");
    }

    private Optional<Policy> loadPolicy(Path path) {
        try {
            Policy policy = objectMapper.readValue(path.toFile(), Policy.class);
            return Optional.of(policy);
        } catch (IOException e) {
            System.err.println("读取策略文件失败: " + path + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    private Policy savePolicy(Path path, Policy policy) {
        try {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(path.toFile(), policy);
            return policy;
        } catch (IOException e) {
            throw new RuntimeException("保存策略失败: " + path, e);
        }
    }

    // 批量导出为 ZIP 到目标 Path
    public Path exportZip(Path targetZip) {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(targetZip))) {
            try (Stream<Path> paths = Files.walk(Paths.get(POLICIES_DIR))) {
                paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    try {
                        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(Paths.get(POLICIES_DIR).relativize(p).toString());
                        zos.putNextEntry(entry);
                        Files.copy(p, zos);
                        zos.closeEntry();
                    } catch (IOException ex) { throw new RuntimeException(ex); }
                });
            }
            String actor = authService.currentUser();
            auditService.recordCtx(actor, "export", "all", targetZip.toString(), requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
            return targetZip;
        } catch (IOException e) { throw new RuntimeException("导出ZIP失败", e); }
    }

    // 从 ZIP 输入流导入策略，覆盖同名
    public void importZip(java.io.InputStream in) {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(in)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                Path out = Paths.get(POLICIES_DIR).resolve(entry.getName()).normalize();
                Files.createDirectories(out.getParent());
                Files.copy(zis, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }
            String actor = authService.currentUser();
            auditService.recordCtx(actor, "import", "all", "zip", requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
        } catch (IOException e) { throw new RuntimeException("导入ZIP失败", e); }
    }

    // 同步：从远端目录拉取/推送（本地目录代替“远端仓库”）
    public void syncPull(String remoteDir) {
        Path src = Paths.get(remoteDir);
        try (Stream<Path> paths = Files.walk(src)) {
            paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    Path out = Paths.get(POLICIES_DIR).resolve(src.relativize(p).toString());
                    Files.createDirectories(out.getParent());
                    Files.copy(p, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) { throw new RuntimeException(ex); }
            });
            String actor = authService.currentUser();
            auditService.recordCtx(actor, "pull", remoteDir, "", requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
        } catch (IOException e) { throw new RuntimeException("同步拉取失败", e); }
    }

    public void syncPush(String remoteDir) {
        Path dst = Paths.get(remoteDir);
        try (Stream<Path> paths = Files.walk(Paths.get(POLICIES_DIR))) {
            paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    Path out = dst.resolve(Paths.get(POLICIES_DIR).relativize(p).toString());
                    Files.createDirectories(out.getParent());
                    Files.copy(p, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) { throw new RuntimeException(ex); }
            });
            String actor = authService.currentUser();
            auditService.recordCtx(actor, "push", remoteDir, "", requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
        } catch (IOException e) { throw new RuntimeException("同步推送失败", e); }
    }

    public static class SyncResult { public int created; public int updated; public int skipped; }

    public SyncResult syncPullWithResult(String remoteDir) {
        Path src = Paths.get(remoteDir);
        SyncResult r = new SyncResult();
        try (Stream<Path> paths = Files.walk(src)) {
            paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    Path out = Paths.get(POLICIES_DIR).resolve(src.relativize(p).toString());
                    Files.createDirectories(out.getParent());
                    if (!Files.exists(out)) { Files.copy(p, out); r.created++; }
                    else {
                        byte[] a = Files.readAllBytes(p), b = Files.readAllBytes(out);
                        if (!java.util.Arrays.equals(a, b)) { Files.copy(p, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING); r.updated++; }
                        else { r.skipped++; }
                    }
                } catch (IOException ex) { throw new RuntimeException(ex); }
            });
            String actor = authService.currentUser();
            auditService.recordCtx(actor, "pull", remoteDir, String.format("created=%d updated=%d skipped=%d", r.created,r.updated,r.skipped), requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
            return r;
        } catch (IOException e) { throw new RuntimeException("同步拉取失败", e); }
    }

    public SyncResult syncPushWithResult(String remoteDir) {
        Path dst = Paths.get(remoteDir);
        SyncResult r = new SyncResult();
        try (Stream<Path> paths = Files.walk(Paths.get(POLICIES_DIR))) {
            paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    Path out = dst.resolve(Paths.get(POLICIES_DIR).relativize(p).toString());
                    Files.createDirectories(out.getParent());
                    if (!Files.exists(out)) { Files.copy(p, out); r.created++; }
                    else {
                        byte[] a = Files.readAllBytes(p), b = Files.readAllBytes(out);
                        if (!java.util.Arrays.equals(a, b)) { Files.copy(p, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING); r.updated++; }
                        else { r.skipped++; }
                    }
                } catch (IOException ex) { throw new RuntimeException(ex); }
            });
            String actor = authService.currentUser();
            auditService.recordCtx(actor, "push", remoteDir, String.format("created=%d updated=%d skipped=%d", r.created,r.updated,r.skipped), requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
            return r;
        } catch (IOException e) { throw new RuntimeException("同步推送失败", e); }
    }
}
