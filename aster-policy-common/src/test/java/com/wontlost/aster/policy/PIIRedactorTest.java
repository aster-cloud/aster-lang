package com.wontlost.aster.policy;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PIIRedactorTest {
    private final PIIRedactor redactor = new PIIRedactor();

    @Test
    void shouldRedactSSN() {
        String message = "Customer SSN: 123-45-6789";
        String redacted = redactor.redact(message);

        assertThat(redacted).isEqualTo("Customer SSN: ***-**-****");
        assertThat(redacted).doesNotContain("123-45-6789");
    }

    @Test
    void shouldRedactSSNWithoutDashes() {
        String message = "SSN: 123456789";
        String redacted = redactor.redact(message);

        assertThat(redacted).isEqualTo("SSN: ***-**-****");
    }

    @Test
    void shouldRedactEmail() {
        String message = "Contact: john.doe@example.com";
        String redacted = redactor.redact(message);

        assertThat(redacted).isEqualTo("Contact: ***@***.***");
        assertThat(redacted).doesNotContain("john.doe@example.com");
    }

    @Test
    void shouldRedactPhoneNumbers() {
        assertThat(redactor.redact("Call (123) 456-7890"))
            .isEqualTo("Call (***) ***-****");

        assertThat(redactor.redact("Phone: 123-456-7890"))
            .isEqualTo("Phone: (***) ***-****");

        assertThat(redactor.redact("Mobile: 1234567890"))
            .isEqualTo("Mobile: (***) ***-****");
    }

    @Test
    void shouldRedactCreditCardNumbers() {
        String message = "Card: 1234-5678-9012-3456";
        String redacted = redactor.redact(message);

        assertThat(redacted).isEqualTo("Card: ****-****-****-****");
    }

    @Test
    void shouldRedactIPAddresses() {
        String message = "Source IP: 192.168.1.100";
        String redacted = redactor.redact(message);

        assertThat(redacted).isEqualTo("Source IP: ***.***.***.***");
    }

    @Test
    void shouldRedactMultiplePIITypes() {
        String message = "User john@example.com with SSN 123-45-6789 called from (555) 123-4567";
        String redacted = redactor.redact(message);

        assertThat(redacted)
            .contains("***@***.***")
            .contains("***-**-****")
            .contains("(***) ***-****")
            .doesNotContain("john@example.com")
            .doesNotContain("123-45-6789")
            .doesNotContain("(555) 123-4567");
    }

    @Test
    void shouldDetectPII() {
        assertThat(redactor.containsPII("SSN: 123-45-6789")).isTrue();
        assertThat(redactor.containsPII("Email: test@example.com")).isTrue();
        assertThat(redactor.containsPII("No sensitive data here")).isFalse();
    }

    @Test
    void shouldHandleNullAndEmpty() {
        assertThat(redactor.redact(null)).isNull();
        assertThat(redactor.redact("")).isEmpty();
        assertThat(redactor.containsPII(null)).isFalse();
        assertThat(redactor.containsPII("")).isFalse();
    }

    @Test
    void shouldSupportSelectiveRedaction() {
        PIIRedactor emailOnly = new PIIRedactor.Builder()
            .redactSSN(false)
            .redactEmail(true)
            .redactPhone(false)
            .redactCreditCard(false)
            .redactIPAddress(false)
            .build();

        String message = "SSN: 123-45-6789, Email: test@example.com";
        String redacted = emailOnly.redact(message);

        assertThat(redacted)
            .contains("123-45-6789")  // SSN not redacted
            .contains("***@***.***");  // Email redacted
    }

    @Test
    void shouldPreserveNonPIIContent() {
        String message = "Transaction ID: ABC-123, Amount: $500.00, Status: approved";
        String redacted = redactor.redact(message);

        assertThat(redacted).isEqualTo(message);
    }
}
