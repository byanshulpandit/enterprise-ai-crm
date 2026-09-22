package com.crm.platform.segment;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SegmentRepositoryTest {

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("seg_tester", "seg_tester@crm.internal", "hash1234", RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        testUser = userRepository.saveAndFlush(testUser);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify Segment persists correctly with JSON rules, timestamps, and creator relation")
    void testPersistSegment() {
        String jsonRules = "{\"combinator\":\"AND\",\"conditions\":[{\"field\":\"city\",\"operator\":\"EQUALS\",\"value\":\"Mumbai\"}]}";
        Segment segment = new Segment("High Spenders Mumbai", "Customers in Mumbai", jsonRules, testUser);

        Segment saved = segmentRepository.saveAndFlush(segment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("High Spenders Mumbai");
        assertThat(saved.getDescription()).isEqualTo("Customers in Mumbai");
        assertThat(saved.getRules()).isEqualTo(jsonRules);
        assertThat(saved.getCreatedBy().getId()).isEqualTo(testUser.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Verify findByName and existsByName methods")
    void testFindAndExistsByName() {
        String jsonRules = "{\"combinator\":\"OR\",\"conditions\":[]}";
        Segment segment = new Segment("North Shoppers", "Region segment", jsonRules, testUser);
        segmentRepository.saveAndFlush(segment);

        assertThat(segmentRepository.existsByName("North Shoppers")).isTrue();
        assertThat(segmentRepository.existsByName("Non Existent")).isFalse();

        Optional<Segment> foundOpt = segmentRepository.findByName("North Shoppers");
        assertThat(foundOpt).isPresent();
        assertThat(foundOpt.get().getName()).isEqualTo("North Shoppers");
    }

    @Test
    @DisplayName("Verify existsByNameAndIdNot correctly excludes self ID and identifies other duplicates")
    void testExistsByNameAndIdNot() {
        Segment seg1 = new Segment("Alpha Segment", "Desc 1", "{}", testUser);
        seg1 = segmentRepository.saveAndFlush(seg1);

        Segment seg2 = new Segment("Beta Segment", "Desc 2", "{}", testUser);
        seg2 = segmentRepository.saveAndFlush(seg2);

        // seg1 keeping its own name "Alpha Segment" should be false (not a conflict with itself)
        assertThat(segmentRepository.existsByNameAndIdNot("Alpha Segment", seg1.getId())).isFalse();

        // seg2 renaming to "Alpha Segment" should be true (conflict with seg1)
        assertThat(segmentRepository.existsByNameAndIdNot("Alpha Segment", seg2.getId())).isTrue();

        // non-existent name should be false
        assertThat(segmentRepository.existsByNameAndIdNot("Non Existent", seg1.getId())).isFalse();
    }

    @Test
    @DisplayName("Verify updating and deleting a segment")
    void testUpdateAndDeleteSegment() {
        Segment segment = new Segment("Initial Name", "Desc", "{}", testUser);
        Segment saved = segmentRepository.saveAndFlush(segment);

        saved.setName("Updated Name");
        saved.setDescription("Updated Desc");
        Segment updated = segmentRepository.saveAndFlush(saved);

        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getDescription()).isEqualTo("Updated Desc");

        segmentRepository.delete(updated);
        assertThat(segmentRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Verify deletion of segment referenced by a campaign is blocked by foreign key constraint at DB level")
    void testDeleteSegment_BlockedByCampaignForeignKey() {
        Segment segment = new Segment("Referenced Segment", "Desc", "{}", testUser);
        Segment savedSegment = segmentRepository.saveAndFlush(segment);

        Campaign campaign = new Campaign("Dependent Campaign", "Desc", savedSegment, "Template", testUser);
        campaignRepository.saveAndFlush(campaign);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            segmentRepository.delete(savedSegment);
            segmentRepository.flush();
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
