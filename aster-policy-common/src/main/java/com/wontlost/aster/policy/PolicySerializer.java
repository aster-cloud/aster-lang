package com.wontlost.aster.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
     * 注意：当前为占位符实现，等待 aster-compiler 提供正式接口
     */
    public String toCNL(Object policy) {
        // TODO: 实现完整的 JSON → Aster CNL 转换
        // 当前返回简化的 CNL 格式作为占位符
        try {
            String json = toJson(policy);
            return "// Placeholder CNL representation\n// TODO: Implement full JSON -> CNL conversion\n" + json;
        } catch (Exception e) {
            throw new PolicySerializationException("Failed to convert policy to CNL", e);
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
     * 从 Aster CNL 格式转换为策略对象
     * 注意：当前为占位符实现，等待 aster-compiler 提供正式接口
     */
    public <T> T fromCNL(String cnl, Class<T> clazz) {
        // TODO: 实现完整的 Aster CNL → JSON 转换
        // 当前假设 CNL 包含嵌入的 JSON（去除注释行）
        try {
            String[] lines = cnl.split("\n");
            StringBuilder jsonBuilder = new StringBuilder();
            for (String line : lines) {
                if (!line.trim().startsWith("//")) {
                    jsonBuilder.append(line).append("\n");
                }
            }
            return fromJson(jsonBuilder.toString().trim(), clazz);
        } catch (Exception e) {
            throw new PolicySerializationException("Failed to convert CNL to policy", e);
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
