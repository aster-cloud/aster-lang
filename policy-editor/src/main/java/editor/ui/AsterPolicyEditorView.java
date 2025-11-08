package editor.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Aster 策略编辑器（带 Live Preview）
 *
 * 功能：
 * - 编辑 Aster 策略代码
 * - 配置示例输入
 * - 实时预览评估结果（通过 WebSocket）
 * - 300ms 防抖
 * - 自动重连
 */
@PageTitle("Aster Policy Editor")
@Route("aster-editor")
public class AsterPolicyEditorView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final int DEBOUNCE_MS = 300;

    private final TextField moduleField;
    private final TextField functionField;
    private final TextArea policyCodeArea;
    private final TextArea sampleInputArea;
    private final Div previewResultDiv;
    private final Div connectionStatusDiv;

    private String webSocketUrl;

    public AsterPolicyEditorView() {
        setSizeFull();
        setPadding(true);

        // 标题
        H3 title = new H3("Aster Policy Editor - Live Preview");

        // 模块和函数名
        moduleField = new TextField("Policy Module");
        moduleField.setValue("aster.finance.loan");
        moduleField.setWidth("300px");

        functionField = new TextField("Policy Function");
        functionField.setValue("evaluateLoanEligibility");
        functionField.setWidth("300px");

        HorizontalLayout headerLayout = new HorizontalLayout(moduleField, functionField);
        headerLayout.setAlignItems(Alignment.END);

        // 策略代码编辑区
        policyCodeArea = new TextArea("Policy Code (.aster)");
        policyCodeArea.setPlaceholder("This module is aster.finance.loan.\n\nTo evaluateLoanEligibility with ...");
        policyCodeArea.setWidth("100%");
        policyCodeArea.setHeight("400px");
        policyCodeArea.getStyle().set("font-family", "monospace");

        // 示例输入配置
        sampleInputArea = new TextArea("Sample Input (JSON Array)");
        sampleInputArea.setValue("[\n  {\"creditScore\": 750, \"income\": 100000, \"loanAmount\": 300000}\n]");
        sampleInputArea.setWidth("100%");
        sampleInputArea.setHeight("150px");
        sampleInputArea.getStyle().set("font-family", "monospace");

        // 左侧编辑器
        VerticalLayout editorLayout = new VerticalLayout(policyCodeArea, sampleInputArea);
        editorLayout.setSizeFull();
        editorLayout.setPadding(false);

        // 连接状态
        connectionStatusDiv = new Div();
        connectionStatusDiv.setText("🔴 未连接");
        connectionStatusDiv.getStyle()
            .set("padding", "8px 12px")
            .set("background", "#fee")
            .set("border-radius", "4px")
            .set("margin-bottom", "8px");

        // 预览结果区
        previewResultDiv = new Div();
        previewResultDiv.getStyle()
            .set("padding", "16px")
            .set("background", "#f5f5f5")
            .set("border", "1px solid #ddd")
            .set("border-radius", "4px")
            .set("overflow-y", "auto")
            .set("height", "100%")
            .set("font-family", "monospace")
            .set("white-space", "pre-wrap");

        previewResultDiv.setText("等待编辑以触发预览...");

        // 右侧预览区
        VerticalLayout previewLayout = new VerticalLayout();
        previewLayout.add(new H3("Live Preview"), connectionStatusDiv, previewResultDiv);
        previewLayout.setSizeFull();
        previewLayout.setPadding(false);

        // 分割布局
        SplitLayout splitLayout = new SplitLayout(editorLayout, previewLayout);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(60);

        // 按钮
        Button saveButton = new Button("保存策略", e -> savePolicy());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button exportButton = new Button("导出 CNL", e -> exportPolicy());
        exportButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button importButton = new Button("导入 CNL", e -> triggerFileImport());

        Button resetButton = new Button("重置", e -> resetEditor());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, exportButton, importButton, resetButton);

        add(title, headerLayout, splitLayout, buttonLayout);
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
        // 监听代码变化，触发防抖预览
        String debounceScript = """
            (function() {
                let debounceTimer = null;

                $0.addEventListener('value-changed', function() {
                    if (debounceTimer) {
                        clearTimeout(debounceTimer);
                    }

                    debounceTimer = setTimeout(function() {
                        $0.$server.triggerPreview();
                    }, %d);
                });
            })();
            """.formatted(DEBOUNCE_MS);

        ui.getPage().executeJs(debounceScript, policyCodeArea.getElement());
        ui.getPage().executeJs(debounceScript, sampleInputArea.getElement());
    }

    /**
     * 更新连接状态（从 JavaScript 回调）
     */
    @ClientCallable
    public void updateConnectionStatus(String status) {
        getUI().ifPresent(ui -> ui.access(() -> {
            switch (status) {
                case "connected":
                    connectionStatusDiv.setText("🟢 已连接");
                    connectionStatusDiv.getStyle().set("background", "#dfd");
                    break;
                case "disconnected":
                    connectionStatusDiv.setText("🔴 已断开（正在重连...）");
                    connectionStatusDiv.getStyle().set("background", "#fee");
                    break;
                case "error":
                    connectionStatusDiv.setText("⚠️ 连接错误");
                    connectionStatusDiv.getStyle().set("background", "#ffe");
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

                    previewResultDiv.getStyle().set("background", "#e8f5e9");
                    previewResultDiv.setText(String.format("✅ 评估成功 (%dms)\n\n%s",
                        executionTime, resultJson));
                } else if ("error".equals(status)) {
                    previewResultDiv.getStyle().set("background", "#ffebee");
                    previewResultDiv.setText(String.format("❌ 评估失败 (%dms)\n\n%s",
                        executionTime, message));
                } else {
                    previewResultDiv.getStyle().set("background", "#f5f5f5");
                    previewResultDiv.setText(message);
                }

            } catch (Exception e) {
                previewResultDiv.getStyle().set("background", "#ffebee");
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
        String code = policyCodeArea.getValue();

        if (code == null || code.isBlank()) {
            Notification.show("策略代码不能为空", 2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // TODO: 实现保存逻辑（保存到文件系统或数据库）
        Notification.show("策略保存功能待实现", 2000, Notification.Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }

    /**
     * 导出策略为 .aster 文件
     */
    private void exportPolicy() {
        String code = policyCodeArea.getValue();

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
                policyCodeArea.setValue(content);

                // 尝试从文件内容中提取 module 信息
                // 格式: "This module is aster.finance.loan."
                String firstLine = content.lines().findFirst().orElse("");
                if (firstLine.startsWith("This module is ") && firstLine.endsWith(".")) {
                    String moduleName = firstLine.substring("This module is ".length(), firstLine.length() - 1);
                    moduleField.setValue(moduleName);
                }

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
        policyCodeArea.clear();
        sampleInputArea.setValue("[\n  {}\n]");
        previewResultDiv.setText("等待编辑以触发预览...");
        previewResultDiv.getStyle().set("background", "#f5f5f5");
    }
}
