package editor.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import editor.model.Policy;
import editor.service.PolicyService;
import jakarta.inject.Inject;

/**
 * 策略编辑器主视图
 * <p>
 * 使用 Vaadin 构建的策略管理界面，支持：
 * - 显示策略列表
 * - 添加新策略
 * - 编辑现有策略
 * - 删除策略
 * </p>
 */
@Route("")
public class MainView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final transient PolicyService policyService;
    private final Grid<Policy> grid;
    private transient PolicyEditorDialog editorDialog;

    @Inject
    public MainView(PolicyService policyService) {
        this.policyService = policyService;

        // 标题
        H1 title = new H1("策略编辑器 (Policy Editor)");
        title.getStyle().set("margin-bottom", "20px");

        // 工具栏
        Button addButton = new Button("➕ 添加策略");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openEditorDialog(null));

        Button refreshButton = new Button("🔄 刷新");
        refreshButton.addClickListener(e -> refreshGrid());

        HorizontalLayout toolbar = new HorizontalLayout(addButton, refreshButton);
        toolbar.getStyle().set("margin-bottom", "20px");

        // 策略列表表格
        grid = new Grid<>(Policy.class, false);
        grid.addColumn(Policy::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Policy::getName).setHeader("名称").setAutoWidth(true);
        grid.addColumn(policy -> policy.getAllow().getRules().size())
                .setHeader("Allow 规则")
                .setAutoWidth(true);
        grid.addColumn(policy -> policy.getDeny().getRules().size())
                .setHeader("Deny 规则")
                .setAutoWidth(true);

        grid.addComponentColumn(policy -> {
            Button editButton = new Button("✏️ 编辑");
            editButton.addClickListener(e -> openEditorDialog(policy));

            Button deleteButton = new Button("🗑️ 删除");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deletePolicy(policy));

            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("操作").setAutoWidth(true);

        grid.setWidth("100%");
        grid.setHeight("500px");

        // 布局
        setSpacing(true);
        setPadding(true);
        setWidth("100%");
        add(title, toolbar, grid);

        // 加载数据
        refreshGrid();
    }

    /**
     * 刷新策略列表
     */
    private void refreshGrid() {
        try {
            grid.setItems(policyService.getAllPolicies());
        } catch (Exception e) {
            Notification.show(
                    "❌ 加载失败: " + e.getMessage(),
                    3000,
                    Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * 打开编辑对话框
     *
     * @param policy 要编辑的策略，null 表示创建新策略
     */
    private void openEditorDialog(Policy policy) {
        if (editorDialog == null) {
            editorDialog = new PolicyEditorDialog(policyService);
            editorDialog.addSaveListener(e -> {
                refreshGrid();
                Notification.show(
                        "✅ 保存成功",
                        3000,
                        Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
        }
        editorDialog.open(policy);
    }

    /**
     * 删除策略
     *
     * @param policy 要删除的策略
     */
    private void deletePolicy(Policy policy) {
        // Vaadin 确认对话框
        com.vaadin.flow.component.dialog.Dialog confirmDialog =
                new com.vaadin.flow.component.dialog.Dialog();
        confirmDialog.setHeaderTitle("确认删除");
        confirmDialog.add(String.format("确定要删除策略 %s 吗？", policy.getId()));

        Button confirmButton = new Button("确认", e -> {
            try {
                if (policyService.deletePolicy(policy.getId())) {
                    refreshGrid();
                    Notification.show(
                            "✅ 删除成功",
                            3000,
                            Notification.Position.TOP_CENTER
                    ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show(
                            "❌ 删除失败",
                            3000,
                            Notification.Position.TOP_CENTER
                    ).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception ex) {
                Notification.show(
                        "❌ 错误: " + ex.getMessage(),
                        3000,
                        Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            confirmDialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("取消", e -> confirmDialog.close());

        confirmDialog.getFooter().add(cancelButton, confirmButton);
        confirmDialog.open();
    }
}
