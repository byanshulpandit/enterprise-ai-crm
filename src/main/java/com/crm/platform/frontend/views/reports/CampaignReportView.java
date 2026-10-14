package com.crm.platform.frontend.views.reports;

import com.crm.platform.ai.dto.AiCampaignSummaryResponse;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.components.charts.DeliveryProgressGauge;
import com.crm.platform.frontend.components.common.KpiStatCard;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.ReportApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.reporting.dto.CampaignReportResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.ZoneId;

/**
 * SCR-13: Deep Campaign Performance & AI Intelligence Inspector View.
 */
@Route(value = "reports/campaigns/:id", layout = MainLayout.class)
@PageTitle("Campaign Performance Report | Enterprise AI-CRM")
public class CampaignReportView extends VerticalLayout implements BeforeEnterObserver {

    private final ReportApiService reportApiService;
    private final StateViewContainer stateContainer;
    private Long campaignId;

    public CampaignReportView(ReportApiService reportApiService) {
        this.reportApiService = reportApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        stateContainer = new StateViewContainer();
        add(stateContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String idParam = event.getRouteParameters().get("id").orElse(null);
        if (idParam == null) {
            stateContainer.showError("Campaign ID parameter is missing.", () -> getUI().ifPresent(ui -> ui.navigate("reports")));
            return;
        }

        try {
            this.campaignId = Long.parseLong(idParam);
            loadReport();
        } catch (NumberFormatException e) {
            stateContainer.showError("Invalid Campaign ID: " + idParam, () -> getUI().ifPresent(ui -> ui.navigate("reports")));
        }
    }

    private void loadReport() {
        stateContainer.showLoading("Compiling Campaign Performance Report for #" + campaignId + "...");

        try {
            CampaignReportResponse report = reportApiService.getCampaignReport(campaignId);
            if (report == null) {
                stateContainer.showEmpty("Report Not Found", "No performance ledger found for campaign #" + campaignId);
                return;
            }

            // Attempt AI campaign summary
            AiCampaignSummaryResponse aiSummary = null;
            try {
                aiSummary = reportApiService.getCampaignAiSummary(campaignId);
            } catch (Exception e) {
                // Non-blocking: AI summary is supplementary intelligence
            }

            VerticalLayout content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(true);
            content.setWidthFull();

            // Navigation Bar
            HorizontalLayout navBar = new HorizontalLayout();
            navBar.setWidthFull();
            navBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            Button backBtn = new Button("← Back to Performance Overview", e -> getUI().ifPresent(ui -> ui.navigate("reports")));
            backBtn.addClassName("crm-btn-secondary");

            Button monitorBtn = new Button("Live Delivery Monitor", VaadinIcon.DASHBOARD.create(),
                    e -> getUI().ifPresent(ui -> ui.navigate("campaigns/" + campaignId + "/monitor")));
            monitorBtn.addClassName("crm-btn-primary");

            navBar.add(backBtn, monitorBtn);
            content.add(navBar);

            // Header Banner
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            header.setAlignItems(FlexComponent.Alignment.CENTER);

            VerticalLayout titleBox = new VerticalLayout();
            titleBox.setPadding(false);
            titleBox.setSpacing(false);

            H2 title = new H2(report.getCampaignName());
            title.getStyle().set("margin", "0 0 4px 0");
            title.getStyle().set("color", "#f0f6fc");

            Span metaSpan = new Span("Campaign ID: #" + report.getCampaignId() + " • Target Audience: " +
                    FormattingUtils.formatNumber(report.getTargetAudienceSize()) + " recipients");
            metaSpan.getStyle().set("color", "#8b949e").set("font-size", "13px");
            titleBox.add(title, metaSpan);

            StatusBadge statusBadge = buildStatusBadge(report.getStatus());
            header.add(titleBox, statusBadge);
            content.add(header);

            // KPI Metrics Row
            HorizontalLayout kpiRow = new HorizontalLayout();
            kpiRow.setWidthFull();
            kpiRow.setSpacing(true);

            long sent = report.getMetrics() != null ? report.getMetrics().getSent() : 0;
            long failed = report.getMetrics() != null ? report.getMetrics().getFailed() : 0;
            double rate = report.getMetrics() != null ? report.getMetrics().getDeliveryRatePercentage() : 0.0;

            KpiStatCard targetCard = new KpiStatCard("Target Audience", FormattingUtils.formatNumber(report.getTargetAudienceSize()),
                    "Qualified recipients", "crm-border-neutral");
            KpiStatCard sentCard = new KpiStatCard("Dispatched / Sent", FormattingUtils.formatNumber(sent),
                    "Confirmed deliveries", "crm-border-success");
            KpiStatCard failedCard = new KpiStatCard("Failed / Bounced", FormattingUtils.formatNumber(failed),
                    "Delivery exceptions", "crm-border-danger");
            KpiStatCard rateCard = new KpiStatCard("Delivery Yield", FormattingUtils.formatPercentage(rate),
                    "Completion efficiency", "crm-border-amber");

            kpiRow.add(targetCard, sentCard, failedCard, rateCard);
            content.add(kpiRow);

            // Progress Visualization & Timeline Breakdown
            HorizontalLayout visualRow = new HorizontalLayout();
            visualRow.setWidthFull();
            visualRow.setSpacing(true);

            Div gaugeBox = new Div();
            gaugeBox.addClassName("crm-card");
            gaugeBox.setWidth("50%");
            H3 gaugeTitle = new H3("Dispatch Yield Distribution");
            gaugeTitle.getStyle().set("margin-top", "0").set("color", "#f0f6fc").set("font-size", "16px");

            DeliveryProgressGauge gauge = new DeliveryProgressGauge(report.getTargetAudienceSize(), sent, failed);
            gaugeBox.add(gaugeTitle, gauge);

            Div timelineBox = new Div();
            timelineBox.addClassName("crm-card");
            timelineBox.setWidth("50%");
            H3 timelineTitle = new H3("Operational Timeline");
            timelineTitle.getStyle().set("margin-top", "0").set("color", "#f0f6fc").set("font-size", "16px");

            VerticalLayout timelineList = new VerticalLayout();
            timelineList.setPadding(false);
            timelineList.setSpacing(true);

            ZoneId zone = ZoneId.systemDefault();
            String launchedStr = (report.getTimeline() != null && report.getTimeline().getLaunchedAt() != null)
                    ? FormattingUtils.formatInstant(report.getTimeline().getLaunchedAt(), zone)
                    : "Not yet launched";

            String completedStr = (report.getTimeline() != null && report.getTimeline().getCompletedAt() != null)
                    ? FormattingUtils.formatInstant(report.getTimeline().getCompletedAt(), zone)
                    : (report.getStatus() == CampaignStatus.RUNNING ? "In Progress..." : "N/A");

            String durationStr = (report.getTimeline() != null && report.getTimeline().getDurationSeconds() != null)
                    ? report.getTimeline().getDurationSeconds() + " seconds"
                    : "N/A";

            timelineList.add(
                    buildTimelineItem("Campaign Dispatched", launchedStr, VaadinIcon.PLAY_CIRCLE.create(), "#38bdf8"),
                    buildTimelineItem("Campaign Completed", completedStr, VaadinIcon.CHECK_CIRCLE.create(), "#10b981"),
                    buildTimelineItem("Total Execution Time", durationStr, VaadinIcon.STOPWATCH.create(), "#d97706")
            );
            timelineBox.add(timelineTitle, timelineList);

            visualRow.add(gaugeBox, timelineBox);
            content.add(visualRow);

            // AI Performance Intelligence Synthesis
            Div aiCard = new Div();
            aiCard.addClassName("crm-card");
            aiCard.setWidthFull();

            HorizontalLayout aiHeader = new HorizontalLayout();
            aiHeader.setAlignItems(FlexComponent.Alignment.CENTER);
            aiHeader.setSpacing(true);

            Span aiBadge = new Span("GEMINI INTELLIGENCE");
            aiBadge.addClassName("crm-badge");
            aiBadge.getStyle().set("background-color", "rgba(217, 119, 6, 0.15)")
                    .set("color", "#f59e0b")
                    .set("border", "1px solid rgba(217, 119, 6, 0.4)")
                    .set("font-weight", "600");

            H3 aiTitle = new H3("Campaign AI Analysis & Synthesis");
            aiTitle.getStyle().set("margin", "0").set("color", "#f0f6fc").set("font-size", "16px");

            aiHeader.add(aiBadge, aiTitle);

            Paragraph aiSummaryText = new Paragraph();
            aiSummaryText.getStyle().set("color", "#c9d1d9").set("font-size", "14px").set("line-height", "1.6").set("margin-top", "12px");

            if (aiSummary != null && aiSummary.getAiSummary() != null && !aiSummary.getAiSummary().isBlank()) {
                aiSummaryText.setText(aiSummary.getAiSummary());
            } else {
                aiSummaryText.setText("Automated campaign performance synthesis has not completed yet or requires campaign execution data to formulate predictive delivery diagnostics.");
                aiSummaryText.getStyle().set("color", "#8b949e").set("font-style", "italic");
            }

            aiCard.add(aiHeader, aiSummaryText);
            content.add(aiCard);

            stateContainer.showContent(content);

        } catch (ApiException e) {
            stateContainer.showError("Failed to retrieve campaign report: " + e.getMessage(), this::loadReport);
        } catch (Exception e) {
            stateContainer.showError("Unexpected error loading campaign metrics: " + e.getMessage(), this::loadReport);
        }
    }

    private HorizontalLayout buildTimelineItem(String label, String value, com.vaadin.flow.component.Component icon, String iconColor) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);

        icon.getElement().getStyle().set("color", iconColor).set("width", "18px").set("height", "18px");

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "#8b949e").set("font-size", "13px").set("min-width", "180px");

        Span valSpan = new Span(value);
        valSpan.getStyle().set("color", "#f0f6fc").set("font-size", "13px").set("font-weight", "500");

        row.add(icon, labelSpan, valSpan);
        return row;
    }

    private StatusBadge buildStatusBadge(CampaignStatus status) {
        if (status == null) return new StatusBadge("UNKNOWN", StatusBadge.BadgeVariant.NEUTRAL);
        return switch (status) {
            case RUNNING -> new StatusBadge("RUNNING", StatusBadge.BadgeVariant.INFO);
            case COMPLETED -> new StatusBadge("COMPLETED", StatusBadge.BadgeVariant.SUCCESS);
            case FAILED -> new StatusBadge("FAILED", StatusBadge.BadgeVariant.DANGER);
            case DRAFT -> new StatusBadge("DRAFT", StatusBadge.BadgeVariant.NEUTRAL);
        };
    }
}
