package editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import editor.service.PolicyValidationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PolicyValidationServiceTest {

    @Inject
    PolicyValidationService validationService;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void validPolicyPassesSchema() throws Exception {
        String json = "{\n" +
                "  \"id\": \"p1\",\n" +
                "  \"name\": \"demo\",\n" +
                "  \"allow\": { \"io\": [\"*\"] },\n" +
                "  \"deny\": {}\n" +
                "}";
        JsonNode node = objectMapper.readTree(json);
        Set<ValidationMessage> errors = validationService.validate(node);
        assertTrue(errors.isEmpty(), () -> "Unexpected errors: " + errors);
    }

    @Test
    void missingRequiredFieldsFail() throws Exception {
        String json = "{ \"id\": \"p1\" }";
        JsonNode node = objectMapper.readTree(json);
        Set<ValidationMessage> errors = validationService.validate(node);
        assertFalse(errors.isEmpty());
    }
}

