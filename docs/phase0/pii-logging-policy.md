# PII 敏感信息日志策略

> 更新时间：2025-11-15 21:29 NZST · 执行者：Codex

## 支持的 PII 类型与正则

| 类型 | 正则（摘自 `PIIRedactor.java`） | 占位符 |
| --- | --- | --- |
| SSN | `\\b\\d{3}-?\\d{2}-?\\d{4}\\b` | `***-**-****` |
| Email | `\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b` | `***@***.***` |
| Phone | `\\(\\d{3}\\)\\s*\\d{3}[-.]?\\d{4}|\\b\\d{3}[-.]\\d{3}[-.]\\d{4}\\b|\\b\\d{10}\\b` | `(***) ***-****` |
| CreditCard | `\\b(?:\\d{4}[-\\s]?){3}\\d{4}(?:\\d{3})?\\b` | `****-****-****-****` |
| IP | `\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b` | `***.***.***.***` |

`PIIRedactor.redact()` 会按上述顺序执行 `replaceAll`，因此最终输出永远不会包含原始 PII。

## 集成点

1. **全局应用日志**：`AuditLogger.toJson()` 遍历所有字符串字段并调用 `PIIRedactor.redact()`，确保 INFO 日志中不泄漏敏感信息。
2. **审计事件持久化**：`AuditEventListener` 在写入 `AuditLog` 时对 `tenantId`、`policyModule`、`reason`、`metadata` 等字段进行脱敏（包括异步事件 metadata）。
3. **自定义 Logger**：业务代码可直接持有 `PIIRedactor` 或其 `Builder`（可配置性更高）实现额外日志线路的脱敏。

## 使用示例

```java
// ✅ 正确示例：在日志写入前调用 PIIRedactor
import com.wontlost.aster.policy.PIIRedactor;
import org.jboss.logging.Logger;

public class LoginLogger {
    private static final Logger LOG = Logger.getLogger(LoginLogger.class);
    private static final PIIRedactor REDACTOR = new PIIRedactor();

    public void logLogin(String email, String ssn, String ip) {
        String raw = "login email=%s ssn=%s ip=%s".formatted(email, ssn, ip);
        LOG.info(REDACTOR.redact(raw)); // 输出中只有掩码
    }
}

// ❌ 错误示例：直接输出原始 PII，违反日志策略与合规要求
public class LoginLoggerLegacy {
    private static final Logger LOG = Logger.getLogger(LoginLoggerLegacy.class);

    public void logLogin(String email, String ssn, String ip) {
        LOG.infof("login email=%s ssn=%s ip=%s", email, ssn, ip);
        // 该日志将被合规扫描立即阻断，并触发 ArchUnit 附加检查
    }
}
```

## 扩展方法

1. **添加正则**：在 `PIIRedactor` 中新增 `Pattern` 常量（例如护照号），并在 `redact()` / `containsPII()` 中插入相同顺序的替换逻辑。
2. **Builder 支持**：更新 `PIIRedactor.Builder` 增加新的布尔开关，使调用方可以开启/关闭该类型。
3. **集成验证**：为新类型添加至少 1 条 `PIIRedactionIntegrationTest` 用例，并根据需要扩展 `AuditLogger` / `AuditEventListener` 的字段脱敏。
4. **渐进发布**：如需暂时禁用，可通过 `new PIIRedactor(false, true, true, true, true)` 局部关闭部分类型。

## 合规性

- **GDPR**：所有日志输出都不可包含 “可识别个人信息”，PIIRedactor 确保默认掩码，若需要删除数据可直接删除日志行而无需额外清洗。
- **CCPA**：支持在导出审计日志前通过 `AuditChainVerifier` 验证链路，再将掩码后的数据提供给合规团队，减少个人信息暴露。
- **审计留痕**：AuditLogger/AuditEventListener 将掩码后的值写入数据库，数据库备份中不会出现原始 PII，降低数据脱敏成本。

## 测试验证

1. 运行 `SKIP_GENERATE_ASTER_JAR=true ./gradlew :quarkus-policy-api:test --tests PIIRedactionIntegrationTest`，确保 6 个用例（SSN、Email、Phone、CreditCard、IP、组合）全部通过。
2. 若新增 PII 类型，需增加对应测试并确认 `containsPII()` 能立即识别原始值。
3. 在本地调试日志输出时，可使用 `piiRedactor.containsPII(raw)` 快速检测输入是否含敏感内容，避免未脱敏字符串被输出。
