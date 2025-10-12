package editor.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.UUID;

/**
 * 表示完整的策略文档，包含 allow 和 deny 规则集
 */
public final class Policy {
    private final String id;
    private final String name;
    private final PolicyRuleSet allow;
    private final PolicyRuleSet deny;

    public Policy(String name) {
        this(UUID.randomUUID().toString(), name, new PolicyRuleSet(null), new PolicyRuleSet(null));
    }

    @JsonCreator
    public Policy(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("allow") PolicyRuleSet allow,
            @JsonProperty("deny") PolicyRuleSet deny) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name, "name 不能为空");
        this.allow = allow != null ? allow : new PolicyRuleSet(null);
        this.deny = deny != null ? deny : new PolicyRuleSet(null);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PolicyRuleSet getAllow() {
        return allow;
    }

    public PolicyRuleSet getDeny() {
        return deny;
    }

    public Policy withName(String newName) {
        return new Policy(this.id, newName, this.allow, this.deny);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Policy policy = (Policy) o;
        return Objects.equals(id, policy.id) &&
               Objects.equals(name, policy.name) &&
               Objects.equals(allow, policy.allow) &&
               Objects.equals(deny, policy.deny);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, allow, deny);
    }

    @Override
    public String toString() {
        return "Policy{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", allow=" + allow +
               ", deny=" + deny +
               '}';
    }
}
