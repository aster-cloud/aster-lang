package com.wontlost.aster.finance.policies;

import com.wontlost.aster.finance.dto.creditcard.ApplicantInfo;
import com.wontlost.aster.finance.dto.creditcard.ApprovalDecision;
import com.wontlost.aster.finance.dto.creditcard.CreditCardOffer;
import com.wontlost.aster.finance.dto.creditcard.FinancialHistory;
import com.wontlost.aster.finance.dto.creditcard.IncomeValidation;
import com.wontlost.aster.finance.dto.creditcard.RiskScore;

import java.util.Objects;

/**
 * 信用卡策略引擎：嚴格依照 creditcard.aster 定義的 DSL 規則完成審批
 */
public class CreditCardPolicyEngine {

    private static final int MIN_APPROVAL_AGE = 21;
    private static final int MIN_CREDIT_SCORE = 550;

    public ApprovalDecision evaluateCreditCardApplication(
        ApplicantInfo applicant,
        FinancialHistory history,
        CreditCardOffer offer
    ) {
        Objects.requireNonNull(applicant, "ApplicantInfo 不能為 null");
        Objects.requireNonNull(history, "FinancialHistory 不能為 null");
        Objects.requireNonNull(offer, "CreditCardOffer 不能為 null");

        if (history.bankruptcyCount() != 0) {
            return reject("Bankruptcy on record");
        }
        if (applicant.age() < MIN_APPROVAL_AGE) {
            return reject("Age below 21");
        }
        if (applicant.creditScore() < MIN_CREDIT_SCORE) {
            return reject("Credit score too low");
        }

        IncomeValidation income = validateIncomeRequirements(applicant, offer.requestedLimit());
        if (!income.sufficient()) {
            return reject(income.recommendation());
        }

        RiskScore riskScore = calculateComprehensiveRiskScore(applicant, history);
        int employmentScore = assessEmploymentStability(applicant);
        int finalLimit = determineFinalCreditLimit(
            offer.requestedLimit(),
            applicant,
            history,
            riskScore.score(),
            employmentScore
        );
        int apr = calculateApr(applicant.creditScore(), riskScore.score(), history);
        boolean requiresDeposit = shouldRequireSecuredCard(applicant, history, riskScore.score());
        int deposit = calculateRequiredDeposit(requiresDeposit, finalLimit);
        int monthlyFee = determineMonthlyFee(offer.productType(), requiresDeposit, applicant.creditScore());

        return new ApprovalDecision(
            true,
            "Approved",
            finalLimit,
            apr,
            monthlyFee,
            finalLimit,
            requiresDeposit,
            deposit
        );
    }

    private ApprovalDecision reject(String reason) {
        return new ApprovalDecision(false, reason, 0, 0, 0, 0, false, 0);
    }

    RiskScore calculateComprehensiveRiskScore(ApplicantInfo applicant, FinancialHistory history) {
        int creditScorePoints = calculateCreditScorePoints(applicant.creditScore());
        int historyPoints = calculateHistoryPoints(history);
        int utilizationPenalty = calculateUtilizationPenalty(history.utilization());
        int inquiryPenalty = calculateInquiryPenalty(history.hardInquiries());
        int totalScore = creditScorePoints - historyPoints - utilizationPenalty - inquiryPenalty;

        if (totalScore > 800) {
            return new RiskScore(totalScore, "Excellent", "Strong profile");
        }
        if (totalScore > 700) {
            return new RiskScore(totalScore, "Good", "Solid credit");
        }
        if (totalScore > 600) {
            return new RiskScore(totalScore, "Fair", "Some concerns");
        }
        return new RiskScore(totalScore, "Poor", "High risk");
    }

    private int calculateCreditScorePoints(int creditScore) {
        if (creditScore > 780) {
            return 300;
        }
        if (creditScore > 720) {
            return 250;
        }
        if (creditScore > 670) {
            return 200;
        }
        if (creditScore > 620) {
            return 150;
        }
        return 100;
    }

