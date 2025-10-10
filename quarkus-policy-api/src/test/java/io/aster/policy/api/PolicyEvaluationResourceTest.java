package io.aster.policy.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

/**
 * 策略评估API测试
 */
@QuarkusTest
public class PolicyEvaluationResourceTest {

    @Test
    public void testListPoliciesEndpoint() {
        given()
            .when().get("/api/policies/list")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("policies", notNullValue())
            .body("count", greaterThan(0));
    }

    @Test
    public void testHealthEndpoint() {
        given()
            .when().get("/api/policies/health")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", equalTo("UP"))
            .body("service", equalTo("aster-policy-api"));
    }

    @Test
    public void testEvaluatePolicyWithMissingFields() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when().post("/api/policies/evaluate")
            .then()
            .statusCode(400)
            .body("error", containsString("Missing required fields"));
    }

    @Test
    public void testEvaluatePolicyNotFound() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"policyModule\": \"nonexistent\", \"policyFunction\": \"test\", \"context\": []}")
            .when().post("/api/policies/evaluate")
            .then()
            .statusCode(404)
            .body("error", containsString("Policy not found"));
    }

    // TODO: Add more tests when aster-finance policies are implemented
}
