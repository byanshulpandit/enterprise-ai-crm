package com.crm.platform.reporting.service;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.reporting.dto.CampaignHistoryItemResponse;
import com.crm.platform.reporting.dto.CampaignReportResponse;
import com.crm.platform.reporting.dto.CustomerOverviewReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportingServiceImpl implements ReportingService {

    private final CampaignRepository campaignRepository;
    private final CampaignDeliveryRecordRepository deliveryRecordRepository;
    private final CustomerRepository customerRepository;

    public ReportingServiceImpl(
            CampaignRepository campaignRepository,
            CampaignDeliveryRecordRepository deliveryRecordRepository,
            CustomerRepository customerRepository) {
        this.campaignRepository = campaignRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public CampaignReportResponse getCampaignReport(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        long sent = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.SENT);
        long failed = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.FAILED);
        long pending = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.PENDING);
        long targetAudience = sent + failed + pending;

        double deliveryRate = 0.0;
        if (targetAudience > 0) {
            deliveryRate = BigDecimal.valueOf(sent * 100.0)
                    .divide(BigDecimal.valueOf(targetAudience), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        Instant launchedAt = campaign.getStartedAt();
        Instant completedAt = campaign.getCompletedAt();
        Long durationSeconds = (launchedAt != null && completedAt != null)
                ? Duration.between(launchedAt, completedAt).getSeconds()
                : null;

        CampaignReportResponse.MetricsDto metrics = new CampaignReportResponse.MetricsDto(sent, failed, deliveryRate);
        CampaignReportResponse.TimelineDto timeline = new CampaignReportResponse.TimelineDto(launchedAt, completedAt, durationSeconds);

        return new CampaignReportResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getStatus(),
                targetAudience,
                metrics,
                timeline
        );
    }

    @Override
    public CustomerOverviewReportResponse getCustomerOverview() {
        long totalActive = customerRepository.countByDeletedAtIsNull();
        BigDecimal grossSpend = customerRepository.sumActiveTotalSpend();
        BigDecimal avgSpend = customerRepository.avgActiveTotalSpend();
        Long totalVisits = customerRepository.sumActiveVisitCount();
        Double avgVisits = customerRepository.avgActiveVisitCount();

        List<Object[]> locationRows = customerRepository.findTopLocations(PageRequest.of(0, 5));
        List<CustomerOverviewReportResponse.LocationMetricDto> topLocations = new ArrayList<>();
        for (Object[] row : locationRows) {
            String city = (String) row[0];
            long count = ((Number) row[1]).longValue();
            topLocations.add(new CustomerOverviewReportResponse.LocationMetricDto(city, count));
        }

        return new CustomerOverviewReportResponse(
                totalActive,
                grossSpend != null ? grossSpend : BigDecimal.ZERO,
                avgSpend != null ? avgSpend.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                totalVisits != null ? totalVisits : 0L,
                avgVisits != null ? Math.round(avgVisits * 100.0) / 100.0 : 0.0,
                topLocations
        );
    }

    @Override
    public Page<CampaignHistoryItemResponse> getCampaignHistory(Pageable pageable) {
        Page<Campaign> page = campaignRepository.findAll(pageable);
        return page.map(campaign -> {
            long sent = deliveryRecordRepository.countByCampaignIdAndStatus(campaign.getId(), DeliveryStatus.SENT);
            long failed = deliveryRecordRepository.countByCampaignIdAndStatus(campaign.getId(), DeliveryStatus.FAILED);
            long pending = deliveryRecordRepository.countByCampaignIdAndStatus(campaign.getId(), DeliveryStatus.PENDING);
            long total = sent + failed + pending;

            double deliveryRate = (total > 0)
                    ? BigDecimal.valueOf(sent * 100.0).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            return new CampaignHistoryItemResponse(
                    campaign.getId(),
                    campaign.getName(),
                    campaign.getStatus(),
                    total,
                    sent,
                    failed,
                    deliveryRate,
                    campaign.getStartedAt(),
                    campaign.getCompletedAt()
            );
        });
    }
}
