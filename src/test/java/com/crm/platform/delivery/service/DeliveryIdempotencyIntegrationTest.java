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
import com.crm.platform.delivery.provider.DeliveryRequest;
import com.crm.platform.delivery.provider.DeliveryResult;
import com.crm.platform.delivery.repository.CampaignDeliveryOutboxRepository;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class DeliveryIdempotencyIntegrationTest {

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

    @MockBean
    private DeliveryProvider deliveryProvider;

    private Campaign testCampaign;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        deliveryRecordRepository.deleteAll();
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.saveAndFlush(
                new User("idem_user", "idem@crm.internal", "pass", RoleEnum.ROLE_ADMIN, Boolean.TRUE)
        );

        Segment segment = segmentRepository.saveAndFlush(
                new Segment("VIP Seg", "desc", "{}", user)
        );

        testCampaign = new Campaign("Idempotency Campaign", "desc", segment, "Msg", user);
        testCampaign.setStatus(CampaignStatus.RUNNING);
        testCampaign = campaignRepository.saveAndFlush(testCampaign);

        testCustomer = new Customer();
        testCustomer.setFirstName("Pooja");
        testCustomer.setLastName("Sharma");
        testCustomer.setEmail("pooja@crm.internal");
        testCustomer.setCity("Pune");
        testCustomer.setTotalSpend(new BigDecimal("100.00"));
        testCustomer.setVisitCount(1);
        testCustomer = customerRepository.saveAndFlush(testCustomer);
    }

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        deliveryRecordRepository.deleteAll();
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("First delivery executes provider; second retry skips idempotently without provider call")
    void testDelivery_FirstExecution_AndSubsequentRetryIdempotency() {
        CampaignDeliveryRecord record = new CampaignDeliveryRecord(testCampaign, testCustomer, "Hello Pooja");
        record.setStatus(DeliveryStatus.PENDING);
        record = deliveryRecordRepository.saveAndFlush(record);

        when(deliveryProvider.send(any(DeliveryRequest.class)))
                .thenReturn(DeliveryResult.success("sim-msg-123"));

        // First delivery processing
        boolean firstRun = deliveryWorkerService.processDelivery(testCampaign.getId(), testCustomer.getId());
        assertThat(firstRun).isTrue();

        CampaignDeliveryRecord postFirst = deliveryRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(postFirst.getStatus()).isEqualTo(DeliveryStatus.SENT);
        verify(deliveryProvider, times(1)).send(any(DeliveryRequest.class));

        // Retry same delivery - should detect terminal status SENT and skip
        boolean secondRun = deliveryWorkerService.processDelivery(testCampaign.getId(), testCustomer.getId());
        assertThat(secondRun).isTrue();

        // Provider was NOT called a second time
        verify(deliveryProvider, times(1)).send(any(DeliveryRequest.class));
        CampaignDeliveryRecord postSecond = deliveryRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(postSecond.getStatus()).isEqualTo(DeliveryStatus.SENT);
    }

    @Test
    @DisplayName("Outbox entry publication retry recovers after transient Redis failure")
    void testOutbox_RetryRecoversAfterFailure() {
        CampaignDeliveryRecord record = new CampaignDeliveryRecord(testCampaign, testCustomer, "Retry Msg");
        record = deliveryRecordRepository.saveAndFlush(record);

        CampaignDeliveryOutbox outbox = new CampaignDeliveryOutbox(
                testCampaign.getId(),
                testCustomer.getId(),
                record.getId(),
                "corr-test-123"
        );
        outbox = outboxRepository.saveAndFlush(outbox);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // Outbox publisher publishes pending events
        int published = outboxPublisher.publishPendingEvents(10);
        assertThat(published).isGreaterThanOrEqualTo(1);

        CampaignDeliveryOutbox updated = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
    }
}
