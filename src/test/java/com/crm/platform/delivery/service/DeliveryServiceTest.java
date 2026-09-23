package com.crm.platform.delivery.service;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.delivery.dto.DeliveryRecordResponse;
import com.crm.platform.delivery.dto.DeliverySummaryResponse;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignDeliveryRecordRepository deliveryRecordRepository;

    private DeliveryServiceImpl deliveryService;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryServiceImpl(campaignRepository, deliveryRecordRepository);
    }

    @Test
    @DisplayName("Should return accurate delivery summary metrics")
    void testGetDeliverySummary_Success() {
        Campaign campaign = new Campaign();
        campaign.setId(10L);
        campaign.setStatus(CampaignStatus.RUNNING);

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(deliveryRecordRepository.countByCampaignIdAndStatus(10L, DeliveryStatus.PENDING)).thenReturn(10L);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(10L, DeliveryStatus.SENT)).thenReturn(85L);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(10L, DeliveryStatus.FAILED)).thenReturn(5L);

        DeliverySummaryResponse summary = deliveryService.getDeliverySummary(10L);

        assertThat(summary).isNotNull();
        assertThat(summary.getCampaignId()).isEqualTo(10L);
        assertThat(summary.getCampaignStatus()).isEqualTo(CampaignStatus.RUNNING);
        assertThat(summary.getTargetAudienceSize()).isEqualTo(100L);
        assertThat(summary.getPendingCount()).isEqualTo(10L);
        assertThat(summary.getSentCount()).isEqualTo(85L);
        assertThat(summary.getFailedCount()).isEqualTo(5L);
        assertThat(summary.getCompletionPercentage()).isEqualTo(90.00);
        assertThat(summary.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when campaign does not exist for delivery summary")
    void testGetDeliverySummary_NotFound() {
        when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.getDeliverySummary(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaign not found with id: 999");
    }

    @Test
    @DisplayName("Should return paginated deliveries")
    void testGetDeliveries_Success() {
        when(campaignRepository.existsById(10L)).thenReturn(true);

        Campaign campaign = new Campaign();
        campaign.setId(10L);
        Customer customer = new Customer();
        customer.setId(5L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        CampaignDeliveryRecord record = new CampaignDeliveryRecord(campaign, customer, "Hello John");
        record.setId(100L);
        record.setStatus(DeliveryStatus.SENT);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CampaignDeliveryRecord> page = new PageImpl<>(List.of(record), pageable, 1);

        when(deliveryRecordRepository.findByCampaignId(10L, pageable)).thenReturn(page);

        Page<DeliveryRecordResponse> response = deliveryService.getDeliveries(10L, null, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(1);
        DeliveryRecordResponse item = response.getContent().get(0);
        assertThat(item.getId()).isEqualTo(100L);
        assertThat(item.getCustomerEmail()).isEqualTo("john@example.com");
        assertThat(item.getCustomerName()).isEqualTo("John Doe");
        assertThat(item.getStatus()).isEqualTo(DeliveryStatus.SENT);
    }
}
