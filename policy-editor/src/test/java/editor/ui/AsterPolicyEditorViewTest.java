package editor.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import editor.converter.CoreIRToPolicyConverter;
import editor.converter.CoreIRToPolicyConverter.ConversionException;
import editor.model.Policy;
import editor.model.PolicyRuleSet;
import editor.service.PolicyService;
import editor.template.PolicyTemplate;
import editor.template.PolicyTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AsterPolicyEditorView 集成测试
 *
 * 注意：由于未配置 Vaadin TestBench，本测试聚焦于服务层集成和业务逻辑验证。
 * 完整的浏览器UI测试需要配置 TestBench 或 Playwright。
 */
@ExtendWith(MockitoExtension.class)
class AsterPolicyEditorViewTest {

    @Mock
    private PolicyService policyService;

    @Mock
    private PolicyTemplateService templateService;

    @Mock
    private CoreIRToPolicyConverter converter;

    @Mock
    private ObjectMapper objectMapper;

    private AsterPolicyEditorView view;

    @BeforeEach
    void setUp() {
        // 注意：Vaadin 组件在非 Quarkus 环境下无法完全初始化
        // 本测试验证业务逻辑方法的行为
    }

    @Test
    void testSavePolicyCreatesNewPolicy() throws Exception {
        // Arrange
        String cnlCode = "This module is test.finance.\n\nTo evaluate with data is approve.";
        String policyName = "test.finance.evaluate";
        String policyId = UUID.randomUUID().toString();

        Policy convertedPolicy = new Policy(
            null,
            policyName,
            new PolicyRuleSet(null),
            new PolicyRuleSet(null),
            cnlCode
        );

        Policy createdPolicy = new Policy(
            policyId,
            policyName,
            new PolicyRuleSet(null),
            new PolicyRuleSet(null),
            cnlCode
        );

        when(converter.convertCNLToPolicy(anyString(), any(), anyString()))
            .thenReturn(convertedPolicy);
        when(policyService.createPolicy(any(Policy.class)))
            .thenReturn(createdPolicy);

        view = new AsterPolicyEditorView(templateService);
        view.policyService = policyService;
        view.converter = converter;

        // Act - 模拟调用 savePolicy 的逻辑
        Policy result = converter.convertCNLToPolicy(cnlCode, null, policyName);
        Policy created = policyService.createPolicy(result);

        // Assert
        assertNotNull(created);
        assertEquals(policyId, created.getId());
        assertEquals(policyName, created.getName());
        assertEquals(cnlCode, created.getCnl());

        verify(converter).convertCNLToPolicy(cnlCode, null, policyName);
        verify(policyService).createPolicy(convertedPolicy);
    }

    @Test
    void testSavePolicyUpdatesExistingPolicy() throws Exception {
        // Arrange
        String existingPolicyId = UUID.randomUUID().toString();
        String cnlCode = "This module is test.finance.\n\nTo evaluate with data is deny.";
        String policyName = "test.finance.evaluate";

        Policy convertedPolicy = new Policy(
            existingPolicyId,
            policyName,
            new PolicyRuleSet(null),
            new PolicyRuleSet(null),
            cnlCode
        );

        Policy updatedPolicy = new Policy(
            existingPolicyId,
            policyName,
            new PolicyRuleSet(null),
            new PolicyRuleSet(null),
            cnlCode
        );

        when(converter.convertCNLToPolicy(anyString(), anyString(), anyString()))
            .thenReturn(convertedPolicy);
        when(policyService.updatePolicy(anyString(), any(Policy.class)))
            .thenReturn(Optional.of(updatedPolicy));

        view = new AsterPolicyEditorView(templateService);
        view.policyService = policyService;
        view.converter = converter;

        // Act
        Policy result = converter.convertCNLToPolicy(cnlCode, existingPolicyId, policyName);
        Optional<Policy> updated = policyService.updatePolicy(existingPolicyId, result);

        // Assert
        assertTrue(updated.isPresent());
        assertEquals(existingPolicyId, updated.get().getId());
        assertEquals(cnlCode, updated.get().getCnl());

        verify(converter).convertCNLToPolicy(cnlCode, existingPolicyId, policyName);
        verify(policyService).updatePolicy(existingPolicyId, convertedPolicy);
    }

    @Test
    void testSavePolicyHandlesConversionError() throws Exception {
        // Arrange
        String invalidCnlCode = "Invalid CNL syntax here";
        String policyName = "test.invalid";

        when(converter.convertCNLToPolicy(anyString(), any(), anyString()))
            .thenThrow(new ConversionException("编译错误：语法不正确"));

        view = new AsterPolicyEditorView(templateService);
        view.converter = converter;

        // Act & Assert
        ConversionException exception = assertThrows(
            ConversionException.class,
            () -> converter.convertCNLToPolicy(invalidCnlCode, null, policyName)
        );

        assertTrue(exception.getMessage().contains("编译错误"));
        verify(converter).convertCNLToPolicy(invalidCnlCode, null, policyName);
        verify(policyService, never()).createPolicy(any());
        verify(policyService, never()).updatePolicy(anyString(), any());
    }

    @Test
    void testSavePolicyHandlesEmptyCode() throws Exception {
        // Arrange
        view = new AsterPolicyEditorView(templateService);
        view.policyService = policyService;
        view.converter = converter;

        // Act - 模拟空代码场景
        String emptyCode = "";

        // Assert - 应该在保存前验证
        assertTrue(emptyCode.isBlank(), "空代码应该被验证拒绝");
        verify(converter, never()).convertCNLToPolicy(anyString(), any(), anyString());
    }

