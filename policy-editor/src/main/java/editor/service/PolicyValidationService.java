package editor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.Set;

/**
 * 使用 JSON Schema 校验策略结构与必填项。
 */
@ApplicationScoped
public class PolicyValidationService {

    @Inject
    ObjectMapper objectMapper;

    private volatile JsonSchema schema;

    private JsonSchema getSchema() {
        if (schema == null) {
            synchronized (this) {
                if (schema == null) {
                    try (InputStream in = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("policy-schema.json")) {
                        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
                        schema = factory.getSchema(in);
                    } catch (Exception e) {
                        throw new RuntimeException("加载 policy-schema.json 失败", e);
                    }
                }
            }
        }
        return schema;
    }

    public Set<ValidationMessage> validate(JsonNode node) {
        return getSchema().validate(node);
    }
}

