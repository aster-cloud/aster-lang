package io.aster.policy.graphql;

import io.aster.policy.api.PolicyEvaluationService;
import io.aster.policy.graphql.types.AutoInsuranceTypes;
import io.aster.policy.graphql.types.CreditCardTypes;
import io.aster.policy.graphql.types.EnterpriseLendingTypes;
import io.aster.policy.graphql.types.HealthcareTypes;
import io.aster.policy.graphql.types.LifeInsuranceTypes;
import io.aster.policy.graphql.types.PolicyTypes;
import io.aster.policy.graphql.types.LoanTypes;
import io.aster.policy.graphql.types.PersonalLendingTypes;
import io.aster.policy.service.PolicyStorageService;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

/**
 * GraphQL API for Policy Evaluation
 *
 * 提供GraphQL查询接口用于复杂策略评估
 * 支持类型安全的参数传递和结果返回
 */
@GraphQLApi
public class PolicyGraphQLResource {

    private static final Logger LOG = Logger.getLogger(PolicyGraphQLResource.class);

    @Inject
    PolicyEvaluationService policyEvaluationService;

    @Inject
    PolicyStorageService policyStorageService;

    @Context
    RoutingContext routingContext;

    private String tenantId() {
        // GraphQL 请求不经过 RESTEasy Reactive 的 JAX-RS 上下文，这里改用 Vert.x RoutingContext 获取 HTTP 头
        if (routingContext == null || routingContext.request() == null) {
            return "default";
        }
        String tenant = routingContext.request().getHeader("X-Tenant-Id");
        return tenant == null || tenant.isBlank() ? "default" : tenant.trim();
    }

    // ==================== Life Insurance Queries ====================

