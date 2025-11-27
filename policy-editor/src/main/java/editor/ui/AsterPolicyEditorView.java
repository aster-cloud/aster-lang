package editor.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.ThemeVariant;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import editor.converter.CoreIRToPolicyConverter;
import editor.converter.CoreIRToPolicyConverter.ConversionException;
import editor.model.Policy;
import editor.model.PolicyRuleSet;
import editor.service.PolicyService;
import editor.template.PolicyTemplate;
import editor.template.PolicyTemplateService;
import editor.util.PolicyNameParser;

import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

/**
 * Aster 策略编辑器（带 Live Preview） - 现代化UI版本
 *
 * 功能：
 * - 编辑 Aster 策略代码（Monaco Editor）
 * - 配置示例输入
 * - 实时预览评估结果（通过 WebSocket）
 * - 300ms 防抖
 * - 自动重连
 * - 现代化卡片式布局
 * - 响应式设计
 */
@PageTitle("Aster Policy Editor")
@Route("aster-editor")
@JsModule("./components/monaco-editor-component.ts")
public class AsterPolicyEditorView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final int DEBOUNCE_MS = 300;
    private static final String DEFAULT_POLICY_SNIPPET = """
        This module is aster.finance.loan.

        To evaluateLoanEligibility with applicant, history is
          When applicant.creditScore is over 720 then
            approve with message "优先客户，建议极速放款".
        """;

    private TextField moduleField;
    private TextField functionField;
    private Element monacoEditorElement;
    private TextArea sampleInputArea;
    private TemplateSelector templateSelector;
    private Div previewResultDiv;
    private Span statusBadge;

    private String webSocketUrl;
    private String policyCodeValue = DEFAULT_POLICY_SNIPPET.trim();

    @Inject
    transient PolicyService policyService;

    @Inject
    transient ObjectMapper objectMapper;

    @Inject
    transient CoreIRToPolicyConverter converter;

    private String currentPolicyId;
    private String originalPolicyName;  // 保存原始策略名，避免无点名称被改变

    @Inject
    public AsterPolicyEditorView(PolicyTemplateService templateService) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName(LumoUtility.Background.CONTRAST_5);

        // === 页面标题 ===
        H2 pageTitle = new H2("Aster Policy Editor");
        pageTitle.getStyle()
            .set("margin", "0")
            .set("color", "var(--lumo-primary-text-color)");

        // === 配置卡片 ===
        Div configCard = createCard();

        moduleField = new TextField("Policy Module");
        moduleField.setValue("aster.finance.loan");
        moduleField.setWidth("250px");

        functionField = new TextField("Policy Function");
        functionField.setValue("evaluateLoanEligibility");
        functionField.setWidth("250px");

        templateSelector = new TemplateSelector(templateService);
        templateSelector.setTemplateApplyListener(this::applyTemplate);

        HorizontalLayout configLayout = new HorizontalLayout(moduleField, functionField, templateSelector);
        configLayout.setAlignItems(FlexComponent.Alignment.END);
        configLayout.setSpacing(true);
        configLayout.setWidthFull();

        configCard.add(configLayout);

        // === 编辑器和预览区 ===
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(60);
        splitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);

        // 左侧：代码编辑区
        VerticalLayout editorPanel = createEditorPanel();

        // 右侧：实时预览区
        VerticalLayout previewPanel = createPreviewPanel();

        splitLayout.addToPrimary(editorPanel);
        splitLayout.addToSecondary(previewPanel);

        // === 操作按钮 ===
        Button saveButton = new Button("保存策略", e -> savePolicy());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button exportButton = new Button("导出 CNL", e -> exportPolicy());
        exportButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button importButton = new Button("导入 CNL", e -> triggerFileImport());

        Button resetButton = new Button("重置", e -> resetEditor());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, exportButton, importButton, resetButton);
        buttonLayout.setSpacing(true);

        add(pageTitle, configCard, splitLayout, buttonLayout);
        setFlexGrow(1, splitLayout);
    }

    /**
     * 创建卡片样式的容器
     */
    private Div createCard() {
        Div card = new Div();
        card.getStyle()
            .set("background", "var(--lumo-base-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-s)")
            .set("padding", "var(--lumo-space-m)");
        return card;
    }

    /**
     * 创建编辑器面板
     */
    private VerticalLayout createEditorPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);
        panel.setSpacing(true);

        // 代码编辑器卡片
        Div editorCard = createCard();
        editorCard.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("flex-grow", "3")
            .set("min-height", "400px");

        H4 editorTitle = new H4("Policy Code");
        editorTitle.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");

        monacoEditorElement = new Element("monaco-editor-component");
        monacoEditorElement.setProperty("value", DEFAULT_POLICY_SNIPPET.trim());
        monacoEditorElement.setProperty("theme", "vs-dark");
        monacoEditorElement.setProperty("fontSize", 14);
        monacoEditorElement.setProperty("minimap", true);
        monacoEditorElement.setProperty("folding", true);
        monacoEditorElement.addEventListener("monaco-value-changed", event -> {
            policyCodeValue = event.getEventData().getString("event.detail.value");
        }).addEventData("event.detail.value");

        Div monacoWrapper = new Div();
        monacoWrapper.getStyle()
            .set("flex-grow", "1")
            .set("width", "100%")
            .set("height", "100%")
            .set("min-height", "350px")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-s)")
            .set("overflow", "hidden");
        monacoWrapper.getElement().appendChild(monacoEditorElement);

        editorCard.add(editorTitle, monacoWrapper);

        // 示例输入卡片
        Div inputCard = createCard();
        inputCard.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("flex-grow", "1")
            .set("min-height", "150px");

        H4 inputTitle = new H4("Sample Input");
        inputTitle.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");

        sampleInputArea = new TextArea();
        sampleInputArea.setValue("[\n  {\"creditScore\": 750, \"income\": 100000, \"loanAmount\": 300000}\n]");
        sampleInputArea.setSizeFull();
        sampleInputArea.getStyle()
            .set("font-family", "monospace")
            .set("font-size", "13px")
            .set("flex-grow", "1");

        inputCard.add(inputTitle, sampleInputArea);

        panel.add(editorCard, inputCard);
        panel.setFlexGrow(3, editorCard);
        panel.setFlexGrow(1, inputCard);

        return panel;
    }

    /**
     * 创建预览面板
     */
    private VerticalLayout createPreviewPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);
        panel.setSpacing(true);

        Div previewCard = createCard();
        previewCard.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("height", "100%")
            .set("min-height", "400px");

        // 头部：仅状态徽章
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        statusBadge = new Span("未连接");
        statusBadge.getElement().getThemeList().add("badge");
        statusBadge.getElement().getThemeList().add("error");
        statusBadge.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
            .set("border-radius", "var(--lumo-border-radius-m)");

        header.add(statusBadge);

        // 预览结果区域
        previewResultDiv = new Div();
        previewResultDiv.getStyle()
            .set("flex-grow", "1")
            .set("width", "100%")
            .set("padding", "var(--lumo-space-m)")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "var(--lumo-border-radius-s)")
            .set("font-family", "monospace")
            .set("font-size", "13px")
            .set("white-space", "pre-wrap")
            .set("overflow-y", "auto")
            .set("min-height", "0");

        previewResultDiv.setText("等待编辑以触发预览...");

        previewCard.add(header, previewResultDiv);

        panel.add(previewCard);
        panel.setFlexGrow(1, previewCard);

        return panel;
    }

    /**
     * 组件附加到UI时，初始化 WebSocket 连接
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        UI ui = attachEvent.getUI();

        // 获取 WebSocket URL（ws:// 或 wss://）
        VaadinServletRequest request = (VaadinServletRequest) VaadinService.getCurrentRequest();

        String protocol = request.isSecure() ? "wss" : "ws";
        String host = request.getServerName();
        int port = request.getServerPort();

        webSocketUrl = String.format("%s://%s:%d/ws/preview", protocol, host, port);

        // 初始化 WebSocket 客户端（通过 JavaScript）
        initWebSocket(ui);

        // 设置防抖监听器
        setupDebounce(ui);
    }

    /**
     * 组件分离时，关闭 WebSocket 连接
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        // 关闭 WebSocket
        detachEvent.getUI().getPage().executeJs("if (window.aster_ws) { window.aster_ws.close(); }");
    }

    /**
     * 初始化 WebSocket 连接
     */
    private void initWebSocket(UI ui) {
        String script = """
            (function() {
                const url = '%s';
                let ws = null;
                let reconnectTimer = null;

                function connect() {
                    ws = new WebSocket(url);

                    ws.onopen = function() {
                        console.log('WebSocket connected');
                        $0.$server.updateConnectionStatus('connected');
                        if (reconnectTimer) {
                            clearTimeout(reconnectTimer);
                            reconnectTimer = null;
                        }
                    };

                    ws.onmessage = function(event) {
                        console.log('WebSocket message:', event.data);
                        const data = JSON.parse(event.data);
                        $0.$server.handlePreviewResponse(event.data);
                    };

                    ws.onerror = function(error) {
                        console.error('WebSocket error:', error);
                        $0.$server.updateConnectionStatus('error');
                    };

                    ws.onclose = function() {
                        console.log('WebSocket closed');
                        $0.$server.updateConnectionStatus('disconnected');

                        // 自动重连（3秒后）
                        reconnectTimer = setTimeout(function() {
                            console.log('Attempting to reconnect...');
                            connect();
                        }, 3000);
                    };

                    window.aster_ws = ws;
                }

                connect();

                // 发送预览请求的函数
                window.sendPreviewRequest = function(module, func, context) {
                    if (ws && ws.readyState === WebSocket.OPEN) {
                        const request = {
                            policyModule: module,
                            policyFunction: func,
                            context: JSON.parse(context)
                        };
                        ws.send(JSON.stringify(request));
                    } else {
                        console.warn('WebSocket not ready, readyState:', ws ? ws.readyState : 'null');
                    }
                };
            })();
            """.formatted(webSocketUrl);

        ui.getPage().executeJs(script, getElement());
    }

    /**
     * 设置防抖监听器
     */
    private void setupDebounce(UI ui) {
        // 监听输入组件变化，触发防抖预览
        String debounceScript = """
            (function(host, target) {
                let debounceTimer = null;

                target.addEventListener('value-changed', function() {
                    if (debounceTimer) {
                        clearTimeout(debounceTimer);
                    }

                    debounceTimer = setTimeout(function() {
                        host.$server.triggerPreview();
                    }, %d);
                });
            })($0, $1);
            """.formatted(DEBOUNCE_MS);

        ui.getPage().executeJs(debounceScript, getElement(), monacoEditorElement);
        ui.getPage().executeJs(debounceScript, getElement(), sampleInputArea.getElement());
    }

    /**
     * 更新连接状态（从 JavaScript 回调）
     */
    @ClientCallable
    public void updateConnectionStatus(String status) {
        getUI().ifPresent(ui -> ui.access(() -> {
            switch (status) {
                case "connected":
                    statusBadge.setText("已连接");
                    statusBadge.getElement().getThemeList().remove("error");
                    statusBadge.getElement().getThemeList().remove("contrast");
                    statusBadge.getElement().getThemeList().add("success");
                    break;
                case "disconnected":
                    statusBadge.setText("已断开");
                    statusBadge.getElement().getThemeList().remove("success");
                    statusBadge.getElement().getThemeList().remove("contrast");
                    statusBadge.getElement().getThemeList().add("error");
                    break;
                case "error":
                    statusBadge.setText("连接错误");
                    statusBadge.getElement().getThemeList().remove("success");
                    statusBadge.getElement().getThemeList().remove("error");
                    statusBadge.getElement().getThemeList().add("contrast");
                    break;
            }
        }));
    }

    /**
     * 处理预览响应（从 JavaScript 回调）
     */
    @ClientCallable
    public void handlePreviewResponse(String jsonResponse) {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode response = mapper.readTree(jsonResponse);

                String status = response.path("status").asText();
                String message = response.path("message").asText();
                long executionTime = response.path("executionTime").asLong(0);

                if ("success".equals(status)) {
                    String resultJson = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(response.path("result"));

                    previewResultDiv.getStyle()
                        .set("background", "var(--lumo-success-color-10pct)")
                        .set("border-color", "var(--lumo-success-color-50pct)");
                    previewResultDiv.setText(String.format("✅ 评估成功 (%dms)\n\n%s",
                        executionTime, resultJson));
                } else if ("error".equals(status)) {
                    previewResultDiv.getStyle()
                        .set("background", "var(--lumo-error-color-10pct)")
                        .set("border-color", "var(--lumo-error-color-50pct)");
                    previewResultDiv.setText(String.format("❌ 评估失败 (%dms)\n\n%s",
                        executionTime, message));
                } else {
                    previewResultDiv.getStyle()
                        .set("background", "var(--lumo-contrast-5pct)")
                        .set("border-color", "var(--lumo-contrast-10pct)");
                    previewResultDiv.setText(message);
                }

            } catch (Exception e) {
                previewResultDiv.getStyle()
                    .set("background", "var(--lumo-error-color-10pct)")
                    .set("border-color", "var(--lumo-error-color-50pct)");
                previewResultDiv.setText("❌ 响应解析错误: " + e.getMessage());
            }
        }));
    }

    /**
     * 触发预览（防抖后调用）
     */
    @ClientCallable
    public void triggerPreview() {
        String module = moduleField.getValue();
        String function = functionField.getValue();
        String context = sampleInputArea.getValue();

        if (module == null || module.isBlank() || function == null || function.isBlank()) {
            return;
        }

        // 通过 JavaScript 发送 WebSocket 请求
        getUI().ifPresent(ui -> {
            ui.getPage().executeJs("window.sendPreviewRequest($0, $1, $2)",
                module, function, context);
        });
    }

    /**
     * 保存策略
     */
    private void savePolicy() {
        String code = getPolicyCode();
        if (code == null || code.isBlank()) {
            showError("策略代码不能为空");
            return;
        }

        String module = moduleField.getValue();
        String function = functionField.getValue();
        if (module == null || module.isBlank() || function == null || function.isBlank()) {
            showError("模块名和函数名不能为空");
            return;
        }

        try {
            // 计算策略名称，保持无点名称的向后兼容
            String policyName = computePolicyName(module, function);

            // 使用 CoreIRToPolicyConverter 将 CNL 转换为 Policy 对象
            Policy policy = converter.convertCNLToPolicy(code, currentPolicyId, policyName);

            if (currentPolicyId == null) {
                Policy created = policyService.createPolicy(policy);
                currentPolicyId = created.getId();
                showSuccess("策略已创建: " + created.getName());
            } else {
                Optional<Policy> updated = policyService.updatePolicy(currentPolicyId, policy);
                if (updated.isPresent()) {
                    showSuccess("策略已更新: " + updated.get().getName());
                } else {
                    showError("更新失败：策略不存在");
                }
            }
        } catch (ConversionException e) {
            showError("CNL 编译错误: " + e.getMessage());
        } catch (Exception e) {
            showError("保存失败: " + e.getMessage());
        }
    }

    /**
     * 计算策略名称，保持无点名称的向后兼容。
     * 如果原始名称没有点（如 "alpha"），且字段未被用户修改，则保留原始名称。
     * 否则使用 module.function 格式。
     */
    private String computePolicyName(String module, String function) {
        // 检查是否需要保留原始无点名称
        if (originalPolicyName != null && !originalPolicyName.contains(".")) {
            // 原始名称无点，检查字段是否匹配解析结果
            // 解析 "alpha" 会得到 module="alpha", function="evaluate"
            if (originalPolicyName.equals(module) && "evaluate".equals(function)) {
                // 字段未修改，保留原始名称
                return originalPolicyName;
            }
        }
        // 默认：使用 module.function 格式
        return module + "." + function;
    }

    /**
     * 显示错误通知
     */
    private void showError(String message) {
        Notification.show("❌ " + message, 3000, Notification.Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /**
     * 显示成功通知
     */
    private void showSuccess(String message) {
        Notification.show("✅ " + message, 2000, Notification.Position.BOTTOM_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * 导出策略为 .aster 文件
     */
    private void exportPolicy() {
        String code = getPolicyCode();

        if (code == null || code.isBlank()) {
            Notification.show("策略代码不能为空", 2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // 生成文件名：module.function.aster
        String module = moduleField.getValue();
        String function = functionField.getValue();
        String fileName = (module != null && !module.isBlank() && function != null && !function.isBlank())
            ? module + "." + function + ".aster"
            : "policy.aster";

        // 使用 JavaScript 创建下载链接
        byte[] bytes = code.getBytes(StandardCharsets.UTF_8);
        String base64 = java.util.Base64.getEncoder().encodeToString(bytes);

        getUI().ifPresent(ui -> {
            String script = String.format(
                "const link = document.createElement('a');" +
                "link.href = 'data:text/plain;base64,%s';" +
                "link.download = '%s';" +
                "link.click();",
                base64, fileName
            );
            ui.getPage().executeJs(script);
        });

        Notification.show("策略已导出: " + fileName, 2000, Notification.Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * 触发文件导入（通过 JavaScript）
     */
    private void triggerFileImport() {
        getUI().ifPresent(ui -> {
            String script = """
                const input = document.createElement('input');
                input.type = 'file';
                input.accept = '.aster';
                input.onchange = function(e) {
                    const file = e.target.files[0];
                    if (file) {
                        const reader = new FileReader();
                        reader.onload = function(event) {
                            const content = event.target.result;
                            $0.$server.handleImportedFile(file.name, content);
                        };
                        reader.readAsText(file);
                    }
                };
                input.click();
                """;
            ui.getPage().executeJs(script, getElement());
        });
    }

    /**
     * 处理导入的文件（从 JavaScript 回调）
     */
    @ClientCallable
    public void handleImportedFile(String fileName, String content) {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                if (content == null || content.isBlank()) {
                    Notification.show("导入的文件为空", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                // 加载到编辑器
                updateEditorContent(content);

                updateModuleFieldFromContent(content);

                Notification.show("策略已导入: " + fileName, 2000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception e) {
                Notification.show("导入失败: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }));
    }

    /**
     * 重置编辑器
     */
    private void resetEditor() {
        updateEditorContent(DEFAULT_POLICY_SNIPPET.trim());
        sampleInputArea.setValue("[\n  {}\n]");
        previewResultDiv.setText("等待编辑以触发预览...");
        previewResultDiv.getStyle()
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-color", "var(--lumo-contrast-10pct)");
    }

    private void applyTemplate(PolicyTemplate template) {
        if (template == null) {
            return;
        }
        updateEditorContent(template.content());
        updateModuleFieldFromContent(template.content());
        Notification.show("已应用模板: " + template.name(), 2000, Notification.Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private String getPolicyCode() {
        return policyCodeValue == null ? "" : policyCodeValue;
    }

    private void updateEditorContent(String content) {
        policyCodeValue = (content == null) ? "" : content;
        getUI().ifPresent(ui ->
            monacoEditorElement.callJsFunction("setValue", policyCodeValue)
        );
    }

    private void updateModuleFieldFromContent(String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String firstLine = content.lines().findFirst().orElse("");
        String prefix = "This module is ";
        if (firstLine.startsWith(prefix) && firstLine.endsWith(".")) {
            String moduleName = firstLine.substring(prefix.length(), firstLine.length() - 1);
            moduleField.setValue(moduleName);
        }
    }

    /**
     * 加载现有策略到编辑器。
     * 用于从策略管理视图打开策略进行 CNL 编辑。
     *
     * @param policy 要加载的策略
     */
    public void loadPolicy(Policy policy) {
        if (policy == null) {
            return;
        }

        // 设置当前策略 ID（用于更新而非创建）
        this.currentPolicyId = policy.getId();

        // 保存原始策略名，用于保存时保持名称一致性
        String policyName = policy.getName();
        this.originalPolicyName = policyName;

        // 从策略名称解析模块和函数名（无论是否有 CNL 都要同步）
        if (policyName == null || policyName.isBlank()) {
            // 策略名为空时设置默认值，避免使用旧策略的字段值
            moduleField.setValue("default");
            functionField.setValue("evaluate");
        } else {
            syncFieldsFromPolicyName(policyName);
        }

        // 加载 CNL 到编辑器
        String cnl = policy.getCnl();
        if (cnl != null && !cnl.isBlank()) {
            // 有 CNL 源码，直接加载
            updateEditorContent(cnl);
            // CNL 中可能包含更准确的模块名，尝试从 CNL 中提取
            updateModuleFieldFromContent(cnl);
        } else {
            // 没有 CNL，使用策略名称生成模板
            String modulePart = moduleField.getValue();
            String funcPart = functionField.getValue();

            // 生成基础模板
            String template = "This module is " + modulePart + ".\n\n" +
                "To " + funcPart + " with input is\n" +
                "  -- TODO: 添加策略规则\n" +
                "  input.";
            updateEditorContent(template);
        }

        // 更新预览区显示
        String displayName = (policyName != null && !policyName.isBlank())
            ? policyName : "(未命名策略)";
        previewResultDiv.setText("策略已加载：" + displayName);
        previewResultDiv.getStyle()
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-color", "var(--lumo-contrast-10pct)");
    }

    /**
     * 从策略名称同步模块和函数字段。
     * 策略名称格式：module.submodule.functionName
     */
    private void syncFieldsFromPolicyName(String policyName) {
        PolicyNameParser.ParseResult result = PolicyNameParser.parse(policyName);
        if (result != null) {
            moduleField.setValue(result.getModuleName());
            functionField.setValue(result.getFunctionName());
        }
    }

    /**
     * 获取当前策略 ID（用于外部检查是否已加载策略）。
     */
    public String getCurrentPolicyId() {
        return currentPolicyId;
    }
}
