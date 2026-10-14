package com.crm.platform.frontend.views.reports;

import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.charts.TopLocationsBarChart;
import com.crm.platform.frontend.components.common.KpiStatCard;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.ReportApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.reporting.dto.CampaignHistoryItemResponse;
import com.crm.platform.reporting.dto.CustomerOverviewReportResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.ZoneId;

/**
 * SCR-12: Business Intelligence Analytics Center providing portfolio analytics and historical campaign audit.
 */
@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Analytics & Reports | Enterprise AI-CRM")
public class ReportsOverviewView extends VerticalLayout {

    private final ReportApiService reportApiService;
    private final StateViewContainer stateContainer;

    public ReportsOverviewView(ReportApiService reportApiService) {
        this.reportApiService = reportApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        stateContainer = new StateViewContainer();
        add(stateContainer);

        loadReportsData();
    }

    private void loadReportsData() {
        stateContainer.showLoading("Aggregating business intelligence telemetry...");

        try {
            CustomerOverviewReportResponse overview = reportApiService.getCustomerOverview();

            VerticalLayout content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(true);
            content.setWidthFull();

            // Header
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            header.setAlignItems(FlexComponent.Alignment.CENTER);

            Div headerLeft = new Div();
            H2 title = new H2("Analytics & Performance Reporting");
            title.addClassName("crm-view-title");
            Paragraph sub = new Paragraph("Historical campaign delivery metrics, recipient yield, and demographic analytics.");
            sub.addClassName("crm-view-subtitle");
            headerLeft.add(title, sub);

            Button refreshBtn = new Button("Refresh Reports", e -> loadReportsData());
            refreshBtn.addClassName("crm-btn-secondary");

            header.add(headerLeft, refreshBtn);
            content.add(header);

            // Portfolio KPIs
            Div kpiGrid = new Div();
            kpiGrid.addClassName("crm-kpi-grid");

            kpiGrid.add(new KpiStatCard(
                    "Active Directory Size",
                    FormattingUtils.formatInteger(overview != null ? overview.getTotalActiveCustomers() : 0L),
                    "Audience pool"
            ));

            kpiGrid.add(new KpiStatCard(
                    "Gross Customer Spend",
                    FormattingUtils.formatCurrency(overview != null ? overview.getGrossCustomerSpend() : null),
                    "Verified customer revenue"
            ));

            kpiGrid.add(new KpiStatCard(
                    "Average Customer Spend",
                    FormattingUtils.formatCurrency(overview != null ? overview.getAverageSpendPerCustomer() : null),
                    "Per-profile revenue average"
            ));

            kpiGrid.add(new KpiStatCard(
                    "Cumulative Visits",
                    FormattingUtils.formatInteger(overview != null ? overview.getTotalVisits() : 0L),
                    "Customer interaction volume"
            ));

            content.add(kpiGrid);

            // Top Locations Visual Bar
            TopLocationsBarChart chart = new TopLocationsBarChart();
            if (overview != null && overview.getTopLocations() != null) {
                chart.setData(overview.getTopLocations());
            }
            content.add(chart);

            // Historical Campaigns Table
            Div historyPanel = new Div();
            historyPanel.addClassName("crm-panel");
            historyPanel.setWidthFull();

            H3 historyTitle = new H3("Historical Campaign Dispatch Performance");
            historyTitle.addClassName("crm-panel-header");

            Grid<CampaignHistoryItemResponse> historyGrid = new Grid<>();
            historyGrid.setWidthFull();
            historyGrid.setHeight("450px");

            CallbackDataProvider<CampaignHistoryItemResponse, Void> dataProvider = DataProvider.fromCallbacks(
                    query -> {
                        int offset = query.getOffset();
                        int limit = query.getLimit();
                        int page = offset / limit;
                        PageResult<CampaignHistoryItemResponse> result = reportApiService.getCampaignHistory(page, limit);
                        return result.getContent().stream();
                    },
                    query -> {
                        PageResult<CampaignHistoryItemResponse> result = reportApiService.getCampaignHistory(0, 1);
                        return (int) result.getTotalElements();
                    }
            );
            historyGrid.setDataProvider(dataProvider);

            historyGrid.addColumn(h -> "#" + h.getCampaignId())
                    .setHeader("ID")
                    .setWidth("80px")
                    .setFlexGrow(0)
                    .setClassNameGenerator(h -> "crm-mono-cell");

            historyGrid.addComponentColumn(h -> {
                Button nameLink = new Button(h.getCampaignName(), e -> {
                    getUI().ifPresent(ui -> ui.navigate("reports/campaigns/" + h.getCampaignId()));
                });
                nameLink.getStyle()
                        .set("background", "none")
                        .set("border", "none")
                        .set("padding", "0")
                        .set("font-weight", "600")
                        .set("color", "var(--crm-text-primary)")
                        .set("cursor", "pointer");
                return nameLink;
            }).setHeader("Campaign Name").setFlexGrow(2);

            historyGrid.addComponentColumn(h -> new StatusBadge(h.getStatus() != null ? h.getStatus().name() : "COMPLETED"))
                    .setHeader("Status")
                    .setWidth("130px")
                    .setFlexGrow(0);

            historyGrid.addColumn(h -> FormattingUtils.formatInteger(h.getTargetAudienceSize()))
                    .setHeader("Audience")
                    .setWidth("100px")
                    .setFlexGrow(0)
                    .setClassNameGenerator(h -> "crm-numeric-cell");

            historyGrid.addColumn(h -> FormattingUtils.formatInteger(h.getSentCount()))
                    .setHeader("Sent")
                    .setWidth("90px")
                    .setFlexGrow(0)
                    .setClassNameGenerator(h -> "crm-numeric-cell");

            historyGrid.addColumn(h -> FormattingUtils.formatInteger(h.getFailedCount()))
                    .setHeader("Failed")
                    .setWidth("90px")
                    .setFlexGrow(0)
                    .setClassNameGenerator(h -> "crm-numeric-cell");

            historyGrid.addColumn(h -> FormattingUtils.formatPercentage(h.getDeliveryRatePercentage()))
                    .setHeader("Delivery Rate")
                    .setWidth("120px")
                    .setFlexGrow(0)
                    .setClassNameGenerator(h -> "crm-numeric-cell");

            historyGrid.addColumn(h -> h.getLaunchedAt() != null ?
                    FormattingUtils.formatDateTime(h.getLaunchedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—")
                    .setHeader("Dispatched")
                    .setWidth("150px")
                    .setFlexGrow(0);

            historyGrid.addComponentColumn(h -> {
                Button deepDiveBtn = new Button("Audit Report", e -> {
                    getUI().ifPresent(ui -> ui.navigate("reports/campaigns/" + h.getCampaignId()));
                });
                deepDiveBtn.addClassName("crm-btn-secondary");
                deepDiveBtn.getStyle().set("padding", "2px 8px").set("font-size", "11px");
                return deepDiveBtn;
            }).setHeader("Action").setWidth("120px").setFlexGrow(0);

            historyPanel.add(historyTitle, historyGrid);
            content.add(historyPanel);

            stateContainer.setContent(content);

        } catch (Exception ex) {
            stateContainer.showError("Failed to load analytics: " + ex.getMessage(), this::loadReportsData);
        }
    }
}
