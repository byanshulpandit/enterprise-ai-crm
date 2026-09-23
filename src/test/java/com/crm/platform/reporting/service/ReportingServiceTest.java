package com.crm.platform.reporting.service;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.reporting.dto.CampaignHistoryItemResponse;
import com.crm.platform.reporting.dto.CampaignReportResponse;
import com.crm.platform.reporting.dto.CustomerOverviewReportResponse;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignDeliveryRecordRepository deliveryRecordRepository;

    @Mock
    private CustomerRepository customerRepository;

    private ReportingServiceImpl reportingService;

    @BeforeEach
    void setUp() {
        reportingService = new ReportingServiceImpl(campaignRepository, deliveryRecordRepository, customerRepository);
    }

    @Test
    @DisplayName("Should return campaign report with delivery rate and duration")
    void testGetCampaignReport_Success() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("Diwali Flash Sale");
        campaign.setStatus(CampaignStatus.COMPLETED);
        Instant now = Instant.now();
        campaign.setStartedAt(now.minusSeconds(120));
        campaign.setCompletedAt(now);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(deliveryRecordRepository.countByCampaignIdAndStatus(1L, DeliveryStatus.SENT)).thenReturn(98L);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(1L, DeliveryStatus.FAILED)).thenReturn(2L);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(1L, DeliveryStatus.PENDING)).thenReturn(0L);

        CampaignReportResponse response = reportingService.getCampaignReport(1L);

        assertThat(response).isNotNull();
        assertThat(response.getCampaignId()).isEqualTo(1L);
        assertThat(response.getCampaignName()).isEqualTo("Diwali Flash Sale");
        assertThat(response.getTargetAudienceSize()).isEqualTo(100L);
        assertThat(response.getMetrics().getSent()).isEqualTo(98L);
        assertThat(response.getMetrics().getFailed()).isEqualTo(2L);
        assertThat(response.getMetrics().getDeliveryRatePercentage()).isEqualTo(98.00);
        assertThat(response.getTimeline().getDurationSeconds()).isEqualTo(120L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown campaign in report")
    void testGetCampaignReport_NotFound() {
        when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportingService.getCampaignReport(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should aggregate customer overview metrics including top locations")
    void testGetCustomerOverview_Success() {
        when(customerRepository.countByDeletedAtIsNull()).thenReturn(50L);
        when(customerRepository.sumActiveTotalSpend()).thenReturn(new BigDecimal("125000.50"));
        when(customerRepository.avgActiveTotalSpend()).thenReturn(new BigDecimal("2500.01"));
        when(customerRepository.sumActiveVisitCount()).thenReturn(350L);
        when(customerRepository.avgActiveVisitCount()).thenReturn(7.0);

        List<Object[]> locations = List.of(
                new Object[]{"Mumbai", 30L},
                new Object[]{"Delhi", 20L}
        );
        when(customerRepository.findTopLocations(any(Pageable.class))).thenReturn(locations);

        CustomerOverviewReportResponse response = reportingService.getCustomerOverview();

        assertThat(response).isNotNull();
        assertThat(response.getTotalActiveCustomers()).isEqualTo(50L);
        assertThat(response.getGrossCustomerSpend()).isEqualTo(new BigDecimal("125000.50"));
        assertThat(response.getTopLocations()).hasSize(2);
        assertThat(response.getTopLocations().get(0).getCity()).isEqualTo("Mumbai");
        assertThat(response.getTopLocations().get(0).getCustomerCount()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Should retrieve paginated campaign history")
    void testGetCampaignHistory_Success() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("Campaign 1");
        campaign.setStatus(CampaignStatus.COMPLETED);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Campaign> page = new PageImpl<>(List.of(campaign), pageable, 1);
        when(campaignRepository.findAll(pageable)).thenReturn(page);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(1L, DeliveryStatus.SENT)).thenReturn(10L);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(1L, DeliveryStatus.FAILED)).thenReturn(0L);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(1L, DeliveryStatus.PENDING)).thenReturn(0L);

        Page<CampaignHistoryItemResponse> result = reportingService.getCampaignHistory(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCampaignName()).isEqualTo("Campaign 1");
        assertThat(result.getContent().get(0).getDeliveryRatePercentage()).isEqualTo(100.0);
    }
}
