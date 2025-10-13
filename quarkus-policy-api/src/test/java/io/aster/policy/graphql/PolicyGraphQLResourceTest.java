package io.aster.policy.graphql;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
                  hasChronicConditions: false
                  accountBalance: 0
                }
                service: {
                  serviceCode: "MRI-001"
                  cost: 1500
                  isEssential: true
                }
              ) {
                eligible
                reason
                coveragePercentage
                patientCost
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
            .body("data.checkServiceEligibility.coveragePercentage", greaterThanOrEqualTo(0));
    }

    @Test
    public void testProcessClaim() {
        String query = """
            query {
              processClaim(
                claim: {
                  claimId: "CLM-001"
                  claimAmount: 5000
                  serviceDate: "2025-01-01"
                  specialtyType: "Cardiology"
                  diagnosisCode: "I50.9"
                  hasDocumentation: true
                }
                provider: {
                  providerId: "PRV-001"
                  inNetwork: true
                  qualityScore: 90
                }
                patient: {
                  patientId: "PAT-001"
                  age: 55
                  insuranceType: "GOLD"
                  hasChronicConditions: true
                  accountBalance: 500
                }
              ) {
                approved
                reason
                approvedAmount
                requiresReview
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
            .body("data.processClaim.approvedAmount", greaterThanOrEqualTo(0));
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
                  incomeStability: 90
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
                  monthlyMortgage: 2000
                  monthlyCarPayment: 500
                  monthlyStudentLoan: 300
                  monthlyCreditCardPayment: 200
                  otherMonthlyDebt: 0
                  totalOutstandingDebt: 50000
                }
                request: {
                  requestedAmount: 25000
                  loanPurpose: "HOME_IMPROVEMENT"
                  desiredTermMonths: 60
                  downPayment: 5000
                  collateralValue: 0
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
    public void testInvalidateCache() {
        String mutation = """
            mutation {
              invalidateCache(
                policyModule: "aster.finance.loan"
                policyFunction: "evaluateLoanEligibility"
              ) {
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
            .body("data.invalidateCache.success", equalTo(true))
            .body("data.invalidateCache.message", containsString("invalidated"))
            .body("data.invalidateCache.timestamp", notNullValue());
    }

    // ==================== Error Handling Tests ====================

    @Test
    public void testMissingRequiredField() {
        String query = """
            query {
              evaluateLoanEligibility(
                application: {
                  loanId: "LOAN-003"
                  amountRequested: 50000
                  termMonths: 360
                }
                applicant: {
                  applicantId: "APP-003"
                  age: 35
                  annualIncome: 80000
                  creditScore: 700
                  existingDebtMonthly: 1000
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
