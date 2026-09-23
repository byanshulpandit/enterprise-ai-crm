package com.crm.platform.campaign;

import com.crm.platform.campaign.dto.CampaignCreateRequest;
import com.crm.platform.campaign.dto.CampaignUpdateRequest;
import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.campaign.service.CampaignServiceImpl;
import com.crm.platform.common.exception.ConflictException;
import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.crm.platform.segment.service.SegmentService segmentService;

    @Mock
    private com.crm.platform.customer.repository.CustomerRepository customerRepository;

    @Mock
    private com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository deliveryRecordRepository;

    @Mock
    private com.crm.platform.delivery.service.DeliveryStreamProducer deliveryStreamProducer;

    @Mock
    private com.crm.platform.delivery.repository.CampaignDeliveryOutboxRepository outboxRepository;

    @Mock
    private com.crm.platform.delivery.service.DeliveryOutboxPublisher outboxPublisher;

    private CampaignServiceImpl campaignService;
    private User testUser;
    private Segment testSegment;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignServiceImpl(
                campaignRepository,
                segmentRepository,
                userRepository,
                segmentService,
                customerRepository,
                deliveryRecordRepository,
                deliveryStreamProducer,
                outboxRepository,
                outboxPublisher
        );

        testUser = new User("marketer_user", "marketer@crm.internal", "hash", RoleEnum.ROLE_MARKETER, Boolean.TRUE);
        testUser.setId(1L);

        testSegment = new Segment("VIP Segment", "Desc", "{}", testUser);
        testSegment.setId(5L);
    }

    @Test
    @DisplayName("createCampaign creates campaign in DRAFT status with valid segment and creator")
    void createCampaign_Valid_CreatesDraft() {
        CampaignCreateRequest request = new CampaignCreateRequest(
                "Winter Clearance",
                "Year end clearance",
                5L,
                "Hi {{firstName}}, check our deals!",
                Boolean.TRUE
        );

        when(segmentRepository.findById(5L)).thenReturn(Optional.of(testSegment));
        when(userRepository.findByUsername("marketer_user")).thenReturn(Optional.of(testUser));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> {
            Campaign c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        Campaign created = campaignService.createCampaign(request, "marketer_user");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getName()).isEqualTo("Winter Clearance");
        assertThat(created.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(created.getPersonalizationEnabled()).isTrue();
        assertThat(created.getSegment().getId()).isEqualTo(5L);
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    @DisplayName("createCampaign with non-existent segmentId throws ResourceNotFoundException")
    void createCampaign_SegmentNotFound_ThrowsException() {
        CampaignCreateRequest request = new CampaignCreateRequest(
                "Campaign", "Desc", 999L, "Template", false
        );

        when(segmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.createCampaign(request, "marketer_user"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Segment not found with id: 999");

        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCampaign with null or blank callerUsername throws InvalidRequestException")
    void createCampaign_NullOrBlankUsername_ThrowsException() {
        CampaignCreateRequest request = new CampaignCreateRequest("Campaign", "Desc", 5L, "Template", false);
        when(segmentRepository.findById(5L)).thenReturn(Optional.of(testSegment));

        assertThatThrownBy(() -> campaignService.createCampaign(request, null))
                .isInstanceOf(com.crm.platform.common.exception.InvalidRequestException.class)
                .hasMessageContaining("Authenticated user identity is required");

        assertThatThrownBy(() -> campaignService.createCampaign(request, "   "))
                .isInstanceOf(com.crm.platform.common.exception.InvalidRequestException.class)
                .hasMessageContaining("Authenticated user identity is required");
    }

    @Test
    @DisplayName("createCampaign with unknown callerUsername throws ResourceNotFoundException")
    void createCampaign_UnknownUsername_ThrowsException() {
        CampaignCreateRequest request = new CampaignCreateRequest("Campaign", "Desc", 5L, "Template", false);
        when(segmentRepository.findById(5L)).thenReturn(Optional.of(testSegment));
        when(userRepository.findByUsername("unknown_marketer")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.createCampaign(request, "unknown_marketer"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Authenticated user not found: unknown_marketer");
    }

    @Test
    @DisplayName("updateCampaign allowed when status is DRAFT")
    void updateCampaign_DraftStatus_Succeeds() {
        Campaign campaign = new Campaign("Old Title", "Old Desc", testSegment, "Old Template", testUser);
        campaign.setId(20L);
        campaign.setStatus(CampaignStatus.DRAFT);

        when(campaignRepository.findById(20L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

        CampaignUpdateRequest updateReq = new CampaignUpdateRequest(
                "New Title", "New Desc", null, "New Template", true
        );

        Campaign updated = campaignService.updateCampaign(20L, updateReq);

        assertThat(updated.getName()).isEqualTo("New Title");
        assertThat(updated.getDescription()).isEqualTo("New Desc");
        assertThat(updated.getMessageTemplate()).isEqualTo("New Template");
        assertThat(updated.getPersonalizationEnabled()).isTrue();
    }

    @Test
    @DisplayName("updateCampaign rejected with ConflictException when status is RUNNING, COMPLETED, or FAILED")
    void updateCampaign_NonDraftStatus_ThrowsConflictException() {
        for (CampaignStatus nonDraftStatus : new CampaignStatus[]{CampaignStatus.RUNNING, CampaignStatus.COMPLETED, CampaignStatus.FAILED}) {
            Campaign campaign = new Campaign("Title", "Desc", testSegment, "Template", testUser);
            campaign.setId(30L);
            campaign.setStatus(nonDraftStatus);

            when(campaignRepository.findById(30L)).thenReturn(Optional.of(campaign));

            CampaignUpdateRequest updateReq = new CampaignUpdateRequest("New Title", null, null, null, null);

            assertThatThrownBy(() -> campaignService.updateCampaign(30L, updateReq))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Campaign updates are permitted only when status is DRAFT. Current status: " + nonDraftStatus);

            verify(campaignRepository, never()).save(campaign);
        }
    }

    @Test
    @DisplayName("deleteCampaign allowed when status is DRAFT")
    void deleteCampaign_DraftStatus_Succeeds() {
        Campaign campaign = new Campaign("Draft Campaign", "Desc", testSegment, "Template", testUser);
        campaign.setId(40L);
        campaign.setStatus(CampaignStatus.DRAFT);

        when(campaignRepository.findById(40L)).thenReturn(Optional.of(campaign));

        campaignService.deleteCampaign(40L);

        verify(campaignRepository).delete(campaign);
    }

    @Test
    @DisplayName("deleteCampaign rejected with ConflictException when status is not DRAFT")
    void deleteCampaign_NonDraftStatus_ThrowsConflictException() {
        Campaign campaign = new Campaign("Running Campaign", "Desc", testSegment, "Template", testUser);
        campaign.setId(50L);
        campaign.setStatus(CampaignStatus.RUNNING);

        when(campaignRepository.findById(50L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.deleteCampaign(50L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Campaign deletion is permitted only when status is DRAFT. Current status: RUNNING");

        verify(campaignRepository, never()).delete(any());
    }

    @Test
    @DisplayName("listCampaigns with status filter calls findByStatus")
    void listCampaigns_WithStatusFilter() {
        Campaign campaign = new Campaign("Campaign", "Desc", testSegment, "Template", testUser);
        Page<Campaign> page = new PageImpl<>(Collections.singletonList(campaign));

        when(campaignRepository.findByStatus(CampaignStatus.DRAFT, PageRequest.of(0, 10))).thenReturn(page);

        Page<Campaign> result = campaignService.listCampaigns(CampaignStatus.DRAFT, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(campaignRepository).findByStatus(CampaignStatus.DRAFT, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("launchCampaign successfully transitions DRAFT campaign to RUNNING and dispatches tasks")
    void launchCampaign_Success() {
        Campaign campaign = new Campaign("Flash Sale", "Desc", testSegment, "Hello {{firstName}}", testUser);
        campaign.setId(100L);
        campaign.setStatus(CampaignStatus.DRAFT);

        Customer c1 = new Customer();
        c1.setId(1L);
        c1.setFirstName("Aarav");
        c1.setLastName("Sharma");
        c1.setEmail("aarav@crm.internal");

        Customer c2 = new Customer();
        c2.setId(2L);
        c2.setFirstName("Diya");
        c2.setLastName("Patel");
        c2.setEmail("diya@crm.internal");

        org.springframework.data.jpa.domain.Specification<Customer> dummySpec = (root, query, cb) -> null;

        when(campaignRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(campaign));
        when(segmentService.compileSegmentRules(testSegment.getId())).thenReturn(dummySpec);
        when(customerRepository.count(dummySpec)).thenReturn(2L);
        when(customerRepository.findAll(org.mockito.ArgumentMatchers.eq(dummySpec), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(c1, c2)));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRecordRepository.saveAll(any())).thenAnswer(invocation -> {
            List<CampaignDeliveryRecord> list = invocation.getArgument(0);
            for (long i = 1; i <= list.size(); i++) {
                list.get((int) (i - 1)).setId(i);
            }
            return list;
        });

        com.crm.platform.campaign.dto.CampaignLaunchResponse response = campaignService.launchCampaign(100L);

        assertThat(response).isNotNull();
        assertThat(response.getCampaignId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(CampaignStatus.RUNNING);
        assertThat(response.getTargetAudienceSize()).isEqualTo(2);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
        assertThat(campaign.getStartedAt()).isNotNull();

        verify(deliveryRecordRepository).saveAll(any());
        verify(outboxRepository).saveAll(any());
        verify(outboxPublisher).triggerImmediatePublish();
    }

    @Test
    @DisplayName("launchCampaign rejects when campaign is not in DRAFT status")
    void launchCampaign_NonDraftStatus_ThrowsInvalidRequestException() {
        Campaign campaign = new Campaign("Running Campaign", "Desc", testSegment, "Hello", testUser);
        campaign.setId(101L);
        campaign.setStatus(CampaignStatus.RUNNING);

        when(campaignRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.launchCampaign(101L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Only campaigns in DRAFT status can be launched. Current status: RUNNING");

        verify(outboxRepository, never()).saveAll(any());
        verify(outboxPublisher, never()).triggerImmediatePublish();
    }

    @Test
    @DisplayName("launchCampaign rejects when target segment evaluates to zero audience")
    void launchCampaign_ZeroAudience_RejectsAndRemainsDraft() {
        Campaign campaign = new Campaign("Empty Audience Campaign", "Desc", testSegment, "Hello", testUser);
        campaign.setId(102L);
        campaign.setStatus(CampaignStatus.DRAFT);

        org.springframework.data.jpa.domain.Specification<Customer> dummySpec = (root, query, cb) -> null;

        when(campaignRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(campaign));
        when(segmentService.compileSegmentRules(testSegment.getId())).thenReturn(dummySpec);
        when(customerRepository.count(dummySpec)).thenReturn(0L);

        assertThatThrownBy(() -> campaignService.launchCampaign(102L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Campaign launch rejected: Target segment evaluated to 0 matching active customers. Campaign remains in DRAFT.");

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        verify(campaignRepository, never()).save(any());
        verify(outboxRepository, never()).saveAll(any());
        verify(outboxPublisher, never()).triggerImmediatePublish();
    }
}
