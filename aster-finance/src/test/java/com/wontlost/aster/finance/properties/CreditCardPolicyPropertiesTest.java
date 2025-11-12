package com.wontlost.aster.finance.properties;

import com.wontlost.aster.finance.dto.creditcard.ApplicantInfo;
import com.wontlost.aster.finance.dto.creditcard.ApprovalDecision;
import com.wontlost.aster.finance.dto.creditcard.CreditCardOffer;
import com.wontlost.aster.finance.dto.creditcard.FinancialHistory;
import com.wontlost.aster.finance.policies.CreditCardPolicyEngine;
import net.jqwik.api.*;
import net.jqwik.api.Combinators;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CreditCardPolicyEngine 屬性測試 —— 覆蓋 DSL 審批關鍵約束
 */
class CreditCardPolicyPropertiesTest {

    private final CreditCardPolicyEngine engine = new CreditCardPolicyEngine();

    @Provide
    Arbitrary<ApplicantInfo> applicants() {
        Arbitrary<String> ids = Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(10);
        Arbitrary<Integer> ages = Arbitraries.integers().between(18, 70);
        Arbitrary<Integer> incomes = Arbitraries.integers().between(12_000, 200_000);
        Arbitrary<Integer> creditScores = Arbitraries.integers().between(300, 850);
        Arbitrary<Integer> cards = Arbitraries.integers().between(0, 10);
        Arbitrary<Integer> rent = Arbitraries.integers().between(200, 4_000);
        Arbitrary<String> employment = Arbitraries.of("Full-time", "Self-employed", "Unemployed", "Part-time");
        Arbitrary<Integer> years = Arbitraries.integers().between(0, 15);

        return Combinators.combine(ids, ages, incomes, creditScores, cards, rent, employment, years)
            .as(ApplicantInfo::new);
    }

    @Provide
    Arbitrary<FinancialHistory> histories() {
        Arbitrary<Integer> bankruptcies = Arbitraries.integers().between(0, 2);
        Arbitrary<Integer> lates = Arbitraries.integers().between(0, 10);
        Arbitrary<Integer> utilization = Arbitraries.integers().between(0, 100);
        Arbitrary<Integer> age = Arbitraries.integers().between(0, 120);
        Arbitrary<Integer> inquiries = Arbitraries.integers().between(0, 10);

        return Combinators.combine(bankruptcies, lates, utilization, age, inquiries)
            .as(FinancialHistory::new);
    }

    @Provide
    Arbitrary<CreditCardOffer> offers() {
        Arbitrary<String> products = Arbitraries.of("Premium", "Standard", "Starter");
        Arbitrary<Integer> limits = Arbitraries.integers().between(500, 60_000);
        Arbitrary<Boolean> rewards = Arbitraries.of(true, false);
        Arbitrary<Integer> fees = Arbitraries.integers().between(0, 500);

        return Combinators.combine(products, limits, rewards, fees)
            .as(CreditCardOffer::new);
    }

    @Property
    void 核准結果必須符合DSL前置條件(
        @ForAll("applicants") ApplicantInfo applicant,
        @ForAll("histories") FinancialHistory history,
        @ForAll("offers") CreditCardOffer offer
    ) {
        ApprovalDecision decision = engine.evaluateCreditCardApplication(applicant, history, offer);
        if (decision.approved()) {
            assertThat(applicant.age()).isGreaterThanOrEqualTo(21);
            assertThat(applicant.creditScore()).isGreaterThanOrEqualTo(550);
            assertThat(history.bankruptcyCount()).isZero();
            assertThat(debtToIncomeRatio(applicant, offer.requestedLimit())).isLessThan(50);
            assertThat(decision.reason()).isEqualTo("Approved");
        }
    }

    @Property
    void 授信額度與押金永遠落在規定區間(
        @ForAll("applicants") ApplicantInfo applicant,
        @ForAll("histories") FinancialHistory history,
        @ForAll("offers") CreditCardOffer offer
    ) {
        ApprovalDecision decision = engine.evaluateCreditCardApplication(applicant, history, offer);
        if (decision.approved()) {
            assertThat(decision.approvedLimit()).isBetween(500, 50_000);
            assertThat(decision.creditLine()).isEqualTo(decision.approvedLimit());
            if (decision.requiresDeposit()) {
                assertThat(decision.depositAmount()).isEqualTo(decision.approvedLimit());
            } else {
                assertThat(decision.depositAmount()).isZero();
            }
        } else {
            assertThat(decision.approvedLimit()).isZero();
            assertThat(decision.depositAmount()).isZero();
        }
    }

    private int debtToIncomeRatio(ApplicantInfo applicant, int requestedLimit) {
        int monthlyIncome = applicant.annualIncome() / 12;
        if (monthlyIncome == 0) {
            return 100;
        }
        int obligations = applicant.monthlyRent() + requestedLimit / 10;
        return (int) Math.ceil((obligations * 100.0) / monthlyIncome);
    }
}
