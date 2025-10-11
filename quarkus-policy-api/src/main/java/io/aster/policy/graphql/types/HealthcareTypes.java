package io.aster.policy.graphql.types;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

/**
 * GraphQL类型定义 - 医疗健康保险
 */
public class HealthcareTypes {

    @Type("HealthcarePatient")
    @Description("医疗保险患者信息 / Healthcare patient information")
    public static class Patient {
        @NonNull
        @Description("患者ID / Patient ID")
        public String patientId;

        @NonNull
        @Description("年龄 / Age")
        public Integer age;

        @NonNull
        @Description("保险类型 / Insurance type (Premium/Standard/Basic)")
        public String insuranceType;

        @NonNull
        @Description("是否有慢性病 / Has chronic conditions")
        public Boolean hasChronicConditions;

        @NonNull
        @Description("账户余额 / Account balance")
        public Integer accountBalance;

        public Patient() {}

        public Patient(String patientId, Integer age, String insuranceType,
                      Boolean hasChronicConditions, Integer accountBalance) {
            this.patientId = patientId;
            this.age = age;
            this.insuranceType = insuranceType;
            this.hasChronicConditions = hasChronicConditions;
            this.accountBalance = accountBalance;
        }
    }

    @Type("HealthcareService")
    @Description("医疗服务 / Healthcare service")
    public static class Service {
        @NonNull
        @Description("服务代码 / Service code")
        public String serviceCode;

        @NonNull
        @Description("服务费用 / Service cost")
        public Integer cost;

        @NonNull
        @Description("是否为必需服务 / Is essential service")
        public Boolean isEssential;

        public Service() {}

        public Service(String serviceCode, Integer cost, Boolean isEssential) {
            this.serviceCode = serviceCode;
            this.cost = cost;
            this.isEssential = isEssential;
        }
    }

    @Type("HealthcareEligibilityCheck")
    @Description("医疗保险资格审查结果 / Healthcare eligibility check result")
    public static class EligibilityCheck {
        @NonNull
        @Description("是否符合资格 / Is eligible")
        public Boolean eligible;

        @NonNull
        @Description("原因说明 / Reason")
        public String reason;

        @NonNull
        @Description("覆盖百分比 / Coverage percentage")
        public Integer coveragePercentage;

        @NonNull
        @Description("患者需支付金额 / Patient cost")
        public Integer patientCost;

        public EligibilityCheck() {}

        public EligibilityCheck(Boolean eligible, String reason, Integer coveragePercentage,
                               Integer patientCost) {
            this.eligible = eligible;
            this.reason = reason;
            this.coveragePercentage = coveragePercentage;
            this.patientCost = patientCost;
        }
    }

    @Type("HealthcareClaim")
    @Description("医疗索赔 / Healthcare claim")
    public static class Claim {
        @NonNull
        @Description("索赔ID / Claim ID")
        public String claimId;

        @NonNull
        @Description("索赔金额 / Claim amount")
        public Integer claimAmount;

        @NonNull
        @Description("服务日期 / Service date")
        public String serviceDate;

        @NonNull
        @Description("专科类型 / Specialty type")
        public String specialtyType;

        @NonNull
        @Description("诊断代码 / Diagnosis code")
        public String diagnosisCode;

        @NonNull
        @Description("是否有文档 / Has documentation")
        public Boolean hasDocumentation;

        public Claim() {}

        public Claim(String claimId, Integer claimAmount, String serviceDate,
                    String specialtyType, String diagnosisCode, Boolean hasDocumentation) {
            this.claimId = claimId;
            this.claimAmount = claimAmount;
            this.serviceDate = serviceDate;
            this.specialtyType = specialtyType;
            this.diagnosisCode = diagnosisCode;
            this.hasDocumentation = hasDocumentation;
        }
    }

    @Type("HealthcareProvider")
    @Description("医疗服务提供者 / Healthcare provider")
    public static class Provider {
        @NonNull
        @Description("提供者ID / Provider ID")
        public String providerId;

        @NonNull
        @Description("是否为网络内 / Is in network")
        public Boolean inNetwork;

        @NonNull
        @Description("质量评分 / Quality score (0-100)")
        public Integer qualityScore;

        public Provider() {}

        public Provider(String providerId, Boolean inNetwork, Integer qualityScore) {
            this.providerId = providerId;
            this.inNetwork = inNetwork;
            this.qualityScore = qualityScore;
        }
    }

    @Type("HealthcareClaimDecision")
    @Description("医疗索赔决定 / Healthcare claim decision")
    public static class ClaimDecision {
        @NonNull
        @Description("是否批准 / Is approved")
        public Boolean approved;

        @NonNull
        @Description("原因说明 / Reason")
        public String reason;

        @NonNull
        @Description("批准金额 / Approved amount")
        public Integer approvedAmount;

        @NonNull
        @Description("需要审查 / Requires review")
        public Boolean requiresReview;

        public ClaimDecision() {}

        public ClaimDecision(Boolean approved, String reason, Integer approvedAmount,
                            Boolean requiresReview) {
            this.approved = approved;
            this.reason = reason;
            this.approvedAmount = approvedAmount;
            this.requiresReview = requiresReview;
        }
    }
}
