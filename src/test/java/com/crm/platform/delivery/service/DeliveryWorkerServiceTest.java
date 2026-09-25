package com.crm.platform.delivery.service;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryWorkerServiceTest {

    @Mock
    private CampaignDeliveryRecordRepository deliveryRecordRepository;

    @Mock
    private CampaignRepository campaignRepository;

    private com.crm.platform.delivery.provider.SimulatedDeliveryProvider deliveryProvider;
    private DeliveryWorkerServiceImpl workerService;

    @BeforeEach
    void setUp() {
        deliveryProvider = new com.crm.platform.delivery.provider.SimulatedDeliveryProvider();
        workerService = new DeliveryWorkerServiceImpl(deliveryRecordRepository, campaignRepository, deliveryProvider);
    }

    @Test
    @DisplayName("Should skip processing if record is not found in database")
    void testProcessDelivery_NotFound() {
        when(deliveryRecordRepository.findByCampaignIdAndCustomerId(1L, 10L)).thenReturn(Optional.empty());

        boolean result = workerService.processDelivery(1L, 10L);

        assertThat(result).isFalse();
        verify(deliveryRecordRepository, never()).updateStatusIfPending(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should skip processing idempotently if record is already in terminal state")
    void testProcessDelivery_AlreadyTerminal() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        Customer customer = new Customer();
        customer.setId(10L);
        CampaignDeliveryRecord record = new CampaignDeliveryRecord(campaign, customer, "Msg");
        record.setStatus(DeliveryStatus.SENT);

        when(deliveryRecordRepository.findByCampaignIdAndCustomerId(1L, 10L)).thenReturn(Optional.of(record));

        boolean result = workerService.processDelivery(1L, 10L);

        assertThat(result).isTrue();
        verify(deliveryRecordRepository, never()).updateStatusIfPending(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should process pending record, update status and complete campaign when no pending remain")
    void testProcessDelivery_SuccessAndCompleteCampaign() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setStatus(CampaignStatus.RUNNING);

        Customer customer = new Customer();
        customer.setId(10L);

        CampaignDeliveryRecord record = new CampaignDeliveryRecord(campaign, customer, "Msg");
        record.setStatus(DeliveryStatus.PENDING);

        when(deliveryRecordRepository.findByCampaignIdAndCustomerId(1L, 10L)).thenReturn(Optional.of(record));
        when(deliveryRecordRepository.updateStatusIfPending(eq(1L), eq(10L), any(), any(), any())).thenReturn(1);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(1L, DeliveryStatus.PENDING)).thenReturn(0L);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        boolean result = workerService.processDelivery(1L, 10L);

        assertThat(result).isTrue();
        verify(deliveryRecordRepository).updateStatusIfPending(eq(1L), eq(10L), any(), any(), any());

        ArgumentCaptor<Campaign> captor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());
        Campaign saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
        assertThat(saved.getCompletedAt()).isNotNull();
    }
}
