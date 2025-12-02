package com.wontlost.aster.finance.dto;

import com.wontlost.aster.finance.dto.creditcard.ApplicantInfo;
import com.wontlost.aster.finance.dto.creditcard.ApprovalDecision;
import com.wontlost.aster.finance.dto.creditcard.CreditCardOffer;
import com.wontlost.aster.finance.dto.creditcard.FinancialHistory;
import com.wontlost.aster.finance.dto.creditcard.IncomeValidation;
import com.wontlost.aster.finance.dto.creditcard.RiskScore;
import com.wontlost.aster.finance.dto.enterprise_lending.BusinessHistory;
import com.wontlost.aster.finance.dto.enterprise_lending.EnterpriseInfo;
import com.wontlost.aster.finance.dto.enterprise_lending.FinancialPosition;
import com.wontlost.aster.finance.dto.enterprise_lending.LendingDecision;
import com.wontlost.aster.finance.dto.enterprise_lending.LeverageAnalysis;
import com.wontlost.aster.finance.dto.enterprise_lending.LiquidityAnalysis;
import com.wontlost.aster.finance.dto.enterprise_lending.LoanApplication;
import com.wontlost.aster.finance.dto.enterprise_lending.ProfitabilityAnalysis;
import com.wontlost.aster.finance.dto.fraud.AccountHistory;
import com.wontlost.aster.finance.dto.fraud.FraudResult;
import com.wontlost.aster.finance.dto.fraud.Transaction;
import com.wontlost.aster.finance.dto.loan.ApplicantProfile;
import com.wontlost.aster.finance.dto.loan.LoanDecision;
import com.wontlost.aster.finance.dto.personal_lending.AffordabilityAnalysis;
import com.wontlost.aster.finance.dto.personal_lending.CreditAssessment;
import com.wontlost.aster.finance.dto.personal_lending.CreditProfile;
import com.wontlost.aster.finance.dto.personal_lending.DebtProfile;
import com.wontlost.aster.finance.dto.personal_lending.IncomeAssessment;
import com.wontlost.aster.finance.dto.personal_lending.IncomeProfile;
import com.wontlost.aster.finance.dto.personal_lending.LoanRequest;
import com.wontlost.aster.finance.dto.personal_lending.PersonalInfo;
import com.wontlost.aster.finance.dto.risk.FinancialProfile;
import com.wontlost.aster.finance.dto.risk.RiskAssessment;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一校验自动生成 DTO 的值语义，确保 equals/hashCode/toString 按记录默认语义工作。
 */
class DtoTest {

    private static final List<Class<?>> DTO_CLASSES = List.of(
        ApplicantInfo.class,
        ApprovalDecision.class,
        CreditCardOffer.class,
        FinancialHistory.class,
        IncomeValidation.class,
        RiskScore.class,
        AccountHistory.class,
        FraudResult.class,
        Transaction.class,
        com.wontlost.aster.finance.dto.loan.LoanApplication.class,
        ApplicantProfile.class,
        LoanDecision.class,
        BusinessHistory.class,
        EnterpriseInfo.class,
        FinancialPosition.class,
        LendingDecision.class,
        LeverageAnalysis.class,
        LiquidityAnalysis.class,
        ProfitabilityAnalysis.class,
        com.wontlost.aster.finance.dto.enterprise_lending.LoanApplication.class,
        AffordabilityAnalysis.class,
        CreditAssessment.class,
        CreditProfile.class,
        DebtProfile.class,
        IncomeAssessment.class,
        IncomeProfile.class,
        com.wontlost.aster.finance.dto.personal_lending.LoanDecision.class,
        LoanRequest.class,
        PersonalInfo.class,
        FinancialProfile.class,
        RiskAssessment.class
    );

    @Test
    void dtoRecordsShouldExposeStableValueSemantics() throws ReflectiveOperationException {
        for (Class<?> dtoClass : DTO_CLASSES) {
            assertThat(dtoClass.isRecord())
                .as("DTO %s 必须是 Java Record", dtoClass.getName())
                .isTrue();
            RecordComponent[] components = dtoClass.getRecordComponents();
            Object[] baseArgs = buildArgs(components);
            Object[] identicalArgs = baseArgs.clone();
            Object[] differentArgs = baseArgs.clone();
            mutateFirst(components, differentArgs);

            Object first = instantiate(dtoClass, baseArgs);
            Object second = instantiate(dtoClass, identicalArgs);

            assertThat(first)
                .as("DTO %s 应实现基于字段的值语义", dtoClass.getSimpleName())
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
            assertThat(first.toString()).contains(dtoClass.getSimpleName());

            if (components.length > 0) {
                Object mutated = instantiate(dtoClass, differentArgs);
                assertThat(first).isNotEqualTo(mutated);
            }
        }
    }

    private Object instantiate(Class<?> dtoClass, Object[] args) throws ReflectiveOperationException {
        for (Constructor<?> ctor : dtoClass.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == args.length) {
                ctor.setAccessible(true);
                return ctor.newInstance(args);
            }
        }
        throw new IllegalStateException("未找到匹配构造方法：" + dtoClass.getName());
    }

    private Object[] buildArgs(RecordComponent[] components) {
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            values[i] = sampleValue(components[i].getType(), components[i].getName(), i);
        }
        return values;
    }

    private void mutateFirst(RecordComponent[] components, Object[] args) {
        if (components.length == 0) {
            return;
        }
        args[0] = alternateValue(components[0].getType(), args[0]);
    }

    private Object sampleValue(Class<?> type, String name, int index) {
        if (type == String.class) {
            return name.toUpperCase() + "-VAL-" + index;
        }
        if (type == boolean.class || type == Boolean.class) {
            return index % 2 == 0;
        }
        if (type == int.class || type == Integer.class) {
            return 100 + index;
        }
        if (type == long.class || type == Long.class) {
            return 1_000L + index;
        }
        if (type == double.class || type == Double.class) {
            return 1.0 + index;
        }
        if (type == float.class || type == Float.class) {
            return 1.0f + index;
        }
        if (type == short.class || type == Short.class) {
            return (short) (10 + index);
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) (5 + index);
        }
        if (type == char.class || type == Character.class) {
            return (char) ('A' + index);
        }
        throw new IllegalArgumentException("不支持的字段类型: " + type);
    }

    private Object alternateValue(Class<?> type, Object current) {
        if (type == String.class) {
            return current + "-DIFF";
        }
        if (type == boolean.class || type == Boolean.class) {
            return !((Boolean) current);
        }
        if (type == int.class || type == Integer.class) {
            return ((Number) current).intValue() + 1;
        }
        if (type == long.class || type == Long.class) {
            return ((Number) current).longValue() + 1;
        }
        if (type == double.class || type == Double.class) {
            return ((Number) current).doubleValue() + 0.5;
        }
        if (type == float.class || type == Float.class) {
            return ((Number) current).floatValue() + 0.5f;
        }
        if (type == short.class || type == Short.class) {
            return (short) (((Number) current).shortValue() + 1);
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) (((Number) current).byteValue() + 1);
        }
        if (type == char.class || type == Character.class) {
            return (char) ((((Character) current)) + 1);
        }
        throw new IllegalArgumentException("不支持的字段类型: " + type);
    }
}
