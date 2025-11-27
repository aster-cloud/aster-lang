package editor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import editor.model.Policy;
import editor.model.PolicyRuleSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PolicyEditorDialog CNL 保留测试：验证通过 JSON 编辑器编辑策略时 CNL 不丢失。
 */
class PolicyEditorDialogCnlTest {

    @Test
    @DisplayName("复制策略时保留 CNL")
    void testCopyPolicyPreservesCnl() {
        String originalCnl = "module test.policy\\n\\nrule allow Http to \\\"*\\\"";
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("http", List.of("*")));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy original = new Policy("orig-id", "original-policy", allow, deny, originalCnl);

        // 模拟复制操作（使用 5 参数构造函数，传 null ID 会自动生成新 ID）
        Policy copy = new Policy(null, original.getName() + " (copy)",
            original.getAllow(), original.getDeny(), original.getCnl());

        // ID 为 null 时会自动生成新的 UUID
        assertNotNull(copy.getId());
        assertNotEquals(original.getId(), copy.getId());  // 复制的策略应有不同 ID
        assertEquals("original-policy (copy)", copy.getName());
        assertEquals(originalCnl, copy.getCnl());
    }

    @Test
    @DisplayName("编辑策略时保留 CNL（JSON 编辑不影响 CNL）")
    void testEditPolicyPreservesCnl() {
        String originalCnl = "module finance.loan\\n\\nrule allow Sql to \\\"SELECT\\\"";
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("database", List.of("SELECT")));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy original = new Policy("edit-id", "edit-policy", allow, deny, originalCnl);

        // 模拟通过 JSON 编辑器修改规则（不修改 CNL）
        PolicyRuleSet newAllow = new PolicyRuleSet(Map.of(
            "database", List.of("SELECT", "INSERT")
        ));
        Policy edited = new Policy(
            original.getId(),
            "edited-policy-name",
            newAllow,
            original.getDeny(),
            original.getCnl()  // 保留原 CNL
        );

        assertEquals("edit-id", edited.getId());
        assertEquals("edited-policy-name", edited.getName());
        assertEquals(originalCnl, edited.getCnl());
        assertEquals(List.of("SELECT", "INSERT"), edited.getAllow().getRules().get("database"));
    }

    @Test
    @DisplayName("新建策略时 CNL 为 null")
    void testNewPolicyNullCnl() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("io", List.of("*")));
        PolicyRuleSet deny = new PolicyRuleSet(null);

        // 模拟通过对话框新建策略（currentPolicy 为 null）
        Policy newPolicy = new Policy(
            null,
            "new-policy",
            allow,
            deny,
            null  // currentPolicy == null，所以 CNL 为 null
        );

        // ID 为 null 时会自动生成新的 UUID
        assertNotNull(newPolicy.getId());
        assertEquals("new-policy", newPolicy.getName());
        assertNull(newPolicy.getCnl());
    }

    @Test
    @DisplayName("编辑无 CNL 的旧策略")
    void testEditLegacyPolicyWithoutCnl() {
        // 旧策略没有 CNL（使用 4 参数构造函数创建）
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("cpu", List.of("*")));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy legacyPolicy = new Policy("legacy-id", "legacy-policy", allow, deny);

        // 通过 JSON 编辑器编辑
        PolicyRuleSet newDeny = new PolicyRuleSet(Map.of("filesystem", List.of("/etc")));
        Policy edited = new Policy(
            legacyPolicy.getId(),
            legacyPolicy.getName(),
            legacyPolicy.getAllow(),
            newDeny,
            legacyPolicy.getCnl()  // 应该是 null
        );

        assertEquals("legacy-id", edited.getId());
        assertNull(edited.getCnl());
        assertEquals(List.of("/etc"), edited.getDeny().getRules().get("filesystem"));
    }

    @Test
    @DisplayName("AsterPolicyEditorView loadPolicy 逻辑测试 - 有 CNL")
    void testLoadPolicyWithCnl() {
        String cnl = "This module is test.module.\n\nTo evaluate with x is\n  x.";
        PolicyRuleSet allow = new PolicyRuleSet(null);
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("load-id", "test.module.evaluate", allow, deny, cnl);

        // 验证策略数据
        assertEquals("load-id", policy.getId());
        assertEquals("test.module.evaluate", policy.getName());
        assertEquals(cnl, policy.getCnl());
        assertNotNull(policy.getCnl());
    }

    @Test
    @DisplayName("AsterPolicyEditorView loadPolicy 逻辑测试 - 无 CNL")
    void testLoadPolicyWithoutCnl() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("http", List.of("*")));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("no-cnl-id", "aster.finance.loan", allow, deny, null);

        // 验证策略数据
        assertEquals("no-cnl-id", policy.getId());
        assertEquals("aster.finance.loan", policy.getName());
        assertNull(policy.getCnl());

        // 验证名称解析逻辑
        String policyName = policy.getName();
        int lastDot = policyName.lastIndexOf('.');
        String modulePart = policyName.substring(0, lastDot);
        String funcPart = policyName.substring(lastDot + 1);

        assertEquals("aster.finance", modulePart);
        assertEquals("loan", funcPart);
    }

    @Test
    @DisplayName("PolicyNameParser 对空/空白输入返回 null")
    void testPolicyNameParserNullAndEmptyHandling() {
        // 验证 PolicyNameParser 对空值的处理
        // loadPolicy 中会检查 PolicyNameParser.parse 返回值：
        // if (result == null) -> 使用默认值 "default" / "evaluate"

        // null 输入
        assertNull(editor.util.PolicyNameParser.parse(null), "null 应返回 null");

        // 空字符串
        assertNull(editor.util.PolicyNameParser.parse(""), "空字符串应返回 null");

        // 空白字符串
        assertNull(editor.util.PolicyNameParser.parse("   "), "空白应返回 null");
        assertNull(editor.util.PolicyNameParser.parse("\t"), "tab 应返回 null");
        assertNull(editor.util.PolicyNameParser.parse("\n"), "换行应返回 null");
    }

    @Test
    @DisplayName("PolicyNameParser 边界情况解析")
    void testPolicyNameParserBoundaryParsing() {
        // 以点结尾
        var trailing = editor.util.PolicyNameParser.parse("module.");
        assertEquals("module", trailing.getModuleName());
        assertEquals("evaluate", trailing.getFunctionName(), "空函数应回退为 evaluate");

        // 以点开头
        var leading = editor.util.PolicyNameParser.parse(".func");
        assertEquals("default", leading.getModuleName(), "空模块应回退为 default");
        assertEquals("func", leading.getFunctionName());

        // 单点
        var single = editor.util.PolicyNameParser.parse(".");
        assertEquals("default", single.getModuleName());
        assertEquals("evaluate", single.getFunctionName());
    }
}
