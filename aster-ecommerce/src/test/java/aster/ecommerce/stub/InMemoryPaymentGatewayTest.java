package aster.ecommerce.stub;

import aster.ecommerce.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * InMemoryPaymentGateway 单元测试
 */
class InMemoryPaymentGatewayTest {

    private InMemoryPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new InMemoryPaymentGateway();
    }

    @Test
    void shouldChargeSuccessfully() throws Exception {
        String orderId = "ORDER-001";
        BigDecimal amount = new BigDecimal("99.99");

        String paymentId = gateway.charge(orderId, amount);

        assertThat(paymentId).isNotNull().startsWith("PAY-");
        assertThat(gateway.getPaymentCount()).isEqualTo(1);
    }

    @Test
    void shouldSupportIdempotency() throws Exception {
        String orderId = "ORDER-001";
        BigDecimal amount = new BigDecimal("99.99");

        String paymentId1 = gateway.charge(orderId, amount);
        String paymentId2 = gateway.charge(orderId, amount);

        assertThat(paymentId1).isEqualTo(paymentId2);
        assertThat(gateway.getPaymentCount()).isEqualTo(1);
    }

    @Test
    void shouldRefundSuccessfully() throws Exception {
        String orderId = "ORDER-001";
        BigDecimal amount = new BigDecimal("99.99");
        String paymentId = gateway.charge(orderId, amount);

        String refundId = gateway.refund(paymentId);

        assertThat(refundId).isNotNull().startsWith("REFUND-");
    }

    @Test
    void shouldFailWhenRefundingNonexistentPayment() {
        assertThatThrownBy(() -> gateway.refund("INVALID-PAYMENT"))
            .isInstanceOf(PaymentGateway.PaymentException.class)
            .hasMessageContaining("支付记录不存在");
    }

    @Test
    void shouldFailWhenRefundingTwice() throws Exception {
        String orderId = "ORDER-001";
        BigDecimal amount = new BigDecimal("99.99");
        String paymentId = gateway.charge(orderId, amount);
        gateway.refund(paymentId);

        assertThatThrownBy(() -> gateway.refund(paymentId))
            .isInstanceOf(PaymentGateway.PaymentException.class)
            .hasMessageContaining("支付已退款");
    }

    @Test
    void shouldAttemptRefundReturnTrueWhenNoPayment() {
        boolean result = gateway.attemptRefund("NONEXISTENT-ORDER");

        assertThat(result).isTrue();
    }

    @Test
    void shouldAttemptRefundReturnTrueWhenRefundSucceeds() throws Exception {
        String orderId = "ORDER-001";
        gateway.charge(orderId, new BigDecimal("99.99"));

        boolean result = gateway.attemptRefund(orderId);

        assertThat(result).isTrue();
    }

    @Test
    void shouldFailWhenAmountIsZero() {
        assertThatThrownBy(() -> gateway.charge("ORDER-001", BigDecimal.ZERO))
            .isInstanceOf(PaymentGateway.PaymentException.class)
            .hasMessageContaining("扣款金额必须大于 0");
    }

    @Test
    void shouldFailWhenAmountIsNegative() {
        assertThatThrownBy(() -> gateway.charge("ORDER-001", new BigDecimal("-10")))
            .isInstanceOf(PaymentGateway.PaymentException.class)
            .hasMessageContaining("扣款金额必须大于 0");
    }
}
