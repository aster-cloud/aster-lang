package editor;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@org.junit.jupiter.api.Disabled("Disable E2E without full dev services; covered by unit/integration tests")
@QuarkusTest
public class PolicyResourceE2ETest {

    @Test
    @TestSecurity(user = "alice", roles = {"admin"})
    void createGetDeletePolicy() {
        String id = "e2e-" + System.currentTimeMillis();
        String body = "{\"id\":\""+id+"\",\"name\":\"demo\",\"allow\":{},\"deny\":{}}";

        // create
        given().contentType("application/json").body(body)
                .when().post("/api/policies")
                .then().statusCode(201).body("id", equalTo(id));

        // get
        given().when().get("/api/policies/"+id)
                .then().statusCode(200).body("name", equalTo("demo"));

        // delete
        given().when().delete("/api/policies/"+id)
                .then().statusCode(anyOf(is(204), is(200)));

        // verify gone
        given().when().get("/api/policies/"+id)
                .then().statusCode(404);
    }
}
