package editor;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@org.junit.jupiter.api.Disabled("Disable OIDC redirect test in local without dev services; covered by security tests")
@QuarkusTest
public class OidcFlowRedirectTest {

    @Test
    void unauthenticatedProtectedApiShouldBe401OrRedirect() {
        // 由于本服务主要是 REST API，默认返回 401；如将受保护资源换成页面，可能返回 302 重定向到 /q/oidc/login
        given()
            .when().post("/api/policies/importZip")
            .then().statusCode(anyOf(is(401), is(403), is(302)));
    }
}
