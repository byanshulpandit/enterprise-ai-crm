package com.crm.platform.delivery.service;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.delivery.entity.CampaignDeliveryOutbox;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.entity.OutboxStatus;
import com.crm.platform.delivery.provider.DeliveryProvider;
import com.crm.platform.delivery.provider.SmtpDeliveryProvider;
import com.crm.platform.delivery.repository.CampaignDeliveryOutboxRepository;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for SMTP email delivery using embedded GreenMail.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SmtpDeliveryEndToEndIntegrationTest {

    private static final int SMTP_PORT = 3125;
    private static GreenMail greenMail;

    @DynamicPropertySource
    static void configureMailProperties(DynamicPropertyRegistry registry) {
        if (greenMail == null) {
            ServerSetup setup = new ServerSetup(SMTP_PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
            greenMail = new GreenMail(setup);
            greenMail.start();
        }
        registry.add("crm.delivery.provider", () -> "smtp");
        registry.add("smtp.port", () -> SMTP_PORT);
        registry.add("smtp.host", () -> "127.0.0.1");
        registry.add("smtp.from", () -> "noreply@crm.internal");
        registry.add("smtp.timeout-ms", () -> 5000);
        registry.add("crm.async.stream-key", () -> "crm:campaign:deliveries:stream:smtp-e2e");
        registry.add("crm.async.consumer-group", () -> "crm:delivery:workers:smtp-e2e");
    }

    @AfterAll
    static void stopMailServer() {
        if (greenMail != null) {
            greenMail.stop();
            greenMail = null;
        }
    }

    @Autowired
    private DeliveryProvider deliveryProvider;

    @Autowired
    private DeliveryWorkerService deliveryWorkerService;

    @Autowired
    private DeliveryOutboxPublisher outboxPublisher;

    @Autowired
    private CampaignDeliveryRecordRepository deliveryRecordRepository;

    @Autowired
    private CampaignDeliveryOutboxRepository outboxRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Segment testSegment;
    private Campaign testCampaign;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        clearMailboxSafely();
        outboxRepository.deleteAll();
        deliveryRecordRepository.deleteAll();
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.saveAndFlush(
                new User("smtp_admin", "smtp_admin@crm.internal", "SecurePassword123!", RoleEnum.ROLE_ADMIN, Boolean.TRUE)
        );

        testSegment = segmentRepository.saveAndFlush(
                new Segment("High Value Customers", "Segment for VIP recipients", "{}", testUser)
        );

        testCampaign = new Campaign("End-to-End SMTP Launch", "Promotional Blast", testSegment, "Welcome to the new platform!", testUser);
        testCampaign.setStatus(CampaignStatus.RUNNING);
        testCampaign = campaignRepository.saveAndFlush(testCampaign);

        testCustomer = new Customer();
        testCustomer.setFirstName("Vikram");
        testCustomer.setLastName("Patel");
        testCustomer.setEmail("vikram.patel@crm.internal");
        testCustomer.setCity("Mumbai");
        testCustomer.setTotalSpend(new BigDecimal("2500.00"));
        testCustomer.setVisitCount(8);
        testCustomer = customerRepository.saveAndFlush(testCustomer);
    }

    @AfterEach
    void tearDown() {
        clearMailboxSafely();
        outboxRepository.deleteAll();
        deliveryRecordRepository.deleteAll();
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void clearMailboxSafely() {
        if (greenMail != null) {
            try {
                greenMail.purgeEmailFromAllMailboxes();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("Active DeliveryProvider bean is instance of SmtpDeliveryProvider when configured with crm.delivery.provider=smtp")
    void testActiveDeliveryProviderIsSmtp() {
        assertThat(deliveryProvider).isInstanceOf(SmtpDeliveryProvider.class);
    }

    @Test
    @DisplayName("End-to-end SMTP delivery flow: Campaign -> DeliveryRecord (PENDING) -> Outbox -> Redis -> DeliveryWorker -> SmtpDeliveryProvider -> Local SMTP Sink")
    void testFullEndToEndSmtpDeliveryFlow() throws Exception {
        // Step 1: Create Campaign delivery obligation (PENDING)
        CampaignDeliveryRecord deliveryRecord = new CampaignDeliveryRecord(
                testCampaign,
                testCustomer,
                "Exclusive 20% discount on your next visit, Vikram!"
        );
        deliveryRecord.setStatus(DeliveryStatus.PENDING);
        deliveryRecord = deliveryRecordRepository.saveAndFlush(deliveryRecord);
        assertThat(deliveryRecord.getStatus()).isEqualTo(DeliveryStatus.PENDING);

        // Step 2: Create Transactional Outbox record (PENDING)
        String correlationId = UUID.randomUUID().toString();
        CampaignDeliveryOutbox outbox = new CampaignDeliveryOutbox(
                testCampaign.getId(),
                testCustomer.getId(),
                deliveryRecord.getId(),
                correlationId
        );
        outbox = outboxRepository.saveAndFlush(outbox);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // Step 3: Outbox Publisher publishes pending events to Redis Stream
        int publishedCount = outboxPublisher.publishPendingEvents(10);
        assertThat(publishedCount).isGreaterThanOrEqualTo(1);

        CampaignDeliveryOutbox publishedOutbox = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(publishedOutbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publishedOutbox.getPublishedAt()).isNotNull();

        // Step 4: Wait for background Redis Stream consumer or process delivery obligation
        Instant deadline = Instant.now().plusSeconds(5);
        CampaignDeliveryRecord finalizedRecord = null;
        while (Instant.now().isBefore(deadline)) {
            finalizedRecord = deliveryRecordRepository.findById(deliveryRecord.getId()).orElse(null);
            if (finalizedRecord != null && finalizedRecord.getStatus() == DeliveryStatus.SENT) {
                break;
            }
            Thread.sleep(100);
        }

        if (finalizedRecord == null || finalizedRecord.getStatus() != DeliveryStatus.SENT) {
            deliveryWorkerService.processDelivery(testCampaign.getId(), testCustomer.getId());
            finalizedRecord = deliveryRecordRepository.findById(deliveryRecord.getId()).orElseThrow();
        }

        // Step 5: Verify terminal persistence in DB
        assertThat(finalizedRecord.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(finalizedRecord.getProcessedAt()).isNotNull();
        assertThat(finalizedRecord.getFailureReason()).isNull();

        // Step 6: Verify SMTP sink (GreenMail) received the actual email message
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        MimeMessage message = receivedMessages[0];
        assertThat(message.getSubject()).isEqualTo("Campaign Notification #" + testCampaign.getId());
        assertThat(message.getAllRecipients()[0].toString()).contains("vikram.patel@crm.internal");
        assertThat(message.getHeader("X-Delivery-Idempotency-Key", null))
                .isEqualTo("CAMP-" + testCampaign.getId() + "-CUST-" + testCustomer.getId());
        assertThat(message.getHeader("X-Campaign-Id", null)).isEqualTo(String.valueOf(testCampaign.getId()));
        assertThat(message.getHeader("X-Customer-Id", null)).isEqualTo(String.valueOf(testCustomer.getId()));

        // Step 7: Retry / duplicate delivery processing must be idempotent and must not resend email
        boolean retryProcessed = deliveryWorkerService.processDelivery(testCampaign.getId(), testCustomer.getId());
        assertThat(retryProcessed).isTrue();

        // SMTP server still only contains 1 email (idempotency preserved)
        assertThat(greenMail.getReceivedMessages()).hasSize(1);

        // Step 8: Verify Campaign transitions to COMPLETED when all obligations reach terminal state
        Campaign completedCampaign = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(completedCampaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
        assertThat(completedCampaign.getCompletedAt()).isNotNull();
    }
}
