package com.crm.platform.campaign;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CampaignRepositoryTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User testUser;
    private Segment testSegment;

    @BeforeEach
    void setUp() {
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("camp_tester", "camp_tester@crm.internal", "hash1234", RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        testUser = userRepository.saveAndFlush(testUser);

        testSegment = new Segment("VIP Segment", "High rollers", "{\"op\":\"AND\"}", testUser);
        testSegment = segmentRepository.saveAndFlush(testSegment);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify Campaign persists with default DRAFT status and correct relationships")
    void testPersistCampaign() {
        Campaign campaign = new Campaign(
                "Summer Sale Campaign",
                "Promotional discounts",
                testSegment,
                "Hello {{firstName}}, enjoy 20% off!",
                Boolean.TRUE,
                testUser
        );

        Campaign saved = campaignRepository.saveAndFlush(campaign);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Summer Sale Campaign");
        assertThat(saved.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(saved.getPersonalizationEnabled()).isTrue();
        assertThat(saved.getSegment().getId()).isEqualTo(testSegment.getId());
        assertThat(saved.getCreatedBy().getId()).isEqualTo(testUser.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Verify findByStatus, existsBySegmentId, and countBySegmentId")
    void testStatusAndSegmentQueries() {
        Campaign draft1 = new Campaign("Draft 1", "Desc", testSegment, "Template 1", testUser);
        Campaign draft2 = new Campaign("Draft 2", "Desc", testSegment, "Template 2", testUser);
        Campaign running = new Campaign("Running 1", "Desc", testSegment, "Template 3", testUser);
        running.setStatus(CampaignStatus.RUNNING);
        running.setStartedAt(Instant.now());

        campaignRepository.saveAndFlush(draft1);
        campaignRepository.saveAndFlush(draft2);
        campaignRepository.saveAndFlush(running);

        Page<Campaign> draftPage = campaignRepository.findByStatus(CampaignStatus.DRAFT, PageRequest.of(0, 10));
        assertThat(draftPage.getTotalElements()).isEqualTo(2);

        Page<Campaign> runningPage = campaignRepository.findByStatus(CampaignStatus.RUNNING, PageRequest.of(0, 10));
        assertThat(runningPage.getTotalElements()).isEqualTo(1);

        assertThat(campaignRepository.existsBySegmentId(testSegment.getId())).isTrue();
        assertThat(campaignRepository.countBySegmentId(testSegment.getId())).isEqualTo(3);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Verify MySQL chk_campaigns_status rejects invalid status via direct SQL")
    void testCampaignStatusCheckConstraint_DirectSql() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            entityManager.createNativeQuery(
                "INSERT INTO campaigns (name, description, segment_id, message_template, status, personalization_enabled, created_by, created_at, updated_at) " +
                "VALUES ('Bad Campaign', 'Desc', :segId, 'Template', 'INVALID_STATUS', false, :userId, NOW(6), NOW(6))"
            )
            .setParameter("segId", testSegment.getId())
            .setParameter("userId", testUser.getId())
            .executeUpdate();
            entityManager.flush();
        }).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Verify MySQL fk_campaigns_segment rejects non-existent segmentId via direct SQL")
    void testCampaignForeignKey_InvalidSegment_Fails() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            entityManager.createNativeQuery(
                "INSERT INTO campaigns (name, description, segment_id, message_template, status, personalization_enabled, created_by, created_at, updated_at) " +
                "VALUES ('FK Fail Campaign', 'Desc', 999999, 'Template', 'DRAFT', false, :userId, NOW(6), NOW(6))"
            )
            .setParameter("userId", testUser.getId())
            .executeUpdate();
            entityManager.flush();
        }).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }
}
