package com.crm.platform.reporting.service;

import com.crm.platform.reporting.dto.CampaignHistoryItemResponse;
import com.crm.platform.reporting.dto.CampaignReportResponse;
import com.crm.platform.reporting.dto.CustomerOverviewReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportingService {

    CampaignReportResponse getCampaignReport(Long campaignId);

    CustomerOverviewReportResponse getCustomerOverview();

    Page<CampaignHistoryItemResponse> getCampaignHistory(Pageable pageable);
}
