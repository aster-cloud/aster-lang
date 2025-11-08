package editor.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.router.Route;
import editor.graphql.GraphQLClient;
import editor.model.Policy;
import editor.model.EditorSettings;
import editor.service.PolicyService;
import editor.service.SettingsService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * 主视图：围绕 GraphQL Query 的交互表单
 * - 人寿保险报价 generateLifeQuote
 * - 个人贷款评估 evaluatePersonalLoan
 */
@PageTitle("Policy Editor")
@CssImport("./styles/json.css")
@Route("")
@SuppressWarnings({"serial","removal"})
public class MainView extends AppLayout {

    private static final long serialVersionUID = 1L;
    private final PolicyService policyService;
    private final Config config;
    private final SettingsService settingsService;
    private final SecurityIdentity identity;

    private transient GraphQLClient gql;
    private EditorSettings cachedSettings;

    @Inject
    public MainView(PolicyService policyService, Config config, SettingsService settingsService, SecurityIdentity identity) {
        this.policyService = policyService;
        this.config = config;
        this.settingsService = settingsService;
        this.identity = identity;
        // 顶部导航栏（标题 + 抽屉切换）
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("Policy Editor");
        title.getStyle().set("font-size", "1.2rem");
        title.getStyle().set("margin", "0");
        Button themeToggle = new Button("🌓 主题", e -> toggleTheme());
        HorizontalLayout header = new HorizontalLayout(toggle, title, themeToggle, userInfoArea());
        header.setAlignItems(Alignment.CENTER);
        header.setWidthFull();
        header.getStyle().set("padding", "0 1rem");
        addToNavbar(header);

        // 初始化主题（从 localStorage 读取）
        UI.getCurrent().getPage().executeJs("(function(){const t=localStorage.getItem('pe-theme')||'light';document.documentElement.setAttribute('theme',t);})()");

        // 侧边栏 Tabs
        Tab lifeTab = new Tab("人寿保险报价");
        Tab personalLoanTab = new Tab("个人贷款评估");
        Tab asterEditorTab = new Tab("Aster 编辑器");
        Tab policyTab = new Tab("策略管理");
        Tab syncTab = new Tab("同步");
        Tab auditTab = new Tab("审计日志");
        Tab settingsTab = new Tab("设置");
        Tabs tabs = new Tabs(lifeTab, personalLoanTab, asterEditorTab, policyTab, syncTab, auditTab, settingsTab);
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setWidthFull();
        addToDrawer(tabs);

        // 视图内容
        VerticalLayout lifeContent = lifeQuoteForm();
        VerticalLayout loanContent = personalLoanForm();
        AsterPolicyEditorView asterEditorContent = new AsterPolicyEditorView();
        VerticalLayout policyContent = policyManageView();
        VerticalLayout syncContent = syncView();
        VerticalLayout auditContent = auditView();
        VerticalLayout settingsContent = settingsView();

        // 默认内容
        setContent(lifeContent);

        tabs.addSelectedChangeListener(e -> {
            if (e.getSelectedTab() == lifeTab) {
                setContent(lifeContent);
            } else if (e.getSelectedTab() == personalLoanTab) {
                setContent(loanContent);
            } else if (e.getSelectedTab() == asterEditorTab) {
                setContent(asterEditorContent);
            } else if (e.getSelectedTab() == policyTab) {
                setContent(policyContent);
            } else if (e.getSelectedTab() == syncTab) {
                setContent(syncContent);
            } else if (e.getSelectedTab() == auditTab) {
                setContent(auditContent);
            } else {
                setContent(settingsContent);
            }
        });
    }

