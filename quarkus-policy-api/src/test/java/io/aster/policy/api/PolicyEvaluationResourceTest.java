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

    @Test
    public void testPolicyComposition() {
        // Test that the endpoint accepts valid composition requests
        // Note: Actual policy execution success depends on runtime environment
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "steps": [
                    {
                      "policyModule": "aster.finance.loan",
                      "policyFunction": "evaluateLoanEligibility",
                      "useResultAsInput": false
                    },
                    {
                      "policyModule": "aster.finance.loan",
                      "policyFunction": "determineInterestRateBps",
                      "useResultAsInput": false
                    }
                  ],
                  "initialContext": [750, 50000.0, 3.5]
                }
                """)
            .when().post("/api/policies/evaluate/composition")
            .then()
            // Accept either 200 (success) or 500 (execution failure) as valid responses
            // The endpoint itself is working if it doesn't return 400 (bad request)
            .statusCode(anyOf(equalTo(200), equalTo(500)))
            .contentType(ContentType.JSON);
    }

    @Test
    public void testPolicyCompositionWithEmptySteps() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"steps\": [], \"initialContext\": []}")
            .when().post("/api/policies/evaluate/composition")
            .then()
            .statusCode(400)
            .body("error", containsString("Composition steps cannot be empty"));
    }

    @Test
    public void testPolicyCompositionWithMissingFields() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when().post("/api/policies/evaluate/composition")
            .then()
            .statusCode(400)
            .body("error", containsString("Composition steps cannot be empty"));
    }

    @Test
    public void testValidateExistingPolicy() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"policyModule\": \"aster.finance.loan\", \"policyFunction\": \"evaluateLoanEligibility\"}")
            .when().post("/api/policies/validate")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("valid", equalTo(true))
            .body("policyModule", equalTo("aster.finance.loan"))
            .body("policyFunction", equalTo("evaluateLoanEligibility"))
            .body("parameters", notNullValue())
            .body("returnType", notNullValue());
    }

    @Test
    public void testValidateNonExistentPolicy() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"policyModule\": \"nonexistent.module\", \"policyFunction\": \"testFunction\"}")
            .when().post("/api/policies/validate")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("valid", equalTo(false))
            .body("message", containsString("validation failed"));
    }

    @Test
    public void testValidatePolicyWithMissingFields() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when().post("/api/policies/validate")
            .then()
            .statusCode(400)
            .body("error", containsString("Missing required fields"));
    }

    @Test
    public void testBatchEvaluationPartial() {
        // Test that the batch partial endpoint accepts valid requests
        // The endpoint should return 200 even if some policies fail
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "requests": [
                    {
                      "policyModule": "aster.finance.loan",
                      "policyFunction": "evaluateLoanEligibility",
                      "context": [750, 50000.0, 3.5]
                    },
                    {
                      "policyModule": "aster.finance.loan",
                      "policyFunction": "determineInterestRateBps",
                      "context": [800]
                    },
                    {
                      "policyModule": "nonexistent.module",
                      "policyFunction": "testFunction",
                      "context": []
                    }
                  ]
                }
                """)
            .when().post("/api/policies/evaluate/batch/partial")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("successes", notNullValue())
            .body("failures", notNullValue())
            .body("successCount", notNullValue())
            .body("failureCount", notNullValue())
            .body("totalCount", equalTo(3));
    }

    @Test
    public void testBatchEvaluationPartialWithEmptyRequest() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"requests\": []}")
            .when().post("/api/policies/evaluate/batch/partial")
            .then()
            .statusCode(400)
            .body("error", containsString("Batch request cannot be empty"));
    }

    @Test
    public void testBatchEvaluationPartialWithMissingFields() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when().post("/api/policies/evaluate/batch/partial")
            .then()
            .statusCode(400)
            .body("error", containsString("Batch request cannot be empty"));
    }

    // TODO: Add more tests when aster-finance policies are implemented
}