    private int calculateHistoryPoints(FinancialHistory history) {
        int ageBonus = history.accountAge() * 10;
        int latePenalty = history.latePayments() * 15;
        return ageBonus - latePenalty;
    }

    private int calculateUtilizationPenalty(int utilization) {
        if (utilization > 80) {
            return 100;
        }
        if (utilization > 50) {
            return 50;
        }
        if (utilization > 30) {
            return 20;
        }
        return 0;
    }

    private int calculateInquiryPenalty(int inquiries) {
        if (inquiries > 6) {
            return 80;
        }
        if (inquiries > 3) {
            return 40;
        }
        return 0;
    }

    IncomeValidation validateIncomeRequirements(ApplicantInfo applicant, int requestedLimit) {
        int monthlyIncome = applicant.annualIncome() / 12;
        int obligations = applicant.monthlyRent() + requestedLimit / 10;
        int ratio = monthlyIncome == 0 ? 100 : (int) Math.ceil((obligations * 100.0) / monthlyIncome);

        if (ratio >= 50) {
            return new IncomeValidation(false, ratio, "Debt-to-income ratio too high");
        }
        if (applicant.annualIncome() < 15_000) {
            return new IncomeValidation(false, ratio, "Annual income below minimum");
        }
        return new IncomeValidation(true, ratio, "Income requirements met");
    }

    int assessEmploymentStability(ApplicantInfo applicant) {
        String status = applicant.employmentStatus();
        int years = applicant.yearsAtCurrentJob();

        if ("Unemployed".equalsIgnoreCase(status)) {
            return 0;
        }
        if ("Self-employed".equalsIgnoreCase(status)) {
            return years > 3 ? 60 : 30;
        }
        if ("Full-time".equalsIgnoreCase(status)) {
            if (years > 5) {
                return 100;
            }
            if (years > 2) {
                return 80;
            }
            return 50;
        }
        return 40;
    }

    int determineFinalCreditLimit(
        int requestedLimit,
        ApplicantInfo applicant,
        FinancialHistory history,
        int riskScore,
        int employmentScore
    ) {
        int incomeBasedMax = applicant.annualIncome() / 3;
        int riskBasedMax = riskScore * 50 + employmentScore * 20;
        int preliminary = requestedLimit;

        if (preliminary > incomeBasedMax) {
            preliminary = incomeBasedMax;
        }
        if (preliminary > riskBasedMax) {
            preliminary = riskBasedMax;
        }
        if (history.latePayments() > 5) {
            preliminary = preliminary / 2;
        }
        if (preliminary < 500) {
            return 500;
        }
        if (preliminary > 50_000) {
            return 50_000;
        }
        return preliminary;
    }

    int calculateApr(int creditScore, int riskScore, FinancialHistory history) {
        int baseApr = 2_400;
        if (creditScore > 780) {
            baseApr = 1_299;
        } else if (creditScore >= 721) {
            baseApr = 1_599;
        } else if (creditScore >= 671) {
            baseApr = 1_899;
        } else if (creditScore >= 621) {
            baseApr = 2_199;
        }

        if (riskScore < 600) {
            baseApr += 300;
        }
        if (history.latePayments() > 3) {
            baseApr += 200;
        }
        return baseApr;
    }

    boolean shouldRequireSecuredCard(ApplicantInfo applicant, FinancialHistory history, int riskScore) {
        if (applicant.creditScore() <= 620) {
            return true;
        }
        if (riskScore <= 550) {
            return true;
        }
        if (history.latePayments() >= 8) {
            return true;
        }
        return history.utilization() >= 90;
    }

    private int calculateRequiredDeposit(boolean requiresSecured, int creditLimit) {
        return requiresSecured ? creditLimit : 0;
    }

    private int determineMonthlyFee(String productType, boolean isSecured, int creditScore) {
        if (isSecured) {
            return 0;
        }
        if ("Premium".equalsIgnoreCase(productType)) {
            return 25;
        }
        if ("Standard".equalsIgnoreCase(productType)) {
            return creditScore <= 650 ? 10 : 0;
        }
        return 0;
    }
}
