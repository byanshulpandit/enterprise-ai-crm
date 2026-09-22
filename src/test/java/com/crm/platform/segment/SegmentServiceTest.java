package com.crm.platform.segment;

import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.ConflictException;
import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.segment.dto.SegmentCreateRequest;
import com.crm.platform.segment.dto.SegmentUpdateRequest;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.segment.service.SegmentServiceImpl;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SegmentServiceTest {

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserRepository userRepository;

    private SegmentServiceImpl segmentService;
    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        segmentService = new SegmentServiceImpl(segmentRepository, campaignRepository, userRepository);
        objectMapper = new ObjectMapper();
        testUser = new User("admin_user", "admin@crm.internal", "hash", RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        testUser.setId(1L);
    }

    @Test
    @DisplayName("createSegment with valid request saves and returns segment")
    void createSegment_Valid() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest(
                "Loyal Customers",
                "High spenders",
                objectMapper.readTree("{\"op\":\"AND\"}")
        );

        when(segmentRepository.existsByName("Loyal Customers")).thenReturn(false);
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(testUser));
        when(segmentRepository.save(any(Segment.class))).thenAnswer(inv -> {
            Segment s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        Segment created = segmentService.createSegment(request, "admin_user");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getName()).isEqualTo("Loyal Customers");
        verify(segmentRepository).save(any(Segment.class));
    }

    @Test
    @DisplayName("createSegment with duplicate name throws DuplicateResourceException")
    void createSegment_DuplicateName_ThrowsException() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest(
                "Duplicate Segment",
                "Desc",
                objectMapper.readTree("{\"op\":\"AND\"}")
        );

        when(segmentRepository.existsByName("Duplicate Segment")).thenReturn(true);

        assertThatThrownBy(() -> segmentService.createSegment(request, "admin_user"))
                .isInstanceOf(com.crm.platform.common.exception.DuplicateResourceException.class)
                .hasMessageContaining("Segment with name 'Duplicate Segment' already exists");

        verify(segmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSegment with null rules throws InvalidRequestException")
    void createSegment_NullRules_ThrowsException() {
        SegmentCreateRequest request = new SegmentCreateRequest("Name", "Desc", null);

        assertThatThrownBy(() -> segmentService.createSegment(request, "admin_user"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("rules must not be null");
    }

    @Test
    @DisplayName("getSegmentById throws ResourceNotFoundException when segment missing")
    void getSegmentById_NotFound() {
        when(segmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> segmentService.getSegmentById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Segment not found with id: 999");
    }

    @Test
    @DisplayName("updateSegment updates name, description, and rules")
    void updateSegment_Success() throws Exception {
        Segment existing = new Segment("Old Name", "Old Desc", "{}", testUser);
        existing.setId(5L);
        when(segmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(segmentRepository.save(any(Segment.class))).thenAnswer(inv -> inv.getArgument(0));

        SegmentUpdateRequest updateReq = new SegmentUpdateRequest(
                "New Name",
                "New Desc",
                objectMapper.readTree("{\"updated\":true}")
        );

        Segment updated = segmentService.updateSegment(5L, updateReq);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("New Desc");
        assertThat(updated.getRules()).contains("updated");
    }

    @Test
    @DisplayName("updateSegment with duplicate name of another segment throws DuplicateResourceException")
    void updateSegment_DuplicateName_ThrowsException() {
        Segment existing = new Segment("Old Name", "Old Desc", "{}", testUser);
        existing.setId(5L);
        when(segmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(segmentRepository.existsByNameAndIdNot("Taken Name", 5L)).thenReturn(true);

        SegmentUpdateRequest updateReq = new SegmentUpdateRequest("Taken Name", null, null);

        assertThatThrownBy(() -> segmentService.updateSegment(5L, updateReq))
                .isInstanceOf(com.crm.platform.common.exception.DuplicateResourceException.class)
                .hasMessageContaining("Segment with name 'Taken Name' already exists");

        verify(segmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSegment with same existing name succeeds without throwing DuplicateResourceException")
    void updateSegment_SameExistingName_Succeeds() throws Exception {
        Segment existing = new Segment("VIP", "Old Desc", "{}", testUser);
        existing.setId(10L);
        when(segmentRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(segmentRepository.existsByNameAndIdNot("VIP", 10L)).thenReturn(false);
        when(segmentRepository.save(any(Segment.class))).thenAnswer(inv -> inv.getArgument(0));

        SegmentUpdateRequest updateReq = new SegmentUpdateRequest("VIP", "New Desc", objectMapper.readTree("{\"minSpend\":1000}"));
        Segment updated = segmentService.updateSegment(10L, updateReq);

        assertThat(updated.getName()).isEqualTo("VIP");
        assertThat(updated.getDescription()).isEqualTo("New Desc");
        verify(segmentRepository).save(any(Segment.class));
    }

    @Test
    @DisplayName("createSegment with null or blank callerUsername throws InvalidRequestException")
    void createSegment_NullOrBlankUsername_ThrowsException() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest("Valid Name", "Desc", objectMapper.readTree("{}"));

        assertThatThrownBy(() -> segmentService.createSegment(request, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Authenticated user identity is required");

        assertThatThrownBy(() -> segmentService.createSegment(request, "   "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Authenticated user identity is required");
    }

    @Test
    @DisplayName("createSegment with unknown callerUsername throws ResourceNotFoundException")
    void createSegment_UnknownUsername_ThrowsException() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest("Valid Name", "Desc", objectMapper.readTree("{}"));
        when(segmentRepository.existsByName("Valid Name")).thenReturn(false);
        when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> segmentService.createSegment(request, "unknown_user"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Authenticated user not found: unknown_user");
    }

    @Test
    @DisplayName("deleteSegment succeeds when not referenced by any campaign")
    void deleteSegment_Unreferenced_Success() {
        Segment segment = new Segment("Unused Segment", "Desc", "{}", testUser);
        segment.setId(7L);
        when(segmentRepository.findById(7L)).thenReturn(Optional.of(segment));
        when(campaignRepository.existsBySegmentId(7L)).thenReturn(false);

        segmentService.deleteSegment(7L);

        verify(segmentRepository).delete(segment);
    }

    @Test
    @DisplayName("deleteSegment throws ConflictException when referenced by a campaign")
    void deleteSegment_ReferencedByCampaign_ThrowsConflictException() {
        Segment segment = new Segment("Bound Segment", "Desc", "{}", testUser);
        segment.setId(8L);
        when(segmentRepository.findById(8L)).thenReturn(Optional.of(segment));
        when(campaignRepository.existsBySegmentId(8L)).thenReturn(true);

        assertThatThrownBy(() -> segmentService.deleteSegment(8L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Segment cannot be deleted because it is bound to one or more campaigns");

        verify(segmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("listSegments returns paginated results")
    void listSegments_Pagination() {
        Segment segment = new Segment("Segment 1", "Desc", "{}", testUser);
        Page<Segment> page = new PageImpl<>(Collections.singletonList(segment));
        when(segmentRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Segment> result = segmentService.listSegments(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Segment 1");
    }
}
