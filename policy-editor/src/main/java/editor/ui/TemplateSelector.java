package editor.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import editor.template.PolicyTemplate;
import editor.template.PolicyTemplateService;

import java.io.Serial;
import java.util.function.Consumer;

/**
 * 策略模板选择器：展示模板列表、简介与示例片段。
 */
public class TemplateSelector extends Composite<VerticalLayout> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ComboBox<PolicyTemplate> templateField;
    private final Paragraph metaLabel;
    private final Pre previewArea;
    private final Button applyButton;
    private transient Consumer<PolicyTemplate> applyListener;

    public TemplateSelector(PolicyTemplateService service) {
        VerticalLayout layout = getContent();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.addClassName("template-selector");

        templateField = new ComboBox<>("策略模板");
        templateField.setItems(service.getTemplates());
        templateField.setItemLabelGenerator(template -> template.name() + " · " + template.category());
        templateField.setWidthFull();
        templateField.addValueChangeListener(event -> updatePreview(event.getValue()));

        metaLabel = new Paragraph("选择模板以查看描述");
        metaLabel.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("margin", "0")
            .set("color", "var(--lumo-secondary-text-color)");

        previewArea = new Pre();
        previewArea.getStyle()
            .set("background", "#f7f7f9")
            .set("border", "1px dashed #d5d5d5")
            .set("border-radius", "4px")
            .set("max-height", "220px")
            .set("overflow", "auto")
            .set("font-family", "monospace")
            .set("font-size", "var(--lumo-font-size-s)")
            .set("margin-top", "6px")
            .set("padding", "10px");
        previewArea.setText("选择模板后将在此展示示例代码");

        applyButton = new Button("应用模板", event -> applySelected());

        HorizontalLayout actionRow = new HorizontalLayout(applyButton);
        actionRow.setWidthFull();
        actionRow.setPadding(false);
        actionRow.setSpacing(false);
        actionRow.setJustifyContentMode(JustifyContentMode.START);

        layout.add(templateField, metaLabel, previewArea, actionRow);
    }

    public void setTemplateApplyListener(Consumer<PolicyTemplate> listener) {
        this.applyListener = listener;
    }

    private void applySelected() {
        PolicyTemplate selected = templateField.getValue();
        if (selected == null) {
            Notification.show("请先选择模板", 2000, Notification.Position.TOP_CENTER);
            return;
        }
        if (applyListener != null) {
            applyListener.accept(selected);
        }
    }

    private void updatePreview(PolicyTemplate template) {
        if (template == null) {
            metaLabel.setText("选择模板以查看描述");
            previewArea.setText("选择模板后将在此展示示例代码");
            return;
        }
        metaLabel.setText(template.description() + " · 分类: " + template.category());
        previewArea.setText(template.content());
    }
}
