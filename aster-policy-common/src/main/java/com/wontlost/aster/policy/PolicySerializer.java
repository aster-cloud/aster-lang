package com.wontlost.aster.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 策略序列化器 - 支持 JSON 和 Aster CNL 格式互转
 */
public class PolicySerializer {
    private final ObjectMapper objectMapper;

    public PolicySerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 将策略对象序列化为 JSON 字符串
     */
    public String toJson(Object policy) {
        try {
            return objectMapper.writeValueAsString(policy);
        } catch (JsonProcessingException e) {
            throw new PolicySerializationException("Failed to serialize policy to JSON", e);
        }
    }

    /**
     * 将策略对象转换为 Aster CNL 格式
     * 通过 ProcessBuilder 调用 aster-convert CLI
     *
     * @param policy 策略对象或 JSON 字符串
     */
    public String toCNL(Object policy) {
        try {
            // 如果输入已经是 JSON 字符串，直接使用；否则序列化
            String json = (policy instanceof String) ? (String) policy : toJson(policy);

            // 根据 CLI 路径类型构建命令
            String cliPath = resolveCliPath();
            ProcessBuilder pb;
            if (cliPath.endsWith(".js")) {
                // .js 文件需要通过 node 执行
                pb = new ProcessBuilder("node", cliPath, "json-to-cnl", "-");
            } else {
                // 可执行文件或 npm shim 直接执行
                pb = new ProcessBuilder(cliPath, "json-to-cnl", "-");
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 写入 JSON 到 stdin
            try (OutputStream os = process.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            // 读取 CNL 输出
            String cnl = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 等待进程完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new PolicySerializationException("CLI failed with exit code " + exitCode + ": " + cnl, null);
            }

            return cnl;
        } catch (IOException e) {
            throw new PolicySerializationException("Failed to execute aster-convert CLI", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PolicySerializationException("CLI execution was interrupted", e);
        }
    }

    /**
     * 从 JSON 字符串反序列化为策略对象
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new PolicySerializationException("Failed to deserialize policy from JSON", e);
        }
    }

    /**
     * 解析 Aster CLI 路径
     * 优先级：
     * 1. 环境变量 ASTER_CLI_PATH
     * 2. 项目根目录 node_modules/.bin/aster-convert
     * 3. 项目根目录 dist/src/cli/policy-converter.js
     * 4. 全局 aster-convert
     */
    private String resolveCliPath() {
        // 1. 环境变量优先
        String envPath = System.getenv("ASTER_CLI_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }

        // 2. 查找项目根目录（package.json 所在位置）
        Path projectRoot = findProjectRoot(Paths.get("").toAbsolutePath());

        if (projectRoot != null) {
            // 尝试 node_modules/.bin/aster-convert
            Path localCli = projectRoot.resolve("node_modules/.bin/aster-convert");
            if (Files.exists(localCli)) {
                return localCli.toAbsolutePath().toString();
            }

            // 尝试直接使用编译后的 CLI
            Path distCli = projectRoot.resolve("dist/src/cli/policy-converter.js");
            if (Files.exists(distCli)) {
                return distCli.toAbsolutePath().toString();
            }
        }

        // 3. 回退到全局命令（生产环境）
        return "aster-convert";
    }

    /**
     * 向上搜索项目根目录（package.json 所在目录）
     */
    private Path findProjectRoot(Path startDir) {
        Path current = startDir;
        while (current != null) {
            Path packageJson = current.resolve("package.json");
            if (Files.exists(packageJson)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 从 Aster CNL 格式转换为策略对象
     * 通过 ProcessBuilder 调用 aster-convert CLI
     */
    public <T> T fromCNL(String cnl, Class<T> clazz) {
        try {
            // 根据 CLI 路径类型构建命令
            String cliPath = resolveCliPath();
            ProcessBuilder pb;
            if (cliPath.endsWith(".js")) {
                // .js 文件需要通过 node 执行
                pb = new ProcessBuilder("node", cliPath, "compile-to-json", "-");
            } else {
                // 可执行文件或 npm shim 直接执行
                pb = new ProcessBuilder(cliPath, "compile-to-json", "-");
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 写入 CNL 到 stdin
            try (OutputStream os = process.getOutputStream()) {
                os.write(cnl.getBytes(StandardCharsets.UTF_8));
            }

            // 读取 JSON 输出
            String json = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 等待进程完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new PolicySerializationException("CLI failed with exit code " + exitCode + ": " + json, null);
            }

            // 反序列化 JSON 为对象
            return fromJson(json, clazz);
        } catch (IOException e) {
            throw new PolicySerializationException("Failed to execute aster-convert CLI", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PolicySerializationException("CLI execution was interrupted", e);
        }
    }

    /**
     * 策略序列化异常
     */
    public static class PolicySerializationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PolicySerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
