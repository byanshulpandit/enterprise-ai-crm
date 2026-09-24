package com.crm.platform.delivery.provider;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpDeliveryProviderTest {

    private GreenMail greenMail;
    private SmtpDeliveryProvider provider;
    private int smtpPort;

    @BeforeEach
    void setUp() {
        ServerSetup setup = new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
        greenMail = new GreenMail(setup);
        greenMail.start();
        smtpPort = greenMail.getSmtp().getPort();
        provider = new SmtpDeliveryProvider("127.0.0.1", smtpPort, "", "", "noreply@crm.internal", 5000);
    }

    @AfterEach
    void tearDown() {
        if (provider != null) {
            provider.clearCache();
        }
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    @Test
    @DisplayName("SmtpDeliveryProvider successfully dispatches email to local SMTP sink")
    void testSend_Success() throws Exception {
        DeliveryRequest request = new DeliveryRequest(
                "IDEM-SMTP-001",
                10L,
                20L,
                "customer1@example.com",
                "Hello Customer 1"
        );

        DeliveryResult result = provider.send(request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).startsWith("SMTP-IDEM-SMTP-001");
        assertThat(result.getProcessedAt()).isNotNull();
        assertThat(result.getFailureReason()).isNull();

        // Verify captured email in local SMTP server
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        MimeMessage received = receivedMessages[0];
        assertThat(received.getHeader("X-Delivery-Idempotency-Key", null)).isEqualTo("IDEM-SMTP-001");
        assertThat(received.getHeader("X-Campaign-Id", null)).isEqualTo("10");
        assertThat(received.getHeader("X-Customer-Id", null)).isEqualTo("20");
        assertThat(received.getSubject()).isEqualTo("Campaign Notification #10");
        assertThat(received.getAllRecipients()[0].toString()).contains("customer1@example.com");
    }

    @Test
    @DisplayName("SmtpDeliveryProvider enforces idempotency: second call returns cached result without dispatching duplicate email")
    void testSend_Idempotency() {
        DeliveryRequest request = new DeliveryRequest(
                "IDEM-SMTP-002",
                11L,
                21L,
                "customer2@example.com",
                "Idempotent message"
        );

        DeliveryResult firstResult = provider.send(request);
        assertThat(firstResult.isSuccess()).isTrue();
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        assertThat(provider.hasProcessed("IDEM-SMTP-002")).isTrue();

        // Second send with identical idempotency key
        DeliveryResult secondResult = provider.send(request);
        assertThat(secondResult.isSuccess()).isTrue();
        assertThat(secondResult.getMessageId()).isEqualTo(firstResult.getMessageId());

        // Still only 1 email dispatched to the SMTP sink
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("SmtpDeliveryProvider retry with same idempotency key produces identical result")
    void testSend_RetryIdempotency() {
        DeliveryRequest request = new DeliveryRequest(
                "IDEM-SMTP-RETRY",
                12L,
                22L,
                "customer-retry@example.com",
                "Retry test message"
        );

        DeliveryResult result1 = provider.send(request);
        DeliveryResult result2 = provider.send(request);

        assertThat(result1.getMessageId()).isEqualTo(result2.getMessageId());
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("SmtpDeliveryProvider safely catches and reports failure when SMTP server is unreachable")
    void testSend_ServerUnreachable_ReturnsFailureGracefully() {
        // Unused local port where no SMTP server runs
        SmtpDeliveryProvider unreachableProvider = new SmtpDeliveryProvider(
                "127.0.0.1",
                59999,
                "",
                "",
                "noreply@crm.internal",
                2000
        );

        DeliveryRequest request = new DeliveryRequest(
                "IDEM-FAIL-001",
                99L,
                99L,
                "unreachable@example.com",
                "Fail message"
        );

        DeliveryResult result = unreachableProvider.send(request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isNotNull();
        assertThat(result.getFailureReason()).contains("SMTP_DISPATCH_FAILURE");
        assertThat(unreachableProvider.hasProcessed("IDEM-FAIL-001")).isFalse();
    }

    @Test
    @DisplayName("SmtpDeliveryProvider handles authentication with credentials")
    void testSend_WithAuthentication() {
        greenMail.setUser("testuser", "testpass");

        SmtpDeliveryProvider authProvider = new SmtpDeliveryProvider(
                "127.0.0.1",
                smtpPort,
                "testuser",
                "testpass",
                "auth@crm.internal",
                5000
        );

        DeliveryRequest request = new DeliveryRequest(
                "IDEM-AUTH-001",
                15L,
                25L,
                "auth-recipient@example.com",
                "Authenticated message"
        );

        DeliveryResult result = authProvider.send(request);
        assertThat(result.isSuccess()).isTrue();
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("SmtpDeliveryProvider clearCache purges cached results allowing re-dispatch")
    void testClearCache() {
        DeliveryRequest request = new DeliveryRequest(
                "IDEM-CLEAR-001",
                16L,
                26L,
                "clear@example.com",
                "Cache test message"
        );

        provider.send(request);
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        assertThat(provider.hasProcessed("IDEM-CLEAR-001")).isTrue();

        provider.clearCache();
        assertThat(provider.hasProcessed("IDEM-CLEAR-001")).isFalse();

        // Resending after clearCache sends another message
        provider.send(request);
        assertThat(greenMail.getReceivedMessages()).hasSize(2);
    }

    @Test
    @DisplayName("SmtpDeliveryProvider properties accessors return configured values")
    void testPropertiesGetters() {
        SmtpDeliveryProvider custom = new SmtpDeliveryProvider(
                "mail.example.com",
                2525,
                "user",
                "pass",
                "custom@example.com",
                3000
        );

        assertThat(custom.getHost()).isEqualTo("mail.example.com");
        assertThat(custom.getPort()).isEqualTo(2525);
        assertThat(custom.getFrom()).isEqualTo("custom@example.com");
        assertThat(custom.getTimeoutMs()).isEqualTo(3000);
    }
}
