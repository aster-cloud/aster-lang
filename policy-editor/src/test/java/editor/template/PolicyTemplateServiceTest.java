package editor.template;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PolicyTemplateService 单元测试
 * 验证模板加载与查询功能
 */
@QuarkusTest
class PolicyTemplateServiceTest {

    @Inject
    PolicyTemplateService templateService;

    @Test
    void testTemplatesLoaded() {
        List<PolicyTemplate> templates = templateService.getTemplates();

        assertNotNull(templates, "模板列表不应为 null");
        assertEquals(10, templates.size(), "应加载 10 个模板");
    }

    @Test
    void testTemplateContent() {
        List<PolicyTemplate> templates = templateService.getTemplates();

        // 验证所有模板都有有效内容
        for (PolicyTemplate template : templates) {
            assertNotNull(template.name(), "模板名称不应为 null");
            assertFalse(template.name().isBlank(), "模板名称不应为空");

            assertNotNull(template.description(), "模板描述不应为 null");
            assertFalse(template.description().isBlank(), "模板描述不应为空");

            assertNotNull(template.category(), "模板分类不应为 null");
            assertFalse(template.category().isBlank(), "模板分类不应为空");

            assertNotNull(template.content(), "模板内容不应为 null");
            assertFalse(template.content().isBlank(), "模板内容不应为空");

            // 验证模板内容是有效的 Aster 代码（以 "This module" 开头）
            assertTrue(template.content().contains("This module"),
                "模板内容应包含 'This module' 关键字: " + template.name());
        }
    }

    @Test
    void testFindByName() {
        Optional<PolicyTemplate> template = templateService.findByName("Loan Approval");

        assertTrue(template.isPresent(), "应找到 'Loan Approval' 模板");
        assertEquals("Loan Approval", template.get().name());
        assertEquals("Lending", template.get().category());
        assertNotNull(template.get().content());
    }

    @Test
    void testFindByNameCaseInsensitive() {
        Optional<PolicyTemplate> template1 = templateService.findByName("loan approval");
        Optional<PolicyTemplate> template2 = templateService.findByName("LOAN APPROVAL");
        Optional<PolicyTemplate> template3 = templateService.findByName("LoAn ApPrOvAl");

        assertTrue(template1.isPresent(), "小写名称应能找到模板");
        assertTrue(template2.isPresent(), "大写名称应能找到模板");
        assertTrue(template3.isPresent(), "混合大小写名称应能找到模板");

        assertEquals("Loan Approval", template1.get().name());
        assertEquals("Loan Approval", template2.get().name());
        assertEquals("Loan Approval", template3.get().name());
    }

    @Test
    void testFindByNameNotFound() {
        Optional<PolicyTemplate> template = templateService.findByName("Nonexistent Template");

        assertFalse(template.isPresent(), "不存在的模板应返回空 Optional");
    }

    @Test
    void testFindByNameNull() {
        Optional<PolicyTemplate> template = templateService.findByName(null);

        assertFalse(template.isPresent(), "null 名称应返回空 Optional");
    }

    @Test
    void testFindByNameEmpty() {
        Optional<PolicyTemplate> template = templateService.findByName("");

        assertFalse(template.isPresent(), "空字符串名称应返回空 Optional");
    }

    @Test
    void testTemplateCategories() {
        List<PolicyTemplate> templates = templateService.getTemplates();

        long lendingCount = templates.stream()
            .filter(t -> "Lending".equals(t.category()))
            .count();

        long paymentsCount = templates.stream()
            .filter(t -> "Payments".equals(t.category()))
            .count();

        long insuranceCount = templates.stream()
            .filter(t -> "Insurance".equals(t.category()))
            .count();

        assertEquals(2, lendingCount, "应有 2 个 Lending 模板");
        assertEquals(2, paymentsCount, "应有 2 个 Payments 模板");
        assertEquals(3, insuranceCount, "应有 3 个 Insurance 模板");
    }

    @Test
    void testSpecificTemplates() {
        // 验证所有 10 个模板都能通过名称找到
        String[] expectedTemplates = {
            "Loan Approval",
            "Credit Card Upgrade",
            "Real-time Fraud",
            "Healthcare Claims",
            "Auto Insurance Quote",
            "SMB Working Capital",
            "Travel Insurance",
            "Chargeback Playbook",
            "Wealth Risk Profiling",
            "Usage Based Auto"
        };

        for (String templateName : expectedTemplates) {
            Optional<PolicyTemplate> template = templateService.findByName(templateName);
            assertTrue(template.isPresent(),
                "应找到模板: " + templateName);
        }
    }

    @Test
    void testTemplatesImmutable() {
        List<PolicyTemplate> templates1 = templateService.getTemplates();
        List<PolicyTemplate> templates2 = templateService.getTemplates();

        // 应返回同一个不可变列表
        assertSame(templates1, templates2, "多次调用应返回同一个列表实例");

        // 验证列表不可修改
        assertThrows(UnsupportedOperationException.class, () -> {
            templates1.add(new PolicyTemplate("Test", "Test", "Test", "Test"));
        }, "模板列表应不可修改");
    }
}
