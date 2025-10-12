package editor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import editor.model.Policy;
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
        return savePolicy(policyPath, policy);
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
        return Optional.of(savePolicy(policyPath, updatedPolicy));
    }

    /**
     * 删除策略
     */
    public boolean deletePolicy(String id) {
        Path policyPath = getPolicyPath(id);
        try {
            return Files.deleteIfExists(policyPath);
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
}
