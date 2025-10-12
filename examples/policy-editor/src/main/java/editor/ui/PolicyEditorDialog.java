package editor.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import editor.model.Policy;
import editor.model.PolicyRuleSet;
import editor.service.PolicyService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略编辑对话框
 * <p>
 * 用于添加或编辑策略的对话框组件，包含：
 * - 策略名称输入
 * - Allow 规则 JSON 编辑
 * - Deny 规则 JSON 编辑
 * </p>
 */
public class PolicyEditorDialog extends Dialog {

    private static final long serialVersionUID = 1L;

    private final transient PolicyService policyService;
    private final transient ObjectMapper objectMapper;
    private final TextField nameField;
    private final TextArea allowRulesArea;
    private final TextArea denyRulesArea;
    private final Button saveButton;
    private final Button cancelButton;

    private transient Policy currentPolicy;
    private final transient List<ComponentEventListener<SaveEvent>> saveListeners = new ArrayList<>();

    public PolicyEditorDialog(PolicyService policyService) {
        this.policyService = policyService;
        this.objectMapper = new ObjectMapper();

        // 标题
        setHeaderTitle("添加策略");

        // 表单字段
        nameField = new TextField("策略名称");
        nameField.setRequired(true);
        nameField.setWidth("100%");

        allowRulesArea = new TextArea("Allow 规则 (JSON)");
        allowRulesArea.setPlaceholder("{\n  \"io\": [\"*\"],\n  \"cpu\": [\"*\"]\n}");
        allowRulesArea.setWidth("100%");
        allowRulesArea.setHeight("150px");

        denyRulesArea = new TextArea("Deny 规则 (JSON)");
        denyRulesArea.setPlaceholder("{\n  \"io\": [\"/etc/passwd\"]\n}");
        denyRulesArea.setWidth("100%");
        denyRulesArea.setHeight("150px");

        // 表单布局
        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, allowRulesArea, denyRulesArea);
        formLayout.setWidth("600px");
        add(formLayout);

        // 按钮
        saveButton = new Button("💾 保存", e -> savePolicy());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cancelButton = new Button("取消", e -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        getFooter().add(buttonLayout);

        // 设置为模态对话框
        setModal(true);
        setDraggable(true);
        setResizable(true);
    }

    /**
     * 打开对话框
     *
     * @param policy 要编辑的策略，null 表示创建新策略
     */
    public void open(Policy policy) {
        this.currentPolicy = policy;

        if (policy == null) {
            // 添加新策略
            setHeaderTitle("添加策略");
            nameField.clear();
            allowRulesArea.setValue("{\n  \"io\": [\"*\"],\n  \"cpu\": [\"*\"]\n}");
            denyRulesArea.setValue("{}");
        } else {
            // 编辑现有策略
            setHeaderTitle("编辑策略");
            nameField.setValue(policy.getName());
            try {
                allowRulesArea.setValue(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(policy.getAllow().getRules()));
                denyRulesArea.setValue(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(policy.getDeny().getRules()));
            } catch (JsonProcessingException e) {
                Notification.show(
                        "❌ JSON 格式化失败: " + e.getMessage(),
                        3000,
                        Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }

        super.open();
    }

    /**
     * 保存策略
     */
    private void savePolicy() {
        // 验证输入
        if (nameField.isEmpty()) {
            Notification.show(
                    "❌ 策略名称不能为空",
                    3000,
                    Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            // 解析 JSON
            Map<String, List<String>> allowRules = parseRulesJson(allowRulesArea.getValue());
            Map<String, List<String>> denyRules = parseRulesJson(denyRulesArea.getValue());

            // 创建或更新策略
            Policy policy = new Policy(
                    currentPolicy != null ? currentPolicy.getId() : null,
                    nameField.getValue(),
                    new PolicyRuleSet(allowRules),
                    new PolicyRuleSet(denyRules)
            );

            if (currentPolicy == null) {
                // 创建新策略
                policyService.createPolicy(policy);
            } else {
                // 更新现有策略
                policyService.updatePolicy(currentPolicy.getId(), policy);
            }

            // 触发保存事件
            fireSaveEvent();

            // 关闭对话框
            close();

        } catch (JsonProcessingException e) {
            Notification.show(
                    "❌ JSON 格式错误: " + e.getMessage(),
                    3000,
                    Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification.show(
                    "❌ 保存失败: " + e.getMessage(),
                    3000,
                    Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * 解析规则 JSON
     *
     * @param json JSON 字符串
     * @return 规则映射
     * @throws JsonProcessingException JSON 解析异常
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> parseRulesJson(String json) throws JsonProcessingException {
        if (json == null || json.trim().isEmpty() || json.trim().equals("{}")) {
            return new HashMap<>();
        }
        return objectMapper.readValue(json, Map.class);
    }

    /**
     * 添加保存事件监听器
     *
     * @param listener 监听器
     */
    public void addSaveListener(ComponentEventListener<SaveEvent> listener) {
        saveListeners.add(listener);
    }

    /**
     * 触发保存事件
     */
    private void fireSaveEvent() {
        SaveEvent event = new SaveEvent(this);
        saveListeners.forEach(listener -> listener.onComponentEvent(event));
    }

    /**
     * 保存事件
     */
    public static class SaveEvent extends com.vaadin.flow.component.ComponentEvent<PolicyEditorDialog> {
        private static final long serialVersionUID = 1L;

        public SaveEvent(PolicyEditorDialog source) {
            super(source, false);
        }
    }
}
