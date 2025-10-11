package io.aster.policy.graphql;

import io.aster.policy.api.PolicyEvaluationService;
import io.aster.policy.graphql.types.AutoInsuranceTypes;
import io.aster.policy.graphql.types.HealthcareTypes;
import io.aster.policy.graphql.types.LifeInsuranceTypes;
import io.aster.policy.graphql.types.LoanTypes;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
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

    @Inject
    PolicyEvaluationService policyEvaluationService;

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
        return policyEvaluationService.evaluatePolicy(
                "aster.insurance.life",
                "generateLifeQuote",
                new Object[]{
                    convertToAsterApplicant(applicant),
                    convertToAsterPolicyRequest(request)
                }
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
        return policyEvaluationService.evaluatePolicy(
                "aster.insurance.life",
                "calculateRiskScore",
                new Object[]{convertToAsterApplicant(applicant)}
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
        return policyEvaluationService.evaluatePolicy(
                "aster.insurance.auto",
                "generateAutoQuote",
                new Object[]{
                    convertToAsterDriver(driver),
                    convertToAsterVehicle(vehicle),
                    coverageType
                }
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
        return policyEvaluationService.evaluatePolicy(
                "aster.healthcare.eligibility",
                "checkServiceEligibility",
                new Object[]{
                    convertToAsterPatient(patient),
                    convertToAsterService(service)
                }
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

            @NonNull @Description("患者信息 / Patient information")
            HealthcareTypes.Patient patient
    ) {
        return policyEvaluationService.evaluatePolicy(
                "aster.healthcare.claims",
                "processClaim",
                new Object[]{
                    convertToAsterClaim(claim),
                    convertToAsterProvider(provider),
                    convertToAsterPatient(patient)
                }
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
        return policyEvaluationService.evaluatePolicy(
                "aster.finance.loan",
                "evaluateLoanEligibility",
                new Object[]{
                    convertToAsterLoanApplication(application),
                    convertToAsterLoanApplicant(applicant)
                }
            )
            .onItem().transform(result -> convertToGraphQLLoanDecision(result.getResult()));
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
            "hasChronicConditions", patient.hasChronicConditions,
            "accountBalance", patient.accountBalance
        );
    }

    private java.util.Map<String, Object> convertToAsterService(HealthcareTypes.Service service) {
        return java.util.Map.of(
            "serviceCode", service.serviceCode,
            "cost", service.cost,
            "isEssential", service.isEssential
        );
    }

    private java.util.Map<String, Object> convertToAsterClaim(HealthcareTypes.Claim claim) {
        return java.util.Map.of(
            "claimId", claim.claimId,
            "claimAmount", claim.claimAmount,
            "serviceDate", claim.serviceDate,
            "specialtyType", claim.specialtyType,
            "diagnosisCode", claim.diagnosisCode,
            "hasDocumentation", claim.hasDocumentation
        );
    }

    private java.util.Map<String, Object> convertToAsterProvider(HealthcareTypes.Provider provider) {
        return java.util.Map.of(
            "providerId", provider.providerId,
            "inNetwork", provider.inNetwork,
            "qualityScore", provider.qualityScore
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
            Integer coveragePercentage = (Integer) resultClass.getField("coveragePercentage").get(asterResult);
            Integer patientCost = (Integer) resultClass.getField("patientCost").get(asterResult);

            return new HealthcareTypes.EligibilityCheck(eligible, reason, coveragePercentage, patientCost);
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

            return new HealthcareTypes.ClaimDecision(approved, reason, approvedAmount, requiresReview);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Aster result to GraphQL type", e);
        }
    }

    private java.util.Map<String, Object> convertToAsterLoanApplication(LoanTypes.Application application) {
        return java.util.Map.of(
            "applicantId", application.loanId,
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
            "monthlyDebt", applicant.existingDebtMonthly
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
}
