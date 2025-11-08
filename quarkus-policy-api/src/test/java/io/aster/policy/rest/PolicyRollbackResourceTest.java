package io.aster.policy.rest;

import io.aster.policy.rest.model.RollbackRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * PolicyEvaluationResource Rollback API 集成测试
 *
 * 测试策略版本回滚 API 的基本功能：
 * - API 端点可访问
 * - 请求参数验证
 * - 响应格式正确
 */
@QuarkusTest
class PolicyRollbackResourceTest {

    @Test
    void testRollbackToNonExistentPolicy() {
        // 回滚不存在的策略应返回错误
        RollbackRequest request = new RollbackRequest(123456L, "测试回滚");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/policies/non-existent-policy/rollback")
            .then()
            .statusCode(200)
            .body("success", is(false))
            .body("message", notNullValue());
    }

    @Test
    void testRollbackRequestValidation() {
        // 测试 targetVersion 为空的情况
        RollbackRequest invalidRequest = new RollbackRequest(null, "测试");

        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
            .when()
            .post("/api/policies/some-policy/rollback")
            .then()
            .statusCode(400); // Bad Request due to validation failure
    }

    @Test
    void testGetVersionHistoryEmptyList() {
        // 获取不存在策略的版本历史应返回空列表
        given()
            .when()
            .get("/api/policies/non-existent-policy/versions")
            .then()
            .statusCode(200)
            .body("size()", is(0));
    }

    @Test
    void testRollbackWithCustomHeaders() {
        // 测试自定义头部被接受
        RollbackRequest request = new RollbackRequest(123456L, "测试");

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-123")
            .header("X-User-Id", "user-456")
            .body(request)
            .when()
            .post("/api/policies/some-policy/rollback")
            .then()
            .statusCode(200)
            .body("success", anyOf(is(true), is(false)))
            .body("message", notNullValue());
    }
}
