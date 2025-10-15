package io.aster.policy.graphql;

import io.aster.policy.api.PolicyCacheKey;
import io.aster.policy.api.PolicyEvaluationService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * GraphQL API 测试 - 策略评估查询和变更
 *
 * 测试覆盖:
 * 1. Query 查询测试 - 各类策略评估查询
 * 2. Mutation 变更测试 - 缓存管理操作
 * 3. 错误处理测试 - 无效输入和边界情况
 */
@QuarkusTest
public class PolicyGraphQLResourceTest {

    @Inject
    PolicyEvaluationService policyEvaluationService;

    @BeforeEach
    public void resetCache() {
        policyEvaluationService.clearAllCache().await().indefinitely();
    }

    // Helper method to create GraphQL request body
    private Map<String, String> graphQLRequest(String query) {
        return Map.of("query", query);
    }

    // ==================== Query Tests - 贷款评估 ====================

    @Test
    public void testEvaluateLoanEligibility_Approved() {
        String query = """
            query {
              evaluateLoanEligibility(
                application: {
                  loanId: "LOAN-001"
                  applicantId: "APP-001"
                  amountRequested: 50000
                  purposeCode: "HOME"
                  termMonths: 360
                }
                applicant: {
                  applicantId: "APP-001"
                  age: 35
                  annualIncome: 120000
                  creditScore: 750
                  existingDebtMonthly: 1500
                  yearsEmployed: 10
                }
              ) {
                approved
                reason
                maxApprovedAmount
                interestRateBps
                termMonths
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateLoanEligibility.approved", equalTo(true))
            .body("data.evaluateLoanEligibility.maxApprovedAmount", greaterThan(0))
            .body("data.evaluateLoanEligibility.interestRateBps", greaterThan(0))
            .body("data.evaluateLoanEligibility.termMonths", equalTo(360));
    }

    @Test
    public void testEvaluateLoanEligibility_LowCreditScore() {
        String query = """
            query {
              evaluateLoanEligibility(
                application: {
                  loanId: "LOAN-002"
                  applicantId: "APP-002"
                  amountRequested: 100000
                  purposeCode: "HOME"
                  termMonths: 360
                }
                applicant: {
                  applicantId: "APP-002"
                  age: 25
                  annualIncome: 40000
                  creditScore: 550
                  existingDebtMonthly: 2000
                  yearsEmployed: 2
                }
              ) {
                approved
                reason
                maxApprovedAmount
                interestRateBps
                termMonths
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateLoanEligibility.approved", equalTo(false))
            .body("data.evaluateLoanEligibility.reason", notNullValue());
    }

    // ==================== Query Tests - 人寿保险 ====================

    @Test
    public void testGenerateLifeQuote_ValidApplicant() {
        String query = """
            query {
              generateLifeQuote(
                applicant: {
                  applicantId: "LIFE-001"
                  age: 30
                  gender: "M"
                  smoker: false
                  bmi: 24
                  occupation: "Engineer"
                  healthScore: 85
                }
                request: {
                  coverageAmount: 500000
                  termYears: 20
                  policyType: "TERM"
                }
              ) {
                approved
                reason
                monthlyPremium
                coverageAmount
                termYears
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.generateLifeQuote.approved", notNullValue())
            .body("data.generateLifeQuote.coverageAmount", equalTo(500000))
            .body("data.generateLifeQuote.termYears", equalTo(20));
    }

    @Test
    public void testCalculateLifeRiskScore() {
        String query = """
            query {
              calculateLifeRiskScore(
                applicant: {
                  applicantId: "LIFE-002"
                  age: 45
                  gender: "F"
                  smoker: true
                  bmi: 28
                  occupation: "Construction Worker"
                  healthScore: 60
                }
              )
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.calculateLifeRiskScore", notNullValue())
            .body("data.calculateLifeRiskScore", instanceOf(Integer.class));
    }

    // ==================== Query Tests - 汽车保险 ====================

    @Test
    public void testGenerateAutoQuote_PremiumCoverage() {
        String query = """
            query {
              generateAutoQuote(
                driver: {
                  driverId: "DRV-001"
                  age: 35
                  yearsLicensed: 15
                  accidentCount: 0
                  violationCount: 0
                  creditScore: 750
                }
                vehicle: {
                  vin: "1HGBH41JXMN109186"
                  year: 2022
                  make: "Honda"
                  model: "Accord"
                  value: 30000
                  safetyRating: 5
                }
                coverageType: "Premium"
              ) {
                approved
                reason
                monthlyPremium
                deductible
                coverageLimit
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.generateAutoQuote.approved", notNullValue())
            .body("data.generateAutoQuote.monthlyPremium", greaterThan(0));
    }

    // ==================== Query Tests - 医疗保险 ====================

    @Test
    public void testCheckServiceEligibility() {
        String query = """
            query {
              checkServiceEligibility(
                patient: {
                  patientId: "PAT-001"
                  age: 45
                  insuranceType: "GOLD"
                  hasInsurance: true
                  chronicConditions: 0
                  accountBalance: 0
                }
                service: {
                  serviceCode: "MRI-001"
                  serviceName: "MRI Scan"
                  basePrice: 1500
                  requiresPreAuth: true
                }
              ) {
                eligible
                reason
                coveragePercent
                estimatedCost
                requiresPreAuth
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.checkServiceEligibility.eligible", notNullValue())
            .body("data.checkServiceEligibility.coveragePercent", greaterThanOrEqualTo(0))
            .body("data.checkServiceEligibility.requiresPreAuth", notNullValue());
    }

    @Test
    public void testProcessClaim() {
        String query = """
            query {
              processClaim(
                claim: {
                  claimId: "CLM-001"
                  amount: 5000
                  serviceDate: "2025-01-01"
                  specialtyType: "Cardiology"
                  diagnosisCode: "I50.9"
                  hasDocumentation: true
                  patientId: "PAT-001"
                  providerId: "PRV-001"
                }
                provider: {
                  providerId: "PRV-001"
                  inNetwork: true
                  qualityScore: 90
                  specialtyType: "Cardiology"
                }
                patientCoverage: 80
              ) {
                approved
                reason
                approvedAmount
                requiresReview
                denialCode
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.processClaim.approved", notNullValue())
            .body("data.processClaim.approvedAmount", greaterThanOrEqualTo(0))
            .body("data.processClaim.denialCode", notNullValue());
    }

    // ==================== Query Tests - 信用卡评估 ====================

    @Test
    public void testEvaluateCreditCardApplication_Approved() {
        String query = """
            query {
              evaluateCreditCardApplication(
                applicant: {
                  applicantId: "CC-001"
                  age: 35
                  annualIncome: 80000
                  creditScore: 750
                  existingCreditCards: 2
                  monthlyRent: 2000
                  employmentStatus: "FULL_TIME"
                  yearsAtCurrentJob: 5
                }
                history: {
                  bankruptcyCount: 0
                  latePayments: 0
                  utilization: 30
                  accountAge: 120
                  hardInquiries: 1
                }
                offer: {
                  productType: "REWARDS"
                  requestedLimit: 10000
                  hasRewards: true
                  annualFee: 95
                }
              ) {
                approved
                reason
                approvedLimit
                interestRateAPR
                monthlyFee
                creditLine
                requiresDeposit
                depositAmount
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateCreditCardApplication.approved", notNullValue())
            .body("data.evaluateCreditCardApplication.approvedLimit", greaterThanOrEqualTo(0));
    }

    // ==================== Query Tests - 企业贷款 ====================

    @Test
    public void testEvaluateEnterpriseLoan() {
        String query = """
            query {
              evaluateEnterpriseLoan(
                enterprise: {
                  companyId: "ENT-001"
                  companyName: "Tech Solutions Inc"
                  industry: "Technology"
                  yearsInBusiness: 10
                  employeeCount: 150
                  annualRevenue: 10000000
                  revenueGrowthRate: 15
                  profitMargin: 20
                }
                position: {
                  totalAssets: 8000000
                  currentAssets: 3000000
                  totalLiabilities: 4000000
                  currentLiabilities: 1500000
                  equity: 4000000
                  cashFlow: 1200000
                  outstandingDebt: 2000000
                }
                history: {
                  previousLoans: 3
                  defaultCount: 0
                  latePayments: 1
                  creditUtilization: 40
                  largestLoanAmount: 2000000
                  relationshipYears: 5
                }
                application: {
                  requestedAmount: 3000000
                  loanPurpose: "EXPANSION"
                  termMonths: 60
                  collateralValue: 4000000
                  guarantorCount: 2
                }
              ) {
                approved
                approvedAmount
                interestRateBps
                termMonths
                collateralRequired
                specialConditions
                riskCategory
                confidenceScore
                reasonCode
                detailedAnalysis
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateEnterpriseLoan.approved", notNullValue())
            .body("data.evaluateEnterpriseLoan.riskCategory", notNullValue());
    }

    // ==================== Query Tests - 个人贷款 ====================

    @Test
    public void testEvaluatePersonalLoan() {
        String query = """
            query {
              evaluatePersonalLoan(
                personal: {
                  applicantId: "PER-001"
                  age: 32
                  educationLevel: "BACHELOR"
                  employmentStatus: "EMPLOYED"
                  occupation: "Software Engineer"
                  yearsAtJob: 5
                  monthsAtAddress: 36
                  maritalStatus: "MARRIED"
                  dependents: 1
                }
                income: {
                  monthlyIncome: 8000
                  additionalIncome: 1000
                  spouseIncome: 5000
                  rentIncome: 0
                  incomeStability: "PERMANENT"
                  incomeGrowthRate: 10
                }
                credit: {
                  creditScore: 720
                  creditHistory: 96
                  activeLoans: 1
                  creditCardCount: 3
                  creditUtilization: 25
                  latePayments: 0
                  defaults: 0
                  bankruptcies: 0
                  inquiries: 2
                }
                debt: {
                  totalMonthlyDebt: 3000
                  mortgagePayment: 2000
                  carPayment: 500
                  studentLoanPayment: 300
                  creditCardMinPayment: 200
                  otherDebtPayment: 0
                  totalOutstandingDebt: 50000
                }
                request: {
                  requestedAmount: 25000
                  purpose: "HOME_IMPROVEMENT"
                  termMonths: 60
                  downPayment: 5000
                  collateralValue: 30000
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
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluatePersonalLoan.approved", notNullValue())
            .body("data.evaluatePersonalLoan.riskLevel", notNullValue());
    }

    // ==================== Mutation Tests - 缓存管理 ====================

    @Test
    public void testMultiTenantCacheIsolation() {
        String query = """
            query {
              evaluateLoanEligibility(
                application: {
                  loanId: \"TENANT-CACHE\"
                  applicantId: \"APP-TENANT\"
                  amountRequested: 75000
                  purposeCode: \"HOME\"
                  termMonths: 240
                }
                applicant: {
                  applicantId: \"APP-TENANT\"
                  age: 42
                  annualIncome: 95000
                  creditScore: 720
                  existingDebtMonthly: 1200
                  yearsEmployed: 12
                }
              ) {
                approved
                reason
                maxApprovedAmount
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateLoanEligibility.approved", notNullValue());

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-b")
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateLoanEligibility.approved", notNullValue());

        Set<PolicyCacheKey> tenantAKeys = policyEvaluationService.snapshotTenantCacheKeys("tenant-a");
        Set<PolicyCacheKey> tenantBKeys = policyEvaluationService.snapshotTenantCacheKeys("tenant-b");

        Assertions.assertEquals(1, tenantAKeys.size(), "tenant-a 应仅创建一条缓存记录");
        Assertions.assertEquals(1, tenantBKeys.size(), "tenant-b 应仅创建一条缓存记录");

        PolicyCacheKey keyA = tenantAKeys.iterator().next();
        PolicyCacheKey keyB = tenantBKeys.iterator().next();

        Assertions.assertEquals("tenant-a", keyA.getTenantId(), "tenant-a 缓存键应包含正确租户");
        Assertions.assertEquals("tenant-b", keyB.getTenantId(), "tenant-b 缓存键应包含正确租户");
        Assertions.assertNotEquals(keyA, keyB, "不同租户的缓存键必须不同");
    }

    @Test
    public void testClearAllCache() {
        String mutation = """
            mutation {
              clearAllCache {
                success
                message
                timestamp
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(mutation))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.clearAllCache.success", equalTo(true))
            .body("data.clearAllCache.message", containsString("cleared"))
            .body("data.clearAllCache.timestamp", notNullValue());
    }

    @Test
    public void testCacheInvalidation() {
        String tenant = "tenant-cache";
        String query = """
            query {
              evaluateLoanEligibility(
                application: {
                  loanId: \"CACHE-001\"
                  applicantId: \"CACHE-APPLICANT\"
                  amountRequested: 65000
                  purposeCode: \"HOME\"
                  termMonths: 180
                }
                applicant: {
                  applicantId: \"CACHE-APPLICANT\"
                  age: 37
                  annualIncome: 88000
                  creditScore: 710
                  existingDebtMonthly: 900
                  yearsEmployed: 8
                }
              ) {
                approved
                maxApprovedAmount
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenant)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateLoanEligibility.approved", notNullValue());

        Assertions.assertEquals(1, policyEvaluationService.snapshotTenantCacheKeys(tenant).size(), "首次调用后应生成一条缓存");

        String mutation = """
            mutation {
              invalidateCache(
                policyModule: \"aster.finance.loan\"
                policyFunction: \"evaluateLoanEligibility\"
              ) {
                success
                message
                timestamp
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenant)
            .body(graphQLRequest(mutation))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.invalidateCache.success", equalTo(true))
            .body("data.invalidateCache.message", allOf(containsString("tenant-cache"), containsString("aster.finance.loan")))
            .body("data.invalidateCache.timestamp", notNullValue());

        Assertions.assertTrue(policyEvaluationService.snapshotTenantCacheKeys(tenant).isEmpty(), "缓存失效后不应保留旧键");

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenant)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateLoanEligibility.approved", notNullValue());

        Assertions.assertEquals(1, policyEvaluationService.snapshotTenantCacheKeys(tenant).size(), "重新调用后应重新建立缓存");
    }

    // ==================== Error Handling Tests ====================

    @Test
    public void testMissingRequiredField() {
        String query = """
            query {
              evaluateLoanEligibility(
                application: {
                  loanId: "LOAN-003"
                  applicantId: "APP-003"
                  amountRequested: 50000
                  termMonths: 360
                }
                applicant: {
                  applicantId: "APP-003"
                  age: 35
                  annualIncome: 80000
                  creditScore: 700
                  existingDebtMonthly: 1000
                  yearsEmployed: 7
                }
              ) {
                approved
                reason
              }
            }
            """;

        // Missing purposeCode - should return GraphQL validation error
        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("errors", notNullValue());
    }

    @Test
    public void testInvalidQuerySyntax() {
        String query = """
            query {
              evaluateLoanEligibility {
                approved
              }
            }
            """;

        // Missing required arguments
        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("errors", notNullValue());
    }

    // ==================== Integration Tests - 组合查询 ====================

    @Test
    public void testMultipleQueriesInSingleRequest() {
        String query = """
            query {
              loan: evaluateLoanEligibility(
                application: {
                  loanId: "LOAN-004"
                  applicantId: "APP-004"
                  amountRequested: 30000
                  purposeCode: "AUTO"
                  termMonths: 60
                }
                applicant: {
                  applicantId: "APP-004"
                  age: 28
                  annualIncome: 60000
                  creditScore: 680
                  existingDebtMonthly: 800
                  yearsEmployed: 5
                }
              ) {
                approved
                maxApprovedAmount
              }

              creditCard: evaluateCreditCardApplication(
                applicant: {
                  applicantId: "CC-002"
                  age: 28
                  annualIncome: 60000
                  creditScore: 680
                  existingCreditCards: 1
                  monthlyRent: 1500
                  employmentStatus: "FULL_TIME"
                  yearsAtCurrentJob: 3
                }
                history: {
                  bankruptcyCount: 0
                  latePayments: 1
                  utilization: 40
                  accountAge: 60
                  hardInquiries: 2
                }
                offer: {
                  productType: "STANDARD"
                  requestedLimit: 5000
                  hasRewards: false
                  annualFee: 0
                }
              ) {
                approved
                approvedLimit
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.loan.approved", notNullValue())
            .body("data.creditCard.approved", notNullValue());
    }

    @Test
    public void testQueryWithFragment() {
        String query = """
            fragment DecisionFields on LoanDecision {
              approved
              reason
              maxApprovedAmount
              interestRateBps
              termMonths
            }

            query {
              evaluateLoanEligibility(
                application: {
                  loanId: "LOAN-005"
                  applicantId: "APP-005"
                  amountRequested: 40000
                  purposeCode: "EDUCATION"
                  termMonths: 120
                }
                applicant: {
                  applicantId: "APP-005"
                  age: 22
                  annualIncome: 30000
                  creditScore: 650
                  existingDebtMonthly: 300
                  yearsEmployed: 1
                }
              ) {
                ...DecisionFields
              }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(graphQLRequest(query))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("data.evaluateLoanEligibility.approved", notNullValue())
            .body("data.evaluateLoanEligibility.reason", notNullValue())
            .body("data.evaluateLoanEligibility.maxApprovedAmount", notNullValue());
    }
}