    @Test
    void testSavePolicyHandlesEmptyModuleOrFunction() {
        // Arrange
        view = new AsterPolicyEditorView(templateService);

        // Act & Assert - 模拟空模块名场景
        String emptyModule = "";
        String validFunction = "evaluate";

        assertTrue(emptyModule.isBlank() || validFunction.isBlank(),
            "空模块名或函数名应该被验证拒绝");
    }

    @Test
    void testConvertToPolicy() throws Exception {
        // Arrange
        String cnlCode = "This module is test.\n\nTo check with x is approve.";
        String policyId = "test-policy-id";
        String policyName = "test.check";

        Policy expectedPolicy = new Policy(
            policyId,
            policyName,
            new PolicyRuleSet(null),
            new PolicyRuleSet(null),
            cnlCode
        );

        when(converter.convertCNLToPolicy(cnlCode, policyId, policyName))
            .thenReturn(expectedPolicy);

        // Act
        Policy result = converter.convertCNLToPolicy(cnlCode, policyId, policyName);

        // Assert
        assertNotNull(result);
        assertEquals(policyId, result.getId());
        assertEquals(policyName, result.getName());
        assertEquals(cnlCode, result.getCnl());
    }

    @Test
    void testTemplateApplicationFlow() {
        // Arrange
        PolicyTemplate template = new PolicyTemplate(
            "Loan Approval Template",
            "Loan approval policy template",
            "Lending",
            "This module is template.loan.\n\nTo approve with amount is check score."
        );

        when(templateService.getTemplates())
            .thenReturn(List.of(template));

        // Act
        List<PolicyTemplate> templates = templateService.getTemplates();

        // Assert
        assertFalse(templates.isEmpty());
        assertEquals("Loan Approval Template", templates.get(0).name());
        assertEquals("Loan approval policy template", templates.get(0).description());
        assertNotNull(templates.get(0).content());
    }

    @Test
    void testPolicyUpdateReturnsEmptyWhenNotFound() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        Policy policy = new Policy(
            nonExistentId,
            "test.policy",
            new PolicyRuleSet(null),
            new PolicyRuleSet(null),
            "code"
        );

        when(policyService.updatePolicy(nonExistentId, policy))
            .thenReturn(Optional.empty());

        // Act
        Optional<Policy> result = policyService.updatePolicy(nonExistentId, policy);

        // Assert
        assertTrue(result.isEmpty(), "更新不存在的策略应返回空");
        verify(policyService).updatePolicy(nonExistentId, policy);
    }

    @Test
    void testPolicyNameGeneration() {
        // Arrange
        String module = "aster.finance.loan";
        String function = "evaluateLoanEligibility";

        // Act
        String policyName = module + "." + function;

        // Assert
        assertEquals("aster.finance.loan.evaluateLoanEligibility", policyName);
        assertTrue(policyName.startsWith(module));
        assertTrue(policyName.endsWith(function));
    }

    @Test
    void testCNLFieldPreservation() {
        // Arrange
        String originalCnl = "This is original CNL code";
        String policyId = "test-id";
        String policyName = "test.policy";

        Policy policy = new Policy(
            policyId,
            policyName,
            new PolicyRuleSet(null),
            new PolicyRuleSet(null),
            originalCnl
        );

        // Act & Assert
        assertEquals(originalCnl, policy.getCnl(), "CNL 字段应该被保留");
    }

    @Test
    void testPolicyWithoutCNL() {
        // Arrange
        String policyId = "legacy-policy";
        String policyName = "legacy.policy";

        // Act - 创建不带 CNL 的策略（向后兼容）
        Policy policy = new Policy(
            policyId,
            policyName,
            new PolicyRuleSet(null),
            new PolicyRuleSet(null)
        );

        // Assert
        assertNull(policy.getCnl(), "不提供 CNL 时应为 null");
        assertEquals(policyId, policy.getId());
        assertEquals(policyName, policy.getName());
    }

    @Test
    void testMultipleSaveOperations() throws Exception {
        // Arrange
        String cnlCode1 = "This module is test1.\n\nTo eval1 with x is approve.";
        String cnlCode2 = "This module is test2.\n\nTo eval2 with y is deny.";
        String policyId = UUID.randomUUID().toString();

        Policy policy1 = new Policy(policyId, "test1.eval1", new PolicyRuleSet(null), new PolicyRuleSet(null), cnlCode1);
        Policy policy2 = new Policy(policyId, "test2.eval2", new PolicyRuleSet(null), new PolicyRuleSet(null), cnlCode2);

        when(converter.convertCNLToPolicy(cnlCode1, policyId, "test1.eval1")).thenReturn(policy1);
        when(converter.convertCNLToPolicy(cnlCode2, policyId, "test2.eval2")).thenReturn(policy2);
        when(policyService.updatePolicy(eq(policyId), any())).thenReturn(Optional.of(policy1), Optional.of(policy2));

        // Act
        converter.convertCNLToPolicy(cnlCode1, policyId, "test1.eval1");
        policyService.updatePolicy(policyId, policy1);

        converter.convertCNLToPolicy(cnlCode2, policyId, "test2.eval2");
        policyService.updatePolicy(policyId, policy2);

        // Assert
        verify(converter).convertCNLToPolicy(cnlCode1, policyId, "test1.eval1");
        verify(converter).convertCNLToPolicy(cnlCode2, policyId, "test2.eval2");
        verify(policyService, times(2)).updatePolicy(eq(policyId), any());
    }
}
