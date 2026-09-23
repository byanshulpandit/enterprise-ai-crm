package com.crm.platform.reporting.controller;

import com.crm.platform.ai.dto.AiCampaignSummaryResponse;
import com.crm.platform.ai.service.AiService;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import com.crm.platform.reporting.dto.CampaignHistoryItemResponse;
import com.crm.platform.reporting.dto.CampaignReportResponse;
import com.crm.platform.reporting.dto.CustomerOverviewReportResponse;
import com.crm.platform.reporting.service.ReportingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService reportingService;
    private final AiService aiService;

    public ReportingController(ReportingService reportingService, AiService aiService) {
        this.reportingService = reportingService;
        this.aiService = aiService;
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<ApiResponse<CampaignReportResponse>> getCampaignReport(@PathVariable Long id) {
        CampaignReportResponse report = reportingService.getCampaignReport(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/customers/overview")
    public ResponseEntity<ApiResponse<CustomerOverviewReportResponse>> getCustomerOverview() {
        CustomerOverviewReportResponse overview = reportingService.getCustomerOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/campaigns/{id}/ai-summary")
    public ResponseEntity<ApiResponse<AiCampaignSummaryResponse>> getCampaignAiSummary(@PathVariable Long id) {
        AiCampaignSummaryResponse summary = aiService.generateCampaignAiSummary(id);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/campaigns/history")
    public ResponseEntity<ApiResponse<List<CampaignHistoryItemResponse>>> getCampaignHistory(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<CampaignHistoryItemResponse> page = reportingService.getCampaignHistory(pageable);
        List<CampaignHistoryItemResponse> content = page.getContent();
        PageMetadata pagination = PageMetadata.fromPage(page);
        return ResponseEntity.ok(ApiResponse.success(content, pagination));
    }
}
