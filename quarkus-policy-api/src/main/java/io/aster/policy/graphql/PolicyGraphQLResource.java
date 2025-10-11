package io.aster.policy.graphql;

import io.aster.policy.api.PolicyEvaluationService;
import io.aster.policy.graphql.types.AutoInsuranceTypes;
import io.aster.policy.graphql.types.CreditCardTypes;
import io.aster.policy.graphql.types.EnterpriseLendingTypes;
import io.aster.policy.graphql.types.HealthcareTypes;
import io.aster.policy.graphql.types.LifeInsuranceTypes;
import io.aster.policy.graphql.types.LoanTypes;
import io.aster.policy.graphql.types.PersonalLendingTypes;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
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
        return policyEvaluationService.evaluatePolicy(
                "aster.finance.creditcard",
                "evaluateCreditCardApplication",
                new Object[]{
                    convertToAsterCreditCardApplicant(applicant),
                    convertToAsterFinancialHistory(history),
                    convertToAsterCreditCardOffer(offer)
                }
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
        return policyEvaluationService.evaluatePolicy(
                "aster.finance.enterprise_lending",
                "evaluateEnterpriseLoan",
                new Object[]{
                    convertToAsterEnterpriseInfo(enterprise),
                    convertToAsterFinancialPosition(position),
                    convertToAsterBusinessHistory(history),
                    convertToAsterEnterpriseLoanApplication(application)
                }
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
        return policyEvaluationService.evaluatePolicy(
                "aster.finance.personal_lending",
                "evaluatePersonalLoan",
                new Object[]{
                    convertToAsterPersonalInfo(personal),
                    convertToAsterIncomeProfile(income),
                    convertToAsterCreditProfile(credit),
                    convertToAsterDebtProfile(debt),
                    convertToAsterPersonalLoanRequest(request)
                }
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
            "monthlyMortgage", debt.monthlyMortgage,
            "monthlyCarPayment", debt.monthlyCarPayment,
            "monthlyStudentLoan", debt.monthlyStudentLoan,
            "monthlyCreditCardPayment", debt.monthlyCreditCardPayment,
            "otherMonthlyDebt", debt.otherMonthlyDebt,
            "totalOutstandingDebt", debt.totalOutstandingDebt
        );
    }

    private java.util.Map<String, Object> convertToAsterPersonalLoanRequest(PersonalLendingTypes.LoanRequest request) {
        return java.util.Map.of(
            "requestedAmount", request.requestedAmount,
            "loanPurpose", request.loanPurpose,
            "desiredTermMonths", request.desiredTermMonths,
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
            @NonNull @Description("策略模块名称 / Policy module name")
            String policyModule,

            @NonNull @Description("策略函数名称 / Policy function name")
            String policyFunction
    ) {
        return policyEvaluationService.invalidateCache(policyModule, policyFunction, new Object[0])
            .onItem().transform(v -> new CacheOperationResult(
                true,
                "Cache invalidated for " + policyModule + "." + policyFunction,
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
