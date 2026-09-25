package com.crm.platform.campaign;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.campaign.service.CampaignService;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.delivery.entity.CampaignDeliveryOutbox;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CampaignKeysetPaginationIntegrationTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampaignDeliveryRecordRepository deliveryRecordRepository;

    @Autowired
    private CampaignDeliveryOutboxRepository outboxRepository;

    private User testUser;
    private Segment testSegment;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        deliveryRecordRepository.deleteAll();
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.saveAndFlush(
                new User("keyset_admin", "keyset_admin@crm.internal", "secretHash123", RoleEnum.ROLE_ADMIN, Boolean.TRUE)
        );

        String segmentJson = "{\"combinator\":\"AND\",\"conditions\":[{\"field\":\"city\",\"operator\":\"EQUALS\",\"value\":\"Mumbai\"}]}";
        testSegment = segmentRepository.saveAndFlush(
                new Segment("Mumbai Audience", "Segment for Mumbai customers", segmentJson, testUser)
        );
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
    @DisplayName("Keyset pagination query correctly retrieves multiple batches across exact boundaries without duplicates or skips")
    void testKeysetQuery_MultipleBatches_NoDuplicatesOrSkips() {
        // Create 25 customers in Mumbai with sequential IDs
        List<Customer> customers = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Customer c = new Customer();
            c.setFirstName("Cust" + i);
            c.setLastName("Test");
            c.setEmail("cust" + i + "@keyset.test");
            c.setCity("Mumbai");
            c.setTotalSpend(new BigDecimal("100.00"));
            c.setVisitCount(1);
            customers.add(c);
        }
        customers = customerRepository.saveAllAndFlush(customers);

        Specification<Customer> baseSpec = (root, query, cb) -> cb.equal(root.get("city"), "Mumbai");

        // Simulate keyset pagination with batch size = 10
        int batchSize = 10;
        Long lastSeenId = 0L;
        List<Long> retrievedIds = new ArrayList<>();

        while (true) {
            final Long currentLastId = lastSeenId;
            Specification<Customer> batchSpec = baseSpec.and((root, query, cb) -> cb.gt(root.get("id"), currentLastId));
            Page<Customer> page = customerRepository.findAll(
                    batchSpec,
                    PageRequest.of(0, batchSize, Sort.by("id").ascending())
            );

            if (page.isEmpty()) {
                break;
            }

            List<Customer> batch = page.getContent();
            for (Customer c : batch) {
                retrievedIds.add(c.getId());
            }
            lastSeenId = batch.get(batch.size() - 1).getId();

            if (batch.size() < batchSize) {
                break;
            }
        }

        // Verify all 25 customers retrieved in strictly ascending order without duplicates or skips
        assertThat(retrievedIds).hasSize(25);
        Set<Long> uniqueIds = new HashSet<>(retrievedIds);
        assertThat(uniqueIds).hasSize(25);

        for (int i = 0; i < retrievedIds.size() - 1; i++) {
            assertThat(retrievedIds.get(i)).isLessThan(retrievedIds.get(i + 1));
        }
    }

    @Test
    @DisplayName("launchCampaign materializes complete audience into delivery records and outbox via keyset pagination")
    void testLaunchCampaign_KeysetAudienceMaterialization() {
        // Create 15 matching customers in Mumbai
        List<Customer> customers = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Customer c = new Customer();
            c.setFirstName("Audience" + i);
            c.setLastName("Member");
            c.setEmail("audience" + i + "@keyset.test");
            c.setCity("Mumbai");
            c.setTotalSpend(new BigDecimal("500.00"));
            c.setVisitCount(2);
            customers.add(c);
        }
        customerRepository.saveAllAndFlush(customers);

        // Also create 5 non-matching customers in Delhi to ensure criteria filtering
        List<Customer> nonMatching = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Customer c = new Customer();
            c.setFirstName("Other" + i);
            c.setLastName("Delhi");
            c.setEmail("delhi" + i + "@keyset.test");
            c.setCity("Delhi");
            c.setTotalSpend(new BigDecimal("500.00"));
            c.setVisitCount(2);
            nonMatching.add(c);
        }
        customerRepository.saveAllAndFlush(nonMatching);

        Campaign campaign = new Campaign(
                "Mumbai Keyset Campaign",
                "Testing keyset pagination materialization",
                testSegment,
                "Hello {{firstName}}, welcome!",
                testUser
        );
        campaign = campaignRepository.saveAndFlush(campaign);

        var response = campaignService.launchCampaign(campaign.getId());

        assertThat(response).isNotNull();
        assertThat(response.getCampaignId()).isEqualTo(campaign.getId());
        assertThat(response.getStatus()).isEqualTo(CampaignStatus.RUNNING);
        assertThat(response.getTargetAudienceSize()).isEqualTo(15);

        List<CampaignDeliveryRecord> records = deliveryRecordRepository.findAll();
        assertThat(records).hasSize(15);

        List<CampaignDeliveryOutbox> outboxList = outboxRepository.findAll();
        assertThat(outboxList).hasSize(15);

        // Check that all delivery records are for Mumbai customers only
        for (CampaignDeliveryRecord r : records) {
            assertThat(r.getCustomer().getCity()).isEqualTo("Mumbai");
            assertThat(r.getMessage()).contains("Hello " + r.getCustomer().getFirstName());
        }
    }
}
