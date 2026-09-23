package com.crm.platform.delivery.controller;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CampaignDeliveryRecordRepository deliveryRecordRepository;

    private Campaign testCampaign;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        deliveryRecordRepository.deleteAll();

        User marketer = userRepository.findByUsername("marketer_delivery").orElseGet(() -> {
            User u = new User("marketer_delivery", "marketer_delivery@crm.internal", "$2a$12$hash", RoleEnum.ROLE_MARKETER);
            return userRepository.save(u);
        });

        Segment segment = segmentRepository.findByName("Delivery Test Segment").orElseGet(() -> {
            Segment s = new Segment("Delivery Test Segment", "Desc", "{\"combinator\":\"AND\",\"conditions\":[]}", marketer);
            return segmentRepository.save(s);
        });

        testCampaign = campaignRepository.findByName("Delivery Test Campaign").orElseGet(() -> {
            Campaign c = new Campaign("Delivery Test Campaign", "Desc", segment, "Hello {{firstName}}", marketer);
            c.setStatus(CampaignStatus.RUNNING);
            return campaignRepository.save(c);
        });

        long ts = System.currentTimeMillis();
        testCustomer = new Customer();
        testCustomer.setFirstName("Test");
        testCustomer.setLastName("Delivery");
        testCustomer.setEmail("deliv_" + ts + "@example.com");
        testCustomer.setCity("Mumbai");
        testCustomer.setTotalSpend(BigDecimal.valueOf(500));
        testCustomer.setVisitCount(2);
        testCustomer = customerRepository.save(testCustomer);

        CampaignDeliveryRecord record = new CampaignDeliveryRecord(testCampaign, testCustomer, "Hello Test");
        record.setStatus(DeliveryStatus.SENT);
        deliveryRecordRepository.save(record);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        deliveryRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated delivery summary request")
    void testGetDeliverySummaryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/" + testCampaign.getId() + "/delivery-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return delivery summary for authenticated MARKETER")
    @WithMockUser(username = "marketer_delivery", roles = {"MARKETER"})
    void testGetDeliverySummaryAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/" + testCampaign.getId() + "/delivery-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.campaignId", is(testCampaign.getId().intValue())))
                .andExpect(jsonPath("$.data.sentCount", is(1)))
                .andExpect(jsonPath("$.data.pendingCount", is(0)));
    }

    @Test
    @DisplayName("Should return paginated deliveries for authenticated MARKETER")
    @WithMockUser(username = "marketer_delivery", roles = {"MARKETER"})
    void testGetDeliveriesAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/" + testCampaign.getId() + "/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].customerEmail", is(testCustomer.getEmail())))
                .andExpect(jsonPath("$.data[0].status", is("SENT")));
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated campaign launch")
    void testLaunchCampaignUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/campaigns/" + testCampaign.getId() + "/launch"))
                .andExpect(status().isUnauthorized());
    }
}
