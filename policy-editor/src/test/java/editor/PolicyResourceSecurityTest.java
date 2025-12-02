package editor;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@org.junit.jupiter.api.Disabled("Disabled in local env without full Quarkus dev services; enable when OIDC/HTTP stack is up")
@QuarkusTest
public class PolicyResourceSecurityTest {

    @Test
    void permitAll_canListPolicies_withoutAuth() {
        given()
            .when().get("/api/policies")
            .then().statusCode(anyOf(is(200), is(204)));
    }

    @Test
    @TestSecurity(user = "bob", roles = {"user"})
    void nonAdmin_cannotImportZip() {
        given().contentType("application/json").body("{\"base64\":\"\"}")
            .when().post("/api/policies/importZip")
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "alice", roles = {"admin"})
    void admin_canImportZip() {
        given().contentType("application/json").body("{\"base64\":\"\"}")
            .when().post("/api/policies/importZip")
            .then().statusCode(anyOf(is(200), is(400))); // 空内容可能400，验证授权通过
    }
}
