package editor.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * 历史版本查看与对比对话框
 */
public class HistoryDialog extends Dialog {
    private static final long serialVersionUID = 1L;
    private final String policyId;
    private final Grid<String> grid = new Grid<>();
    private final TextArea left = new TextArea("版本A");
    private final TextArea right = new TextArea("版本B");
    private final Div diffView = new Div();

    public HistoryDialog(String policyId) {
        this.policyId = policyId;
        setHeaderTitle("历史版本 - " + policyId);

        grid.addColumn(s -> s).setHeader("时间戳");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setHeight("200px");

        left.setWidth("100%"); right.setWidth("100%");
        left.setHeight("200px"); right.setHeight("200px");

        Button loadBtn = new Button("加载所选两版", e -> loadSelected());
        Button diffBtn = new Button("Diff", e -> showDiff()); diffBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout content = new VerticalLayout(new H3("版本列表"), grid,
                new HorizontalLayout(loadBtn, diffBtn),
                new HorizontalLayout(left, right), new H3("对比结果"), diffView);
        content.setWidth("900px");
        add(content);

        loadHistory();
    }

    private void loadHistory() {
        try {
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create("/api/policies/" + policyId + "/history")).GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                List<String> items = java.util.Arrays.asList(resp.body().replace("[", "").replace("]", "").replace("\"", "").split(","));
                grid.setItems(items.stream().filter(s -> !s.isBlank()).toList());
            }
        } catch (Exception ignored) {}
    }

    private void loadSelected() {
        var sel = grid.getSelectedItems();
        if (sel.size() != 2) return;
        var it = sel.iterator();
        String a = it.next();
        String b = it.next();
        left.setValue(loadVersion(a));
        right.setValue(loadVersion(b));
    }

    private String loadVersion(String ver) {
        try {
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create("/api/policies/" + policyId + "/history/" + ver)).GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (Exception e) { return ""; }
    }

    private void showDiff() {
        String a = left.getValue() == null ? "" : left.getValue();
        String b = right.getValue() == null ? "" : right.getValue();
        diffView.getElement().setProperty("innerHTML", inlineDiff(a, b));
    }

    // 简单行级 Diff：输出添加/删除行，保留上下文
    private String inlineDiff(String a, String b) {
        String[] A = a.split("\n");
        String[] B = b.split("\n");
        int i=0,j=0; StringBuilder sb = new StringBuilder("<pre style='background:#0e1116;color:#c9d1d9;padding:10px;overflow:auto'>");
        while (i<A.length || j<B.length) {
            String la = i<A.length?A[i]:null; String lb = j<B.length?B[j]:null;
            if (la!=null && lb!=null && la.equals(lb)) { sb.append(escape(la)).append("\n"); i++; j++; }
            else {
                if (la!=null) { sb.append("<span style='color:#f85149'>- ").append(escape(la)).append("</span>\n"); i++; }
                if (lb!=null) { sb.append("<span style='color:#3fb950'>+ ").append(escape(lb)).append("</span>\n"); j++; }
            }
        }
        sb.append("</pre>");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