    private VerticalLayout lifeQuoteForm() {
        VerticalLayout box = new VerticalLayout();
        box.setWidth("800px");

        // Applicant
        TextField applicantId = new TextField("Applicant ID");
        IntegerField age = new IntegerField("Age");
        TextField gender = new TextField("Gender (M/F)");
        TextField smoker = new TextField("Smoker (true/false)");
        IntegerField bmi = new IntegerField("BMI");
        TextField occupation = new TextField("Occupation");
        IntegerField healthScore = new IntegerField("Health Score");

        // Request
        IntegerField coverageAmount = new IntegerField("Coverage Amount");
        IntegerField termYears = new IntegerField("Term Years");
        TextField policyType = new TextField("Policy Type (TERM/...) ");

        FormLayout form = new FormLayout(
                applicantId, age, gender, smoker, bmi, occupation, healthScore,
                coverageAmount, termYears, policyType
        );

        Div result = new Div();
        result.addClassName("json");
        result.getStyle().set("width", "100%");

        Button submit = new Button("提交查询", e -> {
            try {
                String query = """
                        query {
                          generateLifeQuote(
                            applicant: {
                              applicantId: \"%s\"
                              age: %d
                              gender: \"%s\"
                              smoker: %s
                              bmi: %d
                              occupation: \"%s\"
                              healthScore: %d
                            }
                            request: {
                              coverageAmount: %d
                              termYears: %d
                              policyType: \"%s\"
                            }
                          ) {
                            approved
                            reason
                            monthlyPremium
                            coverageAmount
                            termYears
                          }
                        }
                        """.formatted(
                        nullToEmpty(applicantId.getValue()),
                        safeInt(age.getValue()),
                        nullToEmpty(gender.getValue()),
                        parseBoolean(smoker.getValue()),
                        safeInt(bmi.getValue()),
                        nullToEmpty(occupation.getValue()),
                        safeInt(healthScore.getValue()),
                        safeInt(coverageAmount.getValue()),
                        safeInt(termYears.getValue()),
                        nullToEmpty(policyType.getValue())
                );

                JsonNode json = getClient().execute(query);
                result.getElement().setProperty("innerHTML", highlightJson(json.toPrettyString()));
            } catch (Exception ex) {
                Notification.show("❌ 查询失败: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        box.add(new HorizontalLayout(submit), form, result);
        return box;
    }

    private VerticalLayout personalLoanForm() {
        VerticalLayout box = new VerticalLayout();
        box.setWidth("900px");

        // Personal
        TextField applicantId = new TextField("Applicant ID");
        IntegerField age = new IntegerField("Age");
        TextField educationLevel = new TextField("Education Level");
        TextField employmentStatus = new TextField("Employment Status");
        TextField occupation = new TextField("Occupation");
        IntegerField yearsAtJob = new IntegerField("Years At Job");
        IntegerField monthsAtAddress = new IntegerField("Months At Address");
        TextField maritalStatus = new TextField("Marital Status");
        IntegerField dependents = new IntegerField("Dependents");

        // Income
        NumberField monthlyIncome = new NumberField("Monthly Income");
        NumberField additionalIncome = new NumberField("Additional Income");
        NumberField spouseIncome = new NumberField("Spouse Income");
        NumberField rentIncome = new NumberField("Rent Income");
        IntegerField incomeStability = new IntegerField("Income Stability");
        IntegerField incomeGrowthRate = new IntegerField("Income Growth Rate");

        // Credit
        IntegerField creditScore = new IntegerField("Credit Score");
        IntegerField creditHistory = new IntegerField("Credit History (months)");
        IntegerField activeLoans = new IntegerField("Active Loans");
        IntegerField creditCardCount = new IntegerField("Credit Card Count");
        IntegerField creditUtilization = new IntegerField("Credit Utilization %");
        IntegerField latePayments = new IntegerField("Late Payments");
        IntegerField defaults = new IntegerField("Defaults");
        IntegerField bankruptcies = new IntegerField("Bankruptcies");
        IntegerField inquiries = new IntegerField("Inquiries");

        // Debt
        NumberField monthlyMortgage = new NumberField("Monthly Mortgage");
        NumberField monthlyCarPayment = new NumberField("Monthly Car Payment");
        NumberField monthlyStudentLoan = new NumberField("Monthly Student Loan");
        NumberField monthlyCreditCardPayment = new NumberField("Monthly CreditCard Payment");
        NumberField otherMonthlyDebt = new NumberField("Other Monthly Debt");
        NumberField totalOutstandingDebt = new NumberField("Total Outstanding Debt");

        // Request
        NumberField requestedAmount = new NumberField("Requested Amount");
        TextField loanPurpose = new TextField("Loan Purpose");
        IntegerField desiredTermMonths = new IntegerField("Desired Term Months");
        NumberField downPayment = new NumberField("Down Payment");
        NumberField collateralValue = new NumberField("Collateral Value");

        FormLayout form = new FormLayout(
                applicantId, age, educationLevel, employmentStatus, occupation, yearsAtJob, monthsAtAddress, maritalStatus, dependents,
                monthlyIncome, additionalIncome, spouseIncome, rentIncome, incomeStability, incomeGrowthRate,
                creditScore, creditHistory, activeLoans, creditCardCount, creditUtilization, latePayments, defaults, bankruptcies, inquiries,
                monthlyMortgage, monthlyCarPayment, monthlyStudentLoan, monthlyCreditCardPayment, otherMonthlyDebt, totalOutstandingDebt,
                requestedAmount, loanPurpose, desiredTermMonths, downPayment, collateralValue
        );

        Div result = new Div();
        result.addClassName("json");
        result.getStyle().set("width", "100%");

        Button submit = new Button("提交评估", e -> {
            try {
                String query = """
                        query {
                          evaluatePersonalLoan(
                            personal: {
                              applicantId: \"%s\"
                              age: %d
                              educationLevel: \"%s\"
                              employmentStatus: \"%s\"
                              occupation: \"%s\"
                              yearsAtJob: %d
                              monthsAtAddress: %d
                              maritalStatus: \"%s\"
                              dependents: %d
                            }
                            income: {
                              monthlyIncome: %s
                              additionalIncome: %s
                              spouseIncome: %s
                              rentIncome: %s
                              incomeStability: %d
                              incomeGrowthRate: %d
                            }
                            credit: {
                              creditScore: %d
                              creditHistory: %d
                              activeLoans: %d
                              creditCardCount: %d
                              creditUtilization: %d
                              latePayments: %d
                              defaults: %d
                              bankruptcies: %d
                              inquiries: %d
                            }
                            debt: {
                              monthlyMortgage: %s
                              monthlyCarPayment: %s
                              monthlyStudentLoan: %s
                              monthlyCreditCardPayment: %s
                              otherMonthlyDebt: %s
                              totalOutstandingDebt: %s
                            }
                            request: {
                              requestedAmount: %s
                              loanPurpose: \"%s\"
                              desiredTermMonths: %d
                              downPayment: %s
                              collateralValue: %s
                            }
                          ) {
                            approved
                            approvedAmount
                            interestRateBps
                            termMonths
                            monthlyPayment
                            downPaymentRequired
                            conditions
                            riskLevel
                            decisionScore
                            reasonCode
                            recommendations
                          }
                        }
                        """.formatted(
                        nullToEmpty(applicantId.getValue()),
                        safeInt(age.getValue()),
                        nullToEmpty(educationLevel.getValue()),
                        nullToEmpty(employmentStatus.getValue()),
                        nullToEmpty(occupation.getValue()),
                        safeInt(yearsAtJob.getValue()),
                        safeInt(monthsAtAddress.getValue()),
                        nullToEmpty(maritalStatus.getValue()),
                        safeInt(dependents.getValue()),
                        safeNum(monthlyIncome.getValue()), safeNum(additionalIncome.getValue()), safeNum(spouseIncome.getValue()), safeNum(rentIncome.getValue()),
                        safeInt(incomeStability.getValue()), safeInt(incomeGrowthRate.getValue()),
                        safeInt(creditScore.getValue()), safeInt(creditHistory.getValue()), safeInt(activeLoans.getValue()), safeInt(creditCardCount.getValue()), safeInt(creditUtilization.getValue()), safeInt(latePayments.getValue()), safeInt(defaults.getValue()), safeInt(bankruptcies.getValue()), safeInt(inquiries.getValue()),
                        safeNum(monthlyMortgage.getValue()), safeNum(monthlyCarPayment.getValue()), safeNum(monthlyStudentLoan.getValue()), safeNum(monthlyCreditCardPayment.getValue()), safeNum(otherMonthlyDebt.getValue()), safeNum(totalOutstandingDebt.getValue()),
                        safeNum(requestedAmount.getValue()), nullToEmpty(loanPurpose.getValue()), safeInt(desiredTermMonths.getValue()), safeNum(downPayment.getValue()), safeNum(collateralValue.getValue())
                );

                JsonNode json = getClient().execute(query);
                result.getElement().setProperty("innerHTML", highlightJson(json.toPrettyString()));
            } catch (Exception ex) {
                Notification.show("❌ 评估失败: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        box.add(new HorizontalLayout(submit), form, result);
        return box;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static int safeInt(Integer v) { return v == null ? 0 : v; }
    private static String safeNum(Double v) { return v == null ? "0" : (v % 1 == 0 ? String.valueOf(v.intValue()) : v.toString()); }
    private static String parseBoolean(String v) {
        if (v == null) return "false";
        String s = v.trim().toLowerCase();
        return ("true".equals(s) || "1".equals(s) || "yes".equals(s)) ? "true" : "false";
    }

    private GraphQLClient getClient() {
        EditorSettings s = settingsService.load();
        if (gql == null || cachedSettings == null || !cachedSettings.getGraphqlEndpoint().equals(s.getGraphqlEndpoint())
                || cachedSettings.getTimeoutMillis() != s.getTimeoutMillis() || cachedSettings.isCompression() != s.isCompression()
                || cachedSettings.getCacheTtlMillis() != s.getCacheTtlMillis()) {
            gql = new GraphQLClient(s.getGraphqlEndpoint(), s.getTimeoutMillis(), s.isCompression(), s.getCacheTtlMillis());
            cachedSettings = s;
        }
        return gql;
    }

    // 设置页面：编辑 GraphQL 端点与 HTTP 选项
    private VerticalLayout settingsView() {
        VerticalLayout box = new VerticalLayout();
        box.setWidth("700px");
        EditorSettings s = settingsService.load();

        TextField endpoint = new TextField("GraphQL 端点（含协议/路径）");
        endpoint.setWidthFull();
        endpoint.setValue(s.getGraphqlEndpoint());

        IntegerField timeout = new IntegerField("HTTP 超时（毫秒）");
        timeout.setValue(s.getTimeoutMillis());
        timeout.setMin(0);

        Checkbox gzip = new Checkbox("启用压缩（Accept-Encoding: gzip）");
        gzip.setValue(s.isCompression());

        Button save = new Button("保存", e -> {
            try {
                EditorSettings ns = new EditorSettings(endpoint.getValue(), timeout.getValue() == null ? 0 : timeout.getValue(), gzip.getValue());
                settingsService.save(ns);
                cachedSettings = null;
                Notification.show("设置已保存", 1500, Notification.Position.BOTTOM_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("保存失败: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button reset = new Button("重置为默认", e -> {
            try {
                EditorSettings def = new EditorSettings(defaultEndpoint(), 5000, true);
                settingsService.save(def);
                endpoint.setValue(def.getGraphqlEndpoint());
                timeout.setValue(def.getTimeoutMillis());
                gzip.setValue(def.isCompression());
                cachedSettings = null;
                Notification.show("已恢复默认设置", 1500, Notification.Position.BOTTOM_CENTER).addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            } catch (Exception ex) {
                Notification.show("重置失败: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        FormLayout form = new FormLayout(endpoint, timeout, gzip);
        box.add(form, new HorizontalLayout(save, reset));
        return box;
    }

    private String defaultEndpoint() {
        int port = 8081;
        try { port = config.getOptionalValue("quarkus.http.port", Integer.class).orElse(8081);} catch (Exception ignored) {}
        return "http://localhost:" + port + "/graphql";
    }

    /**
     * 策略管理视图：列表 + 新增/编辑/删除
     */
    private VerticalLayout policyManageView() {
        VerticalLayout box = new VerticalLayout();
        box.setWidth("1000px");

        Grid<Policy> grid = new Grid<>(Policy.class, false);
        grid.addColumn(Policy::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Policy::getName).setHeader("名称").setAutoWidth(true).setFlexGrow(2);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        TextField filter = new TextField();
        filter.setPlaceholder("搜索名称或ID...");
        filter.addValueChangeListener(e -> grid.setItems(policyService.getAllPolicies().stream()
                .filter(p -> p.getName().toLowerCase().contains(e.getValue().toLowerCase())
                        || p.getId().toLowerCase().contains(e.getValue().toLowerCase()))
                .toList()));

        Button refresh = new Button("刷新", e -> grid.setItems(policyService.getAllPolicies()));
        Button add = new Button("新增策略", e -> {
            PolicyEditorDialog dialog = new PolicyEditorDialog(policyService);
            dialog.addSaveListener(ev -> grid.setItems(policyService.getAllPolicies()));
            dialog.open(null);
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button edit = new Button("编辑选中", e -> {
            Policy selected = grid.asSingleSelect().getValue();
            if (selected == null) {
                Notification.show("请先选择一条策略", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }
            PolicyEditorDialog dialog = new PolicyEditorDialog(policyService);
            dialog.addSaveListener(ev -> grid.setItems(policyService.getAllPolicies()));
            dialog.open(selected);
        });

        Button historyBtn = new Button("历史", e -> {
            Policy selected = grid.asSingleSelect().getValue();
            if (selected == null) {
                Notification.show("请先选择一条策略", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }
            new HistoryDialog(selected.getId()).open();
        });

        Button undoBtn = new Button("撤销", e -> {
            Policy selected = grid.asSingleSelect().getValue();
            if (selected == null) { Notification.show("请选择策略", 1500, Notification.Position.TOP_CENTER); return; }
            callSimplePost("/api/policies/"+selected.getId()+"/undo");
            grid.setItems(policyService.getAllPolicies());
        });

        Button redoBtn = new Button("重做", e -> {
            Policy selected = grid.asSingleSelect().getValue();
            if (selected == null) { Notification.show("请选择策略", 1500, Notification.Position.TOP_CENTER); return; }
            callSimplePost("/api/policies/"+selected.getId()+"/redo");
            grid.setItems(policyService.getAllPolicies());
        });

        Button duplicate = new Button("复制为新策略", e -> {
            Policy selected = grid.asSingleSelect().getValue();
            if (selected == null) {
                Notification.show("请先选择一条策略", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }
            Policy copy = new Policy(null, selected.getName() + " (copy)", selected.getAllow(), selected.getDeny());
            PolicyEditorDialog dialog = new PolicyEditorDialog(policyService);
            dialog.addSaveListener(ev -> grid.setItems(policyService.getAllPolicies()));
            dialog.open(copy);
        });

        Button exportZip = new Button("导出ZIP", e -> UI.getCurrent().getPage().open("/api/policies/export"));

        com.vaadin.flow.component.upload.Upload upload = new com.vaadin.flow.component.upload.Upload();
        com.vaadin.flow.component.upload.receivers.MemoryBuffer buffer = new com.vaadin.flow.component.upload.receivers.MemoryBuffer();
        upload.setReceiver(buffer);
        upload.setMaxFiles(1);
        upload.addSucceededListener(ev -> {
            try {
                byte[] bytes = buffer.getInputStream().readAllBytes();
                String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
                // 调用本地 REST 导入
                java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
                String json = String.format("{\"base64\":\"%s\"}", b64);
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create("/api/policies/importZip"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                        .build();
                http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                Notification.show("导入完成", 1500, Notification.Position.BOTTOM_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                grid.setItems(policyService.getAllPolicies());
            } catch (Exception ex) {
                Notification.show("导入失败: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button del = new Button("删除选中", e -> {
            Policy selected = grid.asSingleSelect().getValue();
            if (selected == null) {
                Notification.show("请先选择一条策略", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }
            boolean ok = policyService.deletePolicy(selected.getId());
            if (ok) {
                Notification.show("已删除", 1500, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                grid.setItems(policyService.getAllPolicies());
            } else {
                Notification.show("删除失败", 2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        del.addThemeVariants(ButtonVariant.LUMO_ERROR);
        boolean admin = hasAdmin();
        del.setVisible(admin);
        undoBtn.setVisible(admin);
        redoBtn.setVisible(admin);
        upload.setVisible(admin);

        grid.setItems(policyService.getAllPolicies());
        grid.setHeight("420px");

        HorizontalLayout actions = new HorizontalLayout(filter, refresh, add, edit, historyBtn, undoBtn, redoBtn, duplicate, exportZip, upload, del);
        actions.setSpacing(true);
        box.add(actions, grid);
        return box;
    }

    private void callSimplePost(String path) {
        try {
            java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(path)).POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build();
            http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {}
    }

    // 同步页面：一键拉取/推送（使用设置中的 remoteRepoDir）
    private VerticalLayout syncView() {
        VerticalLayout box = new VerticalLayout();
        box.setWidth("700px");
        var s = settingsService.load();
        TextField remote = new TextField("远端目录");
        remote.setWidthFull();
        remote.setValue(s.getRemoteRepoDir() == null ? "data/remote-policies" : s.getRemoteRepoDir());
        Button pull = new Button("拉取", e -> {
            var resp = postTextWithResponse("/api/policies/sync/pull", remote.getValue());
            if (resp != null && resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String detail = parseSyncDetail(resp.body());
                Notification.show("✅ 拉取成功 " + detail, 2500, Notification.Position.BOTTOM_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                String msg = resp == null ? "网络错误" : ("HTTP " + resp.statusCode() + ": " + truncate(resp.body()));
                Notification.show("❌ 拉取失败: " + msg, 4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        Button push = new Button("推送", e -> {
            var resp = postTextWithResponse("/api/policies/sync/push", remote.getValue());
            if (resp != null && resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String detail = parseSyncDetail(resp.body());
                Notification.show("✅ 推送成功 " + detail, 2500, Notification.Position.BOTTOM_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                String msg = resp == null ? "网络错误" : ("HTTP " + resp.statusCode() + ": " + truncate(resp.body()));
                Notification.show("❌ 推送失败: " + msg, 4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        push.setVisible(hasAdmin());
        box.add(remote, new HorizontalLayout(pull, push));
        return box;
    }

    private java.net.http.HttpResponse<String> postTextWithResponse(String path, String text) {
        try {
            var http = java.net.http.HttpClient.newHttpClient();
            var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(path))
                    .header("Content-Type", "text/plain").POST(java.net.http.HttpRequest.BodyPublishers.ofString(text)).build();
            return http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) { return null; }
    }

    private String truncate(String s) { if (s == null) return ""; return s.length() > 160 ? s.substring(0,160) + "..." : s; }

    private String parseSyncDetail(String body) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(body);
            if (node.has("created") || node.has("updated") || node.has("skipped")) {
                return String.format("(新建:%d 更新:%d 跳过:%d)",
                        node.path("created").asInt(), node.path("updated").asInt(), node.path("skipped").asInt());
            }
            return "";
        } catch (Exception e) { return ""; }
    }

    // 审计日志页面
    private VerticalLayout auditView() {
        VerticalLayout box = new VerticalLayout();
        box.setWidth("900px");
        Grid<AuditEntry> ag = new Grid<>(AuditEntry.class, false);
        ag.addColumn(a -> a.timestamp).setHeader("时间").setAutoWidth(true);
        ag.addColumn(a -> a.actor).setHeader("用户").setAutoWidth(true);
        ag.addColumn(a -> a.action).setHeader("动作").setAutoWidth(true);
        ag.addColumn(a -> a.target).setHeader("目标").setAutoWidth(true);
        ag.addColumn(a -> a.details).setHeader("详情").setFlexGrow(1);
        ag.setHeight("420px");

        com.vaadin.flow.component.textfield.TextField query = new com.vaadin.flow.component.textfield.TextField();
        query.setPlaceholder("搜索 actor/action/target/关键词...");
        com.vaadin.flow.component.textfield.IntegerField page = new com.vaadin.flow.component.textfield.IntegerField("页"); page.setValue(0);
        com.vaadin.flow.component.textfield.IntegerField size = new com.vaadin.flow.component.textfield.IntegerField("每页"); size.setValue(100);

        Button refresh = new Button("刷新", e -> ag.setItems(fetchAuditPage(query.getValue(), page.getValue(), size.getValue())));
        Button clear = new Button("清空", e -> { callSimplePost("/api/policies/audit/clear"); ag.setItems(java.util.List.of());});
        Button export = new Button("导出", e -> UI.getCurrent().getPage().open("/api/policies/audit/export?q=" + urlEncode(query.getValue())));
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        box.add(new HorizontalLayout(query, page, size, refresh, export, clear), ag);
        ag.setItems(fetchAuditPage(query.getValue(), page.getValue(), size.getValue()));
        return box;
    }

    private String fetchAudit() {
        try {
            var http = java.net.http.HttpClient.newHttpClient();
            var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create("/api/policies/audit")).GET().build();
            var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String body = resp.body();
                // 将 ["line1","line2"] 转换成文本
                return body.replace("[", "").replace("]", "").replace("\\\"", "").replace(",", "\n");
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String filterAudit(String text, String q) {
        if (q == null || q.isBlank()) return text;
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            if (line.toLowerCase().contains(q.toLowerCase())) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private java.util.List<AuditEntry> fetchAuditPage(String q, Integer page, Integer size) {
        try {
            String url = "/api/policies/audit?page=" + (page==null?0:page) + "&size=" + (size==null?100:size) + (q==null||q.isBlank()?"":"&q="+urlEncode(q));
            var http = java.net.http.HttpClient.newHttpClient();
            var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();
            var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var arr = mapper.readTree(resp.body());
                java.util.List<AuditEntry> out = new java.util.ArrayList<>();
                if (arr.isArray()) for (var n : arr) {
                    AuditEntry a = new AuditEntry();
                    a.timestamp = n.path("timestamp").asText();
                    a.actor = n.path("actor").asText();
                    a.action = n.path("action").asText();
                    a.target = n.path("target").asText();
                    a.details = n.path("details").asText();
                    out.add(a);
                }
                return out;
            }
        } catch (Exception ignored) {}
        return java.util.List.of();
    }

    private String urlEncode(String s) { try { return java.net.URLEncoder.encode(s==null?"":s, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception e) { return ""; } }

    public static class AuditEntry { public String timestamp; public String actor; public String action; public String target; public String details; }
    private boolean hasAdmin() { try { return identity != null && identity.getRoles().contains("admin"); } catch (Throwable t) { return false; } }

    private HorizontalLayout userInfoArea() {
        String user = (identity != null && !identity.isAnonymous() && identity.getPrincipal()!=null) ? identity.getPrincipal().getName() : "anonymous";
        String roles = (identity != null) ? String.join(", ", identity.getRoles()) : "";
        com.vaadin.flow.component.html.Span info = new com.vaadin.flow.component.html.Span("👤 "+user+ (roles.isBlank()?"":" ["+roles+"]"));
        Anchor login = new Anchor("/q/oidc/login", "登录");
        login.getElement().setAttribute("router-ignore", "");
        Anchor logout = new Anchor("/q/oidc/logout", "登出");
        logout.getElement().setAttribute("router-ignore", "");
        HorizontalLayout box = new HorizontalLayout(info, login, logout);
        box.setSpacing(true);
        return box;
    }

    // 主题切换：在 documentElement 上设置 theme=light/dark 并持久化到 localStorage
    private void toggleTheme() {
        UI.getCurrent().getPage().executeJs(
            "(function(){const de=document.documentElement;const cur=de.getAttribute('theme')==='dark'?'dark':'light';const next=cur==='dark'?'light':'dark';de.setAttribute('theme',next);localStorage.setItem('pe-theme',next);})()"
        );
    }

    // 简化版 JSON 高亮：仅转义后包裹 <pre>，避免复杂正则在不同 JDK 下转义问题
    private static String highlightJson(String json) {
        String esc = json == null ? "" : json
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return "<pre class=\"json\">" + esc + "</pre>";
    }
}
