package editor.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 表示一组策略规则（allow 或 deny）
 */
public final class PolicyRuleSet {
    private final Map<String, List<String>> rules;

    @JsonCreator
    public PolicyRuleSet(@JsonProperty Map<String, List<String>> rules) {
        this.rules = rules != null ? new HashMap<>(rules) : new HashMap<>();
    }

    public Map<String, List<String>> getRules() {
        return new HashMap<>(rules);
    }

    public void addRule(String resourceType, List<String> patterns) {
        rules.put(resourceType, patterns);
    }

    public void removeRule(String resourceType) {
        rules.remove(resourceType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyRuleSet that = (PolicyRuleSet) o;
        return Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rules);
    }

    @Override
    public String toString() {
        return "PolicyRuleSet{" +
               "rules=" + rules +
               '}';
    }
}
