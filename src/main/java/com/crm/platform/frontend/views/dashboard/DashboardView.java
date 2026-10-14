package com.crm.platform.frontend.views.dashboard;

import com.crm.platform.campaign.dto.CampaignResponse;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.charts.TopLocationsBarChart;
import com.crm.platform.frontend.components.common.KpiStatCard;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.CampaignApiService;
import com.crm.platform.frontend.service.ReportApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.reporting.dto.CustomerOverviewReportResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.time.ZoneId;

/**
 * SCR-02: Operational & Executive Dashboard displaying genuine customer and campaign metrics.
 */
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dashboard", layout = MainLayout.class)
@PageTitle("Executive Dashboard | Enterprise AI-CRM")
public class DashboardView extends VerticalLayout {

    private final ReportApiService reportApiService;
    private final CampaignApiService campaignApiService;
    private final StateViewContainer stateContainer;

    public DashboardView(ReportApiService reportApiService, CampaignApiService campaignApiService) {
        this.reportApiService = reportApiService;
        this.campaignApiService = campaignApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        stateContainer = new StateViewContainer();
        add(stateContainer);

        loadDashboardData();
    }

    private void loadDashboardData() {
        stateContainer.showLoading("Calculating business metrics and campaign status...");

        try {
            CustomerOverviewReportResponse overview = reportApiService.getCustomerOverview();
            PageResult<CampaignResponse> recentCampaigns = campaignApiService.listCampaigns(null, 0, 5);

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
            H2 title = new H2("Operational Overview");
            title.addClassName("crm-view-title");
            Paragraph sub = new Paragraph("Live audience demographics, spend metrics, and campaign dispatch activity.");
            sub.addClassName("crm-view-subtitle");
            headerLeft.add(title, sub);

            Button refreshBtn = new Button("Refresh Telemetry", e -> loadDashboardData());
            refreshBtn.addClassName("crm-btn-secondary");

            header.add(headerLeft, refreshBtn);
            content.add(header);

            // KPI Grid
            Div kpiGrid = new Div();
            kpiGrid.addClassName("crm-kpi-grid");

            long activeCount = overview != null ? overview.getTotalActiveCustomers() : 0L;
            kpiGrid.add(new KpiStatCard(
                    "Active Customers",
                    FormattingUtils.formatInteger(activeCount),
                    "Evaluated across master directory"
            ));

            kpiGrid.add(new KpiStatCard(
                    "Gross Customer Spend",
                    FormattingUtils.formatCurrency(overview != null ? overview.getGrossCustomerSpend() : null),
                    "Cumulative verified transactions"
            ));

            kpiGrid.add(new KpiStatCard(
                    "Average Customer Spend",
                    FormattingUtils.formatCurrency(overview != null ? overview.getAverageSpendPerCustomer() : null),
                    "Mean spend across active base"
            ));

            kpiGrid.add(new KpiStatCard(
                    "Total Customer Visits",
                    FormattingUtils.formatInteger(overview != null ? overview.getTotalVisits() : 0L),
                    "Average visits: " + (overview != null ? String.format("%.1f", overview.getAverageVisitsPerCustomer()) : "0.0")
            ));

            content.add(kpiGrid);

            // Two-column layout: Locations Chart and Recent Campaigns
            HorizontalLayout lowerRow = new HorizontalLayout();
            lowerRow.setWidthFull();
            lowerRow.setSpacing(true);

            TopLocationsBarChart chart = new TopLocationsBarChart();
            if (overview != null && overview.getTopLocations() != null) {
                chart.setData(overview.getTopLocations());
            }
            chart.setWidth("45%");

            Div campaignsPanel = new Div();
            campaignsPanel.addClassName("crm-panel");
            campaignsPanel.setWidth("55%");

            Span campaignsTitle = new Span("Recent Campaign Dispatches");
            campaignsTitle.getStyle()
                    .set("font-size", "14px")
                    .set("font-weight", "600")
                    .set("color", "var(--crm-text-primary)")
                    .set("display", "block")
                    .set("margin-bottom", "14px");

            Grid<CampaignResponse> campaignGrid = new Grid<>();
            campaignGrid.setHeight("260px");
            campaignGrid.setItems(recentCampaigns != null ? recentCampaigns.getContent() : java.util.Collections.emptyList());

            campaignGrid.addColumn(CampaignResponse::getName).setHeader("Campaign").setFlexGrow(2);
            campaignGrid.addComponentColumn(c -> new StatusBadge(c.getStatus() != null ? c.getStatus().name() : "DRAFT"))
                    .setHeader("Status").setWidth("120px").setFlexGrow(0);
            campaignGrid.addColumn(c -> c.getCreatedAt() != null ?
                    FormattingUtils.formatDateTime(c.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—")
                    .setHeader("Created").setWidth("150px").setFlexGrow(0);

            campaignGrid.addComponentColumn(c -> {
                Button actionBtn = new Button("View", e -> {
                    getUI().ifPresent(ui -> ui.navigate("campaigns/" + c.getId() + "/monitor"));
                });
                actionBtn.addClassName("crm-btn-secondary");
                actionBtn.getStyle().set("padding", "2px 8px").set("font-size", "11px");
                return actionBtn;
            }).setWidth("90px").setFlexGrow(0);

            campaignsPanel.add(campaignsTitle, campaignGrid);

            lowerRow.add(chart, campaignsPanel);
            content.add(lowerRow);

            stateContainer.setContent(content);

        } catch (Exception ex) {
            stateContainer.showError("Failed to load dashboard metrics: " + ex.getMessage(), this::loadDashboardData);
        }
    }
}
