package editor.template;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 模板服务：在启动时加载 classpath 下的 .aster 模板，供 UI 选择器复用。
 */
@ApplicationScoped
public class PolicyTemplateService {

    private List<PolicyTemplate> templates = List.of();

    @PostConstruct
    void init() {
        List<TemplateDescriptor> descriptors = List.of(
            new TemplateDescriptor("Loan Approval", "分档审批 + 现金流检查", "Lending", "loan-approval.aster"),
            new TemplateDescriptor("Credit Card Upgrade", "信用卡升级与限额调整", "Payments", "credit-card-tiering.aster"),
            new TemplateDescriptor("Real-time Fraud", "支付行为风险评分", "Fraud", "fraud-detection.aster"),
            new TemplateDescriptor("Healthcare Claims", "高额医疗理赔分流", "Healthcare", "healthcare-claims.aster"),
            new TemplateDescriptor("Auto Insurance Quote", "车险报价分层", "Insurance", "insurance-quote.aster"),
            new TemplateDescriptor("SMB Working Capital", "小微企业循环额度", "Lending", "small-business-line.aster"),
            new TemplateDescriptor("Travel Insurance", "旅行险高风险筛选", "Insurance", "travel-insurance-screening.aster"),
            new TemplateDescriptor("Chargeback Playbook", "商户拒付策略", "Payments", "merchant-chargeback.aster"),
            new TemplateDescriptor("Wealth Risk Profiling", "投资者风险画像", "Wealth", "wealth-risk-profiling.aster"),
            new TemplateDescriptor("Usage Based Auto", "车联网驾驶折扣", "Insurance", "usage-based-auto.aster")
        );

        List<PolicyTemplate> loaded = new ArrayList<>();
        for (TemplateDescriptor descriptor : descriptors) {
            loaded.add(loadTemplate(descriptor));
        }
        templates = Collections.unmodifiableList(loaded);
    }

    public List<PolicyTemplate> getTemplates() {
        return templates;
    }

    public Optional<PolicyTemplate> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return templates.stream()
            .filter(template -> template.name().equalsIgnoreCase(name))
            .findFirst();
    }

    private PolicyTemplate loadTemplate(TemplateDescriptor descriptor) {
        String resourcePath = "templates/" + descriptor.fileName();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("未找到模板文件: " + resourcePath);
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            return new PolicyTemplate(descriptor.name(), descriptor.description(), descriptor.category(), content);
        } catch (IOException e) {
            throw new IllegalStateException("读取模板失败: " + resourcePath, e);
        }
    }

    private record TemplateDescriptor(String name, String description, String category, String fileName) {}
}
