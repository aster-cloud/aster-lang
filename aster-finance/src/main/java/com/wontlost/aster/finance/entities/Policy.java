package com.wontlost.aster.finance.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 风控策略实体 - 配置贷款审批规则
 */
public record Policy(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("version") String version,
    @JsonProperty("rules") Map<String, Object> rules,
    @JsonProperty("createdAt") LocalDateTime createdAt
) {
    /**
     * 构造函数 - 验证参数并创建不可变副本
     */
    @JsonCreator
    public Policy {
        Objects.requireNonNull(id, "Policy ID cannot be null");
        Objects.requireNonNull(name, "Policy name cannot be null");
        Objects.requireNonNull(version, "Policy version cannot be null");
        Objects.requireNonNull(rules, "Policy rules cannot be null");
        Objects.requireNonNull(createdAt, "Created time cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Policy ID cannot be blank");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Policy name cannot be blank");
        }

        if (version.isBlank()) {
            throw new IllegalArgumentException("Policy version cannot be blank");
        }

        // 创建不可变副本以保证 Record 不可变性
        rules = Collections.unmodifiableMap(new HashMap<>(rules));
    }

    /**
     * 获取规则值（泛型安全）
     *
     * @param key 规则键
     * @param type 期望的值类型
     * @param <T> 值类型
     * @return 规则值，如果不存在则返回 null
     * @throws ClassCastException 如果类型不匹配
     */
    public <T> T getRule(String key, Class<T> type) {
        Object value = rules.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    /**
     * 获取规则值，如果不存在则返回默认值
     *
     * @param key 规则键
     * @param type 期望的值类型
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 规则值或默认值
     */
    public <T> T getRuleOrDefault(String key, Class<T> type, T defaultValue) {
        T value = getRule(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查是否包含指定规则
     */
    public boolean hasRule(String key) {
        return rules.containsKey(key);
    }

    /**
     * 获取规则数量
     */
    public int ruleCount() {
        return rules.size();
    }

    /**
     * 创建新版本的策略（仅更新版本号和创建时间）
     *
     * @param newVersion 新版本号
     * @return 新策略实例
     */
    public Policy withVersion(String newVersion) {
        return new Policy(id, name, newVersion, rules, LocalDateTime.now());
    }

    /**
     * 创建策略副本并添加/更新规则
     *
     * @param key 规则键
     * @param value 规则值
     * @return 新策略实例
     */
    public Policy withRule(String key, Object value) {
        Map<String, Object> newRules = new HashMap<>(this.rules);
        newRules.put(key, value);
        return new Policy(id, name, version, newRules, createdAt);
    }

    /**
     * 创建策略副本并删除规则
     *
     * @param key 规则键
     * @return 新策略实例
     */
    public Policy withoutRule(String key) {
        Map<String, Object> newRules = new HashMap<>(this.rules);
        newRules.remove(key);
        return new Policy(id, name, version, newRules, createdAt);
    }

    /**
     * Builder 模式
     */
    public static class Builder {
        private String id;
        private String name;
        private String version = "1.0.0";  // 默认版本
        private Map<String, Object> rules = new HashMap<>();
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder rules(Map<String, Object> rules) {
            this.rules = new HashMap<>(rules);
            return this;
        }

        public Builder addRule(String key, Object value) {
            this.rules.put(key, value);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Policy build() {
            return new Policy(id, name, version, rules, createdAt);
        }
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
