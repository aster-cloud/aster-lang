package editor.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * 表示单个策略规则，包含资源类型和允许的模式列表
 */
public final class PolicyRule {
    private final String resourceType;
    private final List<String> patterns;

    @JsonCreator
    public PolicyRule(
            @JsonProperty("resourceType") String resourceType,
            @JsonProperty("patterns") List<String> patterns) {
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType 不能为空");
        this.patterns = Objects.requireNonNull(patterns, "patterns 不能为空");
    }

    public String getResourceType() {
        return resourceType;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyRule that = (PolicyRule) o;
        return Objects.equals(resourceType, that.resourceType) &&
               Objects.equals(patterns, that.patterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, patterns);
    }

    @Override
    public String toString() {
        return "PolicyRule{" +
               "resourceType='" + resourceType + '\'' +
               ", patterns=" + patterns +
               '}';
    }
}