    @Query("generateLifeQuote")
    @Description("生成人寿保险报价 / Generate life insurance quote")
    public Uni<LifeInsuranceTypes.Quote> generateLifeQuote(
            @NonNull @Description("申请人信息 / Applicant information")
            LifeInsuranceTypes.Applicant applicant,

            @NonNull @Description("保单请求信息 / Policy request information")
            LifeInsuranceTypes.PolicyRequest request
    ) {
        // 将GraphQL类型转换为CNL策略需要的格式
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.insurance.life",
                "generateLifeQuote",
                buildContext(
                    tenant,
                    convertToAsterApplicant(applicant),
                    convertToAsterPolicyRequest(request)
                )
            )
            .onItem().transform(result -> {
                // 将结果转换为GraphQL类型
                Object asterResult = result.getResult();
                return convertToGraphQLQuote(asterResult);
            });
    }

    @Query("calculateLifeRiskScore")
    @Description("计算人寿保险风险评分 / Calculate life insurance risk score")
    public Uni<Integer> calculateLifeRiskScore(
            @NonNull @Description("申请人信息 / Applicant information")
            LifeInsuranceTypes.Applicant applicant
    ) {
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.insurance.life",
                "calculateRiskScore",
                buildContext(
                    tenant,
                    convertToAsterApplicant(applicant)
                )
            )
            .onItem().transform(result -> (Integer) result.getResult());
    }

    // ==================== Auto Insurance Queries ====================

    @Query("generateAutoQuote")
    @Description("生成汽车保险报价 / Generate auto insurance quote")
    public Uni<AutoInsuranceTypes.PolicyQuote> generateAutoQuote(
            @NonNull @Description("驾驶员信息 / Driver information")
            AutoInsuranceTypes.Driver driver,

            @NonNull @Description("车辆信息 / Vehicle information")
            AutoInsuranceTypes.Vehicle vehicle,

            @NonNull @Description("保险类型 / Coverage type (Premium/Standard/Basic)")
            String coverageType
    ) {
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.insurance.auto",
                "generateAutoQuote",
                buildContext(
                    tenant,
                    convertToAsterDriver(driver),
                    convertToAsterVehicle(vehicle),
                    coverageType
                )
            )
            .onItem().transform(result -> convertToGraphQLPolicyQuote(result.getResult()));
    }

    // ==================== Healthcare Queries ====================

    @Query("checkServiceEligibility")
    @Description("检查医疗服务资格 / Check healthcare service eligibility")
    public Uni<HealthcareTypes.EligibilityCheck> checkServiceEligibility(
            @NonNull @Description("患者信息 / Patient information")
            HealthcareTypes.Patient patient,

            @NonNull @Description("服务信息 / Service information")
            HealthcareTypes.Service service
    ) {
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.healthcare.eligibility",
                "checkServiceEligibility",
                buildContext(
                    tenant,
                    convertToAsterPatient(patient),
                    convertToAsterService(service)
                )
            )
            .onItem().transform(result -> convertToGraphQLEligibilityCheck(result.getResult()));
    }

    @Query("processClaim")
    @Description("处理医疗索赔 / Process healthcare claim")
    public Uni<HealthcareTypes.ClaimDecision> processClaim(
            @NonNull @Description("索赔信息 / Claim information")
            HealthcareTypes.Claim claim,

            @NonNull @Description("提供者信息 / Provider information")
            HealthcareTypes.Provider provider,

            @NonNull @Description("患者保障比例 / Patient coverage percentage")
            Integer patientCoverage
    ) {
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.healthcare.claims",
                "processClaim",
                buildContext(
                    tenant,
                    convertToAsterClaim(claim),
                    convertToAsterProvider(provider),
                    patientCoverage
                )
            )
            .onItem().transform(result -> convertToGraphQLClaimDecision(result.getResult()));
    }

    // ==================== Loan Queries ====================

    @Query("evaluateLoanEligibility")
    @Description("评估贷款资格 / Evaluate loan eligibility")
    public Uni<LoanTypes.Decision> evaluateLoanEligibility(
            @NonNull @Description("贷款申请信息 / Loan application information")
            LoanTypes.Application application,

            @NonNull @Description("申请人信息 / Applicant profile")
            LoanTypes.Applicant applicant
    ) {
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.finance.loan",
                "evaluateLoanEligibility",
                buildContext(
                    tenant,
                    convertToAsterLoanApplication(application),
                    convertToAsterLoanApplicant(applicant)
                )
            )
            .onItem().transform(result -> convertToGraphQLLoanDecision(result.getResult()));
    }

    // ==================== Credit Card Queries ====================

    @Query("evaluateCreditCardApplication")
    @Description("评估信用卡申请 / Evaluate credit card application")
    public Uni<CreditCardTypes.ApprovalDecision> evaluateCreditCardApplication(
            @NonNull @Description("申请人信息 / Applicant information")
            CreditCardTypes.ApplicantInfo applicant,

            @NonNull @Description("财务历史 / Financial history")
            CreditCardTypes.FinancialHistory history,

            @NonNull @Description("信用卡产品 / Credit card offer")
            CreditCardTypes.CreditCardOffer offer
    ) {
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.finance.creditcard",
                "evaluateCreditCardApplication",
                buildContext(
                    tenant,
                    convertToAsterCreditCardApplicant(applicant),
                    convertToAsterFinancialHistory(history),
                    convertToAsterCreditCardOffer(offer)
                )
            )
            .onItem().transform(result -> convertToGraphQLApprovalDecision(result.getResult()));
    }

    // ==================== Enterprise Lending Queries ====================

    @Query("evaluateEnterpriseLoan")
    @Description("评估企业贷款 / Evaluate enterprise loan")
    public Uni<EnterpriseLendingTypes.LendingDecision> evaluateEnterpriseLoan(
            @NonNull @Description("企业基本信息 / Enterprise information")
            EnterpriseLendingTypes.EnterpriseInfo enterprise,

            @NonNull @Description("财务状况 / Financial position")
            EnterpriseLendingTypes.FinancialPosition position,

            @NonNull @Description("企业历史记录 / Business history")
            EnterpriseLendingTypes.BusinessHistory history,

            @NonNull @Description("贷款申请 / Loan application")
            EnterpriseLendingTypes.LoanApplication application
    ) {
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.finance.enterprise_lending",
                "evaluateEnterpriseLoan",
                buildContext(
                    tenant,
                    convertToAsterEnterpriseInfo(enterprise),
                    convertToAsterFinancialPosition(position),
                    convertToAsterBusinessHistory(history),
                    convertToAsterEnterpriseLoanApplication(application)
                )
            )
            .onItem().transform(result -> convertToGraphQLEnterpriseLendingDecision(result.getResult()));
    }

    // ==================== Personal Lending Queries ====================

    @Query("evaluatePersonalLoan")
    @Description("评估个人贷款 / Evaluate personal loan")
    public Uni<PersonalLendingTypes.LoanDecision> evaluatePersonalLoan(
            @NonNull @Description("个人基本信息 / Personal information")
            PersonalLendingTypes.PersonalInfo personal,

            @NonNull @Description("收入状况 / Income profile")
            PersonalLendingTypes.IncomeProfile income,

            @NonNull @Description("信用状况 / Credit profile")
            PersonalLendingTypes.CreditProfile credit,

            @NonNull @Description("债务状况 / Debt profile")
            PersonalLendingTypes.DebtProfile debt,

            @NonNull @Description("贷款申请 / Loan request")
            PersonalLendingTypes.LoanRequest request
    ) {
        final String tenant = tenantId();
        return policyEvaluationService.evaluatePolicy(
                tenant,
                "aster.finance.personal_lending",
                "evaluatePersonalLoan",
                buildContext(
                    tenant,
                    convertToAsterPersonalInfo(personal),
                    convertToAsterIncomeProfile(income),
                    convertToAsterCreditProfile(credit),
                    convertToAsterDebtProfile(debt),
                    convertToAsterPersonalLoanRequest(request)
                )
            )
            .onItem().transform(result -> convertToGraphQLPersonalLoanDecision(result.getResult()));
    }

    // ==================== Conversion Helper Methods ====================
    // 这些方法将GraphQL类型转换为Map格式，供PolicyEvaluationService使用

    private java.util.Map<String, Object> convertToAsterApplicant(LifeInsuranceTypes.Applicant applicant) {
        return java.util.Map.of(
            "applicantId", applicant.applicantId,
            "age", applicant.age,
            "gender", applicant.gender,
            "smoker", applicant.smoker,
            "bmi", applicant.bmi,
            "occupation", applicant.occupation,
            "healthScore", applicant.healthScore
        );
    }

    private java.util.Map<String, Object> convertToAsterPolicyRequest(LifeInsuranceTypes.PolicyRequest request) {
        return java.util.Map.of(
            "coverageAmount", request.coverageAmount,
            "termYears", request.termYears,
            "policyType", request.policyType
        );
    }

    private java.util.Map<String, Object> convertToAsterDriver(AutoInsuranceTypes.Driver driver) {
        return java.util.Map.of(
            "driverId", driver.driverId,
            "age", driver.age,
            "yearsLicensed", driver.yearsLicensed,
            "accidentCount", driver.accidentCount,
            "violationCount", driver.violationCount,
            "creditScore", driver.creditScore
        );
    }

    private java.util.Map<String, Object> convertToAsterVehicle(AutoInsuranceTypes.Vehicle vehicle) {
        return java.util.Map.of(
            "vin", vehicle.vin,
            "year", vehicle.year,
            "make", vehicle.make,
            "model", vehicle.model,
            "value", vehicle.value,
            "safetyRating", vehicle.safetyRating
        );
    }

    private java.util.Map<String, Object> convertToAsterPatient(HealthcareTypes.Patient patient) {
        return java.util.Map.of(
            "patientId", patient.patientId,
            "age", patient.age,
            "insuranceType", patient.insuranceType,
            "hasInsurance", patient.hasInsurance,
            "chronicConditions", patient.chronicConditions,
            "accountBalance", patient.accountBalance
        );
    }

    private java.util.Map<String, Object> convertToAsterService(HealthcareTypes.Service service) {
        return java.util.Map.of(
            "serviceCode", service.serviceCode,
            "serviceName", service.serviceName,
            "basePrice", service.basePrice,
            "requiresPreAuth", service.requiresPreAuth
        );
    }

    private java.util.Map<String, Object> convertToAsterClaim(HealthcareTypes.Claim claim) {
        return java.util.Map.of(
            "claimId", claim.claimId,
            "amount", claim.amount,
            "serviceDate", claim.serviceDate,
            "specialtyType", claim.specialtyType,
            "diagnosisCode", claim.diagnosisCode,
            "hasDocumentation", claim.hasDocumentation,
            "patientId", claim.patientId,
            "providerId", claim.providerId
        );
    }

    private java.util.Map<String, Object> convertToAsterProvider(HealthcareTypes.Provider provider) {
        return java.util.Map.of(
            "providerId", provider.providerId,
            "inNetwork", provider.inNetwork,
            "qualityScore", provider.qualityScore,
            "specialtyType", provider.specialtyType
        );
    }

    // 将Aster策略结果转换为GraphQL类型
    private LifeInsuranceTypes.Quote convertToGraphQLQuote(Object asterResult) {
        if (asterResult == null) {
            return null;
        }

        try {
            // 使用反射从Aster类型中提取字段值
            Class<?> resultClass = asterResult.getClass();
            Boolean approved = (Boolean) resultClass.getField("approved").get(asterResult);
            String reason = (String) resultClass.getField("reason").get(asterResult);
            Integer monthlyPremium = (Integer) resultClass.getField("monthlyPremium").get(asterResult);
            Integer coverageAmount = (Integer) resultClass.getField("coverageAmount").get(asterResult);
            Integer termYears = (Integer) resultClass.getField("termYears").get(asterResult);

            return new LifeInsuranceTypes.Quote(approved, reason, monthlyPremium, coverageAmount, termYears);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    private AutoInsuranceTypes.PolicyQuote convertToGraphQLPolicyQuote(Object asterResult) {
        if (asterResult == null) {
            return null;
        }

        try {
            Class<?> resultClass = asterResult.getClass();
            Boolean approved = (Boolean) resultClass.getField("approved").get(asterResult);
            String reason = (String) resultClass.getField("reason").get(asterResult);
            Integer monthlyPremium = (Integer) resultClass.getField("monthlyPremium").get(asterResult);
            Integer deductible = (Integer) resultClass.getField("deductible").get(asterResult);
            Integer coverageLimit = (Integer) resultClass.getField("coverageLimit").get(asterResult);

            return new AutoInsuranceTypes.PolicyQuote(approved, reason, monthlyPremium, deductible, coverageLimit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    private HealthcareTypes.EligibilityCheck convertToGraphQLEligibilityCheck(Object asterResult) {
        if (asterResult == null) {
            return null;
        }

        try {
            Class<?> resultClass = asterResult.getClass();
            Boolean eligible = (Boolean) resultClass.getField("eligible").get(asterResult);
            String reason = (String) resultClass.getField("reason").get(asterResult);
            Integer coveragePercent = (Integer) resultClass.getField("coveragePercent").get(asterResult);
            Integer estimatedCost = (Integer) resultClass.getField("estimatedCost").get(asterResult);
            Boolean requiresPreAuth = (Boolean) resultClass.getField("requiresPreAuth").get(asterResult);

            return new HealthcareTypes.EligibilityCheck(eligible, reason, coveragePercent, estimatedCost, requiresPreAuth);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    private HealthcareTypes.ClaimDecision convertToGraphQLClaimDecision(Object asterResult) {
        if (asterResult == null) {
            return null;
        }

        try {
            Class<?> resultClass = asterResult.getClass();
            Boolean approved = (Boolean) resultClass.getField("approved").get(asterResult);
            String reason = (String) resultClass.getField("reason").get(asterResult);
            Integer approvedAmount = (Integer) resultClass.getField("approvedAmount").get(asterResult);
            Boolean requiresReview = (Boolean) resultClass.getField("requiresReview").get(asterResult);
            String denialCode = (String) resultClass.getField("denialCode").get(asterResult);

            return new HealthcareTypes.ClaimDecision(approved, reason, approvedAmount, requiresReview, denialCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    private java.util.Map<String, Object> convertToAsterLoanApplication(LoanTypes.Application application) {
        return java.util.Map.of(
            "loanId", application.loanId,
            "applicantId", application.applicantId,
            "amount", application.amountRequested,
            "termMonths", application.termMonths,
            "purpose", application.purposeCode
        );
    }

    private java.util.Map<String, Object> convertToAsterLoanApplicant(LoanTypes.Applicant applicant) {
        return java.util.Map.of(
            "age", applicant.age,
            "creditScore", applicant.creditScore,
            "annualIncome", applicant.annualIncome,
            "monthlyDebt", applicant.existingDebtMonthly,
            "yearsEmployed", applicant.yearsEmployed
        );
    }

    private LoanTypes.Decision convertToGraphQLLoanDecision(Object asterResult) {
        if (asterResult == null) {
            return null;
        }

        try {
            Class<?> resultClass = asterResult.getClass();
            Boolean approved = (Boolean) resultClass.getField("approved").get(asterResult);
            String reason = (String) resultClass.getField("reason").get(asterResult);
            Integer approvedAmount = (Integer) resultClass.getField("approvedAmount").get(asterResult);
            Integer interestRateBps = (Integer) resultClass.getField("interestRateBps").get(asterResult);
            Integer termMonths = (Integer) resultClass.getField("termMonths").get(asterResult);

            return new LoanTypes.Decision(approved, reason, approvedAmount, interestRateBps, termMonths);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    private java.util.Map<String, Object> convertToAsterCreditCardApplicant(CreditCardTypes.ApplicantInfo applicant) {
        return java.util.Map.of(
            "applicantId", applicant.applicantId,
            "age", applicant.age,
            "annualIncome", applicant.annualIncome,
            "creditScore", applicant.creditScore,
            "existingCreditCards", applicant.existingCreditCards,
            "monthlyRent", applicant.monthlyRent,
            "employmentStatus", applicant.employmentStatus,
            "yearsAtCurrentJob", applicant.yearsAtCurrentJob
        );
    }

    private java.util.Map<String, Object> convertToAsterFinancialHistory(CreditCardTypes.FinancialHistory history) {
        return java.util.Map.of(
            "bankruptcyCount", history.bankruptcyCount,
            "latePayments", history.latePayments,
            "utilization", history.utilization,
            "accountAge", history.accountAge,
            "hardInquiries", history.hardInquiries
        );
    }

    private java.util.Map<String, Object> convertToAsterCreditCardOffer(CreditCardTypes.CreditCardOffer offer) {
        return java.util.Map.of(
            "productType", offer.productType,
            "requestedLimit", offer.requestedLimit,
            "hasRewards", offer.hasRewards,
            "annualFee", offer.annualFee
        );
    }

    private CreditCardTypes.ApprovalDecision convertToGraphQLApprovalDecision(Object asterResult) {
        if (asterResult == null) {
            return null;
        }

        try {
            Class<?> resultClass = asterResult.getClass();
            Boolean approved = (Boolean) resultClass.getField("approved").get(asterResult);
            String reason = (String) resultClass.getField("reason").get(asterResult);
            Integer approvedLimit = (Integer) resultClass.getField("approvedLimit").get(asterResult);
            Integer interestRateAPR = (Integer) resultClass.getField("interestRateAPR").get(asterResult);
            Integer monthlyFee = (Integer) resultClass.getField("monthlyFee").get(asterResult);
            Integer creditLine = (Integer) resultClass.getField("creditLine").get(asterResult);
            Boolean requiresDeposit = (Boolean) resultClass.getField("requiresDeposit").get(asterResult);
            Integer depositAmount = (Integer) resultClass.getField("depositAmount").get(asterResult);

            return new CreditCardTypes.ApprovalDecision(approved, reason, approvedLimit, interestRateAPR,
                                                       monthlyFee, creditLine, requiresDeposit, depositAmount);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    // ==================== Enterprise Lending Conversion Methods ====================

    private java.util.Map<String, Object> convertToAsterEnterpriseInfo(EnterpriseLendingTypes.EnterpriseInfo enterprise) {
        return java.util.Map.of(
            "companyId", enterprise.companyId,
            "companyName", enterprise.companyName,
            "industry", enterprise.industry,
            "yearsInBusiness", enterprise.yearsInBusiness,
            "employeeCount", enterprise.employeeCount,
            "annualRevenue", enterprise.annualRevenue,
            "revenueGrowthRate", enterprise.revenueGrowthRate,
            "profitMargin", enterprise.profitMargin
        );
    }

    private java.util.Map<String, Object> convertToAsterFinancialPosition(EnterpriseLendingTypes.FinancialPosition position) {
        return java.util.Map.of(
            "totalAssets", position.totalAssets,
            "currentAssets", position.currentAssets,
            "totalLiabilities", position.totalLiabilities,
            "currentLiabilities", position.currentLiabilities,
            "equity", position.equity,
            "cashFlow", position.cashFlow,
            "outstandingDebt", position.outstandingDebt
        );
    }

    private java.util.Map<String, Object> convertToAsterBusinessHistory(EnterpriseLendingTypes.BusinessHistory history) {
        return java.util.Map.of(
            "previousLoans", history.previousLoans,
            "defaultCount", history.defaultCount,
            "latePayments", history.latePayments,
            "creditUtilization", history.creditUtilization,
            "largestLoanAmount", history.largestLoanAmount,
            "relationshipYears", history.relationshipYears
        );
    }

    private java.util.Map<String, Object> convertToAsterEnterpriseLoanApplication(EnterpriseLendingTypes.LoanApplication application) {
        return java.util.Map.of(
            "requestedAmount", application.requestedAmount,
            "loanPurpose", application.loanPurpose,
            "termMonths", application.termMonths,
            "collateralValue", application.collateralValue,
            "guarantorCount", application.guarantorCount
        );
    }

    private EnterpriseLendingTypes.LendingDecision convertToGraphQLEnterpriseLendingDecision(Object asterResult) {
        if (asterResult == null) {
            return null;
        }

        try {
            Class<?> resultClass = asterResult.getClass();
            Boolean approved = (Boolean) resultClass.getField("approved").get(asterResult);
            Integer approvedAmount = (Integer) resultClass.getField("approvedAmount").get(asterResult);
            Integer interestRateBps = (Integer) resultClass.getField("interestRateBps").get(asterResult);
            Integer termMonths = (Integer) resultClass.getField("termMonths").get(asterResult);
            Integer collateralRequired = (Integer) resultClass.getField("collateralRequired").get(asterResult);
            String specialConditions = (String) resultClass.getField("specialConditions").get(asterResult);
            String riskCategory = (String) resultClass.getField("riskCategory").get(asterResult);
            Integer confidenceScore = (Integer) resultClass.getField("confidenceScore").get(asterResult);
            String reasonCode = (String) resultClass.getField("reasonCode").get(asterResult);
            String detailedAnalysis = (String) resultClass.getField("detailedAnalysis").get(asterResult);

            return new EnterpriseLendingTypes.LendingDecision(approved, approvedAmount, interestRateBps, termMonths,
                collateralRequired, specialConditions, riskCategory, confidenceScore, reasonCode, detailedAnalysis);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    // ==================== Personal Lending Conversion Methods ====================

    private java.util.Map<String, Object> convertToAsterPersonalInfo(PersonalLendingTypes.PersonalInfo personal) {
        return java.util.Map.of(
            "applicantId", personal.applicantId,
            "age", personal.age,
            "educationLevel", personal.educationLevel,
            "employmentStatus", personal.employmentStatus,
            "occupation", personal.occupation,
            "yearsAtJob", personal.yearsAtJob,
            "monthsAtAddress", personal.monthsAtAddress,
            "maritalStatus", personal.maritalStatus,
            "dependents", personal.dependents
        );
    }

    private java.util.Map<String, Object> convertToAsterIncomeProfile(PersonalLendingTypes.IncomeProfile income) {
        return java.util.Map.of(
            "monthlyIncome", income.monthlyIncome,
            "additionalIncome", income.additionalIncome,
            "spouseIncome", income.spouseIncome,
            "rentIncome", income.rentIncome,
            "incomeStability", income.incomeStability,
            "incomeGrowthRate", income.incomeGrowthRate
        );
    }

    private java.util.Map<String, Object> convertToAsterCreditProfile(PersonalLendingTypes.CreditProfile credit) {
        return java.util.Map.of(
            "creditScore", credit.creditScore,
            "creditHistory", credit.creditHistory,
            "activeLoans", credit.activeLoans,
            "creditCardCount", credit.creditCardCount,
            "creditUtilization", credit.creditUtilization,
            "latePayments", credit.latePayments,
            "defaults", credit.defaults,
            "bankruptcies", credit.bankruptcies,
            "inquiries", credit.inquiries
        );
    }

    private java.util.Map<String, Object> convertToAsterDebtProfile(PersonalLendingTypes.DebtProfile debt) {
        return java.util.Map.of(
            "totalMonthlyDebt", debt.totalMonthlyDebt,
            "mortgagePayment", debt.mortgagePayment,
            "carPayment", debt.carPayment,
            "studentLoanPayment", debt.studentLoanPayment,
            "creditCardMinPayment", debt.creditCardMinPayment,
            "otherDebtPayment", debt.otherDebtPayment,
            "totalOutstandingDebt", debt.totalOutstandingDebt
        );
    }

    private java.util.Map<String, Object> convertToAsterPersonalLoanRequest(PersonalLendingTypes.LoanRequest request) {
        return java.util.Map.of(
            "requestedAmount", request.requestedAmount,
            "purpose", request.purpose,
            "termMonths", request.termMonths,
            "downPayment", request.downPayment,
            "collateralValue", request.collateralValue
        );
    }

    private PersonalLendingTypes.LoanDecision convertToGraphQLPersonalLoanDecision(Object asterResult) {
        if (asterResult == null) {
            return null;
        }

        try {
            Class<?> resultClass = asterResult.getClass();
            Boolean approved = (Boolean) resultClass.getField("approved").get(asterResult);
            Integer approvedAmount = (Integer) resultClass.getField("approvedAmount").get(asterResult);
            Integer interestRateBps = (Integer) resultClass.getField("interestRateBps").get(asterResult);
            Integer termMonths = (Integer) resultClass.getField("termMonths").get(asterResult);
            Integer monthlyPayment = (Integer) resultClass.getField("monthlyPayment").get(asterResult);
            Integer downPaymentRequired = (Integer) resultClass.getField("downPaymentRequired").get(asterResult);
            String conditions = (String) resultClass.getField("conditions").get(asterResult);
            String riskLevel = (String) resultClass.getField("riskLevel").get(asterResult);
            Integer decisionScore = (Integer) resultClass.getField("decisionScore").get(asterResult);
            String reasonCode = (String) resultClass.getField("reasonCode").get(asterResult);
            String recommendations = (String) resultClass.getField("recommendations").get(asterResult);

            return new PersonalLendingTypes.LoanDecision(approved, approvedAmount, interestRateBps, termMonths,
                monthlyPayment, downPaymentRequired, conditions, riskLevel, decisionScore, reasonCode, recommendations);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    // ==================== Policy Management ====================

    @Query("getPolicy")
    @Description("根据ID获取策略 / Get policy by ID")
    public Uni<PolicyTypes.Policy> getPolicy(
            @NonNull @Description("策略ID / Policy identifier")
            String id
    ) {
        return Uni.createFrom().item(() ->
            policyStorageService.getPolicy(tenantId(), id)
                .map(this::convertToGraphQLPolicy)
                .orElse(null)
        );
    }

    @Query("listPolicies")
    @Description("列出所有策略 / List policies")
    public Uni<List<PolicyTypes.Policy>> listPolicies() {
        return Uni.createFrom().item(() -> {
            String tenant = tenantId();
            List<PolicyStorageService.PolicyDocument> documents = policyStorageService.listPolicies(tenant);
            List<PolicyTypes.Policy> policies = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                PolicyStorageService.PolicyDocument document = documents.get(i);
                if (document == null) {
                    LOG.warnf("[listPolicies] 跳过空文档: tenant=%s index=%d", tenant, i);
                    continue;
                }
                try {
                    policies.add(convertToGraphQLPolicy(document));
                } catch (Exception e) {
                    LOG.warnf(e, "[listPolicies] 转换策略失败, 已跳过: tenant=%s id=%s name=%s", tenant,
                        safe(document.getId()), safe(document.getName()));
                }
            }
            return policies;
        });
    }

    @Mutation("createPolicy")
   @Description("创建策略 / Create policy")
   public Uni<PolicyTypes.Policy> createPolicy(
           @NonNull @Description("策略输入 / Policy payload")
           PolicyTypes.PolicyInput input
   ) {
        final String tenant = tenantId();
        return Uni.createFrom().item(() ->
            policyStorageService.createPolicy(
                tenant,
                convertToPolicyDocument(null, input)
            )
        ).call(created -> policyEvaluationService.invalidateCache(tenant, null, null))
         .onItem().transform(this::convertToGraphQLPolicy);
    }

    @Mutation("updatePolicy")
    @Description("更新策略 / Update policy")
    public Uni<PolicyTypes.Policy> updatePolicy(
            @NonNull @Description("策略ID / Policy identifier")
            String id,
            @NonNull @Description("策略输入 / Policy payload")
            PolicyTypes.PolicyInput input
    ) {
        final String tenant = tenantId();
        return Uni.createFrom().item(() ->
            policyStorageService.updatePolicy(
                tenant,
                id,
                convertToPolicyDocument(id, input)
            )
        ).call(optional -> optional.isPresent()
            ? policyEvaluationService.invalidateCache(tenant, null, null)
            : Uni.createFrom().voidItem())
         .onItem().transform(optional -> optional.map(this::convertToGraphQLPolicy).orElse(null));
    }

    @Mutation("deletePolicy")
    @Description("删除策略 / Delete policy")
    public Uni<Boolean> deletePolicy(
            @NonNull @Description("策略ID / Policy identifier")
            String id
    ) {
        final String tenant = tenantId();
        return Uni.createFrom().item(() -> policyStorageService.deletePolicy(tenant, id))
            .call(deleted -> Boolean.TRUE.equals(deleted)
                ? policyEvaluationService.invalidateCache(tenant, null, null)
                : Uni.createFrom().voidItem());
    }

    private PolicyStorageService.PolicyDocument convertToPolicyDocument(String id, PolicyTypes.PolicyInput input) {
        if (input == null) {
            throw new IllegalArgumentException("策略输入不能为空");
        }

        String name = Objects.requireNonNull(input.name, "策略名称不能为空");
        Map<String, List<String>> allow = convertRuleSetInput(input.allow);
        Map<String, List<String>> deny = convertRuleSetInput(input.deny);
        String effectiveId = id != null ? id : input.id;
        return new PolicyStorageService.PolicyDocument(effectiveId, name, allow, deny);
    }

    private Map<String, List<String>> convertRuleSetInput(PolicyTypes.PolicyRuleSetInput input) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (input == null || input.rules == null) {
            return result;
        }
        for (PolicyTypes.PolicyRuleInput ruleInput : input.rules) {
            if (ruleInput == null || ruleInput.resourceType == null || ruleInput.resourceType.isBlank()) {
                continue;
            }
            String resourceType = ruleInput.resourceType.trim();
            List<String> patterns = new ArrayList<>();
            if (ruleInput.patterns != null) {
                for (String pattern : ruleInput.patterns) {
                    if (pattern != null && !pattern.trim().isEmpty()) {
                        patterns.add(pattern.trim());
                    }
                }
            }
            result.put(resourceType, patterns);
        }
        return result;
    }

    private PolicyTypes.Policy convertToGraphQLPolicy(PolicyStorageService.PolicyDocument document) {
        String id = document.getId() == null ? "" : document.getId();
        String name = document.getName() == null ? "" : document.getName();
        Map<String, List<String>> allow = document.getAllow() == null ? new LinkedHashMap<>() : document.getAllow();
        Map<String, List<String>> deny = document.getDeny() == null ? new LinkedHashMap<>() : document.getDeny();
        return new PolicyTypes.Policy(id, name, convertToGraphQLRuleSet(allow), convertToGraphQLRuleSet(deny));
    }

    private PolicyTypes.PolicyRuleSet convertToGraphQLRuleSet(Map<String, List<String>> rules) {
        List<PolicyTypes.PolicyRule> gqlRules = new ArrayList<>();
        if (rules != null) {
            for (Map.Entry<String, List<String>> entry : rules.entrySet()) {
                String resourceType = entry.getKey() == null ? "" : entry.getKey();
                List<String> patterns = entry.getValue() == null ? new ArrayList<>() : entry.getValue();
                gqlRules.add(new PolicyTypes.PolicyRule(resourceType, patterns));
            }
        }
        return new PolicyTypes.PolicyRuleSet(gqlRules);
    }

    private Object[] buildContext(String tenant, Object... args) {
        String normalizedTenant = tenant == null || tenant.isBlank() ? "default" : tenant.trim();
        Object[] context = new Object[args.length + 1];
        context[0] = normalizedTenant;
        System.arraycopy(args, 0, context, 1, args.length);
        return context;
    }

    private String sanitize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildInvalidateMessage(String tenant, String module, String function) {
        String normalizedTenant = tenant == null || tenant.isBlank() ? "default" : tenant.trim();
        if (module == null && function == null) {
            return "Cache invalidated for tenant " + normalizedTenant + " (all entries)";
        }
        if (module != null && function == null) {
            return "Cache invalidated for tenant " + normalizedTenant + " module " + module;
        }
        if (module == null) {
            return "Cache invalidated for tenant " + normalizedTenant + " function " + function;
        }
        return "Cache invalidated for tenant " + normalizedTenant + " policy " + module + "." + function;
    }

    private static String safe(Object v) { return v == null ? "<null>" : String.valueOf(v); }

    // ==================== Cache Management Mutations ====================

    @Mutation("clearAllCache")
    @Description("清空所有策略缓存 / Clear all policy cache")
    public Uni<CacheOperationResult> clearAllCache() {
        return policyEvaluationService.clearAllCache()
            .onItem().transform(v -> new CacheOperationResult(
                true,
                "All policy cache cleared successfully",
                System.currentTimeMillis()
            ));
    }

    @Mutation("invalidateCache")
    @Description("使特定策略的缓存失效 / Invalidate cache for specific policy")
    public Uni<CacheOperationResult> invalidateCache(
            @Description("策略模块名称 / Policy module name")
            String policyModule,

            @Description("策略函数名称 / Policy function name")
            String policyFunction
    ) {
        final String tenant = tenantId();
        String normalizedModule = sanitize(policyModule);
        String normalizedFunction = sanitize(policyFunction);

        return policyEvaluationService.invalidateCache(tenant, normalizedModule, normalizedFunction)
            .onItem().transform(v -> new CacheOperationResult(
                true,
                buildInvalidateMessage(tenant, normalizedModule, normalizedFunction),
                System.currentTimeMillis()
            ));
    }

    // ==================== Support Types ====================

    public static class CacheOperationResult {
        @Description("操作是否成功 / Operation success status")
        public Boolean success;

        @Description("操作结果消息 / Operation message")
        public String message;

        @Description("操作时间戳 / Operation timestamp")
        public Long timestamp;

        public CacheOperationResult() {}

        public CacheOperationResult(Boolean success, String message, Long timestamp) {
            this.success = success;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
