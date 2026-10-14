package com.crm.platform.frontend.views.campaigns;

import com.crm.platform.ai.dto.AiCampaignSummaryResponse;
import com.crm.platform.campaign.dto.CampaignResponse;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.delivery.dto.DeliveryRecordResponse;
import com.crm.platform.delivery.dto.DeliverySummaryResponse;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.charts.DeliveryProgressGauge;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.CampaignApiService;
import com.crm.platform.frontend.service.DeliveryApiService;
import com.crm.platform.frontend.service.ReportApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.frontend.util.NotificationUtils;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import java.time.ZoneId;

/**
 * SCR-10: Near-Real-Time Delivery Monitoring Console with periodic polling and recipient status tracking.
 */
@Route(value = "campaigns/:id/monitor", layout = MainLayout.class)
@PageTitle("Delivery Monitor | Enterprise AI-CRM")
public class CampaignMonitorView extends VerticalLayout implements BeforeEnterObserver, BeforeLeaveObserver {

    private final CampaignApiService campaignApiService;
    private final DeliveryApiService deliveryApiService;
    private final ReportApiService reportApiService;
    private final StateViewContainer stateContainer;

    private Long campaignId;
    private CampaignResponse campaign;
    private DeliverySummaryResponse latestSummary;
    private Registration pollRegistration;
    private int consecutivePollFailures = 0;

    private final DeliveryProgressGauge progressGauge = new DeliveryProgressGauge();
    private final Grid<DeliveryRecordResponse> deliveryGrid = new Grid<>();
    private final Select<String> statusFilter = new Select<>();
    private final HorizontalLayout statusBadgeContainer = new HorizontalLayout();
    private final Button aiSummaryBtn = new Button("AI Performance Summary", VaadinIcon.MAGIC.create());

    public CampaignMonitorView(CampaignApiService campaignApiService,
                               DeliveryApiService deliveryApiService,
                               ReportApiService reportApiService) {
        this.campaignApiService = campaignApiService;
        this.deliveryApiService = deliveryApiService;
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
            stateContainer.showError("Campaign ID missing", () -> getUI().ifPresent(ui -> ui.navigate("campaigns")));
            return;
        }

        try {
            this.campaignId = Long.parseLong(idParam);
            loadMonitorConsole();
        } catch (NumberFormatException e) {
            stateContainer.showError("Invalid Campaign ID: " + idParam, () -> getUI().ifPresent(ui -> ui.navigate("campaigns")));
        }
    }

    private void loadMonitorConsole() {
        stateContainer.showLoading("Loading campaign delivery console...");

        try {
            campaign = campaignApiService.getCampaignById(campaignId);
            latestSummary = deliveryApiService.getDeliverySummary(campaignId);

            VerticalLayout content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(true);
            content.setWidthFull();

            Button backBtn = new Button("← Back to Campaigns", e -> getUI().ifPresent(ui -> ui.navigate("campaigns")));
            backBtn.addClassName("crm-btn-secondary");
            content.add(backBtn);

            // Header
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            header.setAlignItems(FlexComponent.Alignment.CENTER);

            Div headerLeft = new Div();
            H2 title = new H2(campaign != null ? campaign.getName() : "Campaign #" + campaignId);
            title.addClassName("crm-view-title");

            statusBadgeContainer.removeAll();
            statusBadgeContainer.setAlignItems(FlexComponent.Alignment.CENTER);
            if (latestSummary != null && latestSummary.getCampaignStatus() != null) {
                statusBadgeContainer.add(new StatusBadge(latestSummary.getCampaignStatus().name()));
            }

            Paragraph sub = new Paragraph("Live dispatch telemetry and recipient execution log.");
            sub.addClassName("crm-view-subtitle");
            headerLeft.add(title, statusBadgeContainer, sub);

            aiSummaryBtn.addClassName("crm-btn-primary");
            aiSummaryBtn.addClickListener(e -> showAiSummaryDialog());
            aiSummaryBtn.setEnabled(latestSummary != null && latestSummary.isTerminal());

            header.add(headerLeft, aiSummaryBtn);
            content.add(header);

            // Progress Gauge
            progressGauge.update(latestSummary);
            content.add(progressGauge);

            // Delivery Table
            Div tablePanel = new Div();
            tablePanel.addClassName("crm-panel");
            tablePanel.setWidthFull();

            HorizontalLayout tableHeader = new HorizontalLayout();
            tableHeader.setWidthFull();
            tableHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            tableHeader.setAlignItems(FlexComponent.Alignment.CENTER);

            H3 tableTitle = new H3("Recipient Outbox Ledger");
            tableTitle.addClassName("crm-panel-header");
            tableTitle.getStyle().set("margin-bottom", "0");

            statusFilter.setItems("ALL", "PENDING", "SENT", "FAILED");
            statusFilter.setValue("ALL");
            statusFilter.setWidth("140px");
            statusFilter.addValueChangeListener(e -> deliveryGrid.getDataProvider().refreshAll());

            tableHeader.add(tableTitle, statusFilter);
            tablePanel.add(tableHeader);

            setupDeliveryGrid();
            tablePanel.add(deliveryGrid);

            content.add(tablePanel);
            stateContainer.setContent(content);

            setupPollingIfRunning();

        } catch (Exception ex) {
            stateContainer.showError("Failed to initialize monitor console: " + ex.getMessage(), this::loadMonitorConsole);
        }
    }

    private void setupDeliveryGrid() {
        deliveryGrid.setWidthFull();
        deliveryGrid.setHeight("450px");

        CallbackDataProvider<DeliveryRecordResponse, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;

                    String sel = statusFilter.getValue();
                    DeliveryStatus status = null;
                    if (sel != null && !"ALL".equalsIgnoreCase(sel)) {
                        try {
                            status = DeliveryStatus.valueOf(sel);
                        } catch (Exception ignored) {}
                    }

                    PageResult<DeliveryRecordResponse> result = deliveryApiService.getDeliveries(campaignId, status, page, limit);
                    return result.getContent().stream();
                },
                query -> {
                    String sel = statusFilter.getValue();
                    DeliveryStatus status = null;
                    if (sel != null && !"ALL".equalsIgnoreCase(sel)) {
                        try {
                            status = DeliveryStatus.valueOf(sel);
                        } catch (Exception ignored) {}
                    }
                    PageResult<DeliveryRecordResponse> result = deliveryApiService.getDeliveries(campaignId, status, 0, 1);
                    return (int) result.getTotalElements();
                }
        );

        deliveryGrid.setDataProvider(dataProvider);

        deliveryGrid.addColumn(d -> "#" + d.getId())
                .setHeader("ID")
                .setWidth("80px")
                .setFlexGrow(0)
                .setClassNameGenerator(d -> "crm-mono-cell");

        deliveryGrid.addColumn(d -> d.getCustomerName() != null ? d.getCustomerName() : "Customer #" + d.getCustomerId())
                .setHeader("Recipient")
                .setFlexGrow(2);

        deliveryGrid.addColumn(DeliveryRecordResponse::getCustomerEmail)
                .setHeader("Email")
                .setFlexGrow(2)
                .setClassNameGenerator(d -> "crm-mono-cell");

        deliveryGrid.addComponentColumn(d -> new StatusBadge(d.getStatus() != null ? d.getStatus().name() : "PENDING"))
                .setHeader("Status")
                .setWidth("120px")
                .setFlexGrow(0);

        deliveryGrid.addColumn(d -> d.getFailureReason() != null ? d.getFailureReason() : "—")
                .setHeader("Failure Reason")
                .setFlexGrow(3)
                .setClassNameGenerator(d -> d.getFailureReason() != null ? "crm-mono-cell" : "");

        deliveryGrid.addColumn(d -> d.getProcessedAt() != null ?
                FormattingUtils.formatDateTime(d.getProcessedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—")
                .setHeader("Timestamp")
                .setWidth("150px")
                .setFlexGrow(0);
    }

    private void setupPollingIfRunning() {
        if (latestSummary != null && latestSummary.getCampaignStatus() == CampaignStatus.RUNNING && !latestSummary.isTerminal()) {
            UI currentUi = UI.getCurrent();
            if (currentUi != null) {
                currentUi.setPollInterval(2500); // 2.5 seconds
                if (pollRegistration != null) {
                    pollRegistration.remove();
                }
                pollRegistration = currentUi.addPollListener(e -> pollTelemetry());
            }
        } else {
            stopPolling();
        }
    }

    private void pollTelemetry() {
        try {
            DeliverySummaryResponse summary = deliveryApiService.getDeliverySummary(campaignId);
            this.latestSummary = summary;
            this.consecutivePollFailures = 0;
            progressGauge.update(summary);

            statusBadgeContainer.removeAll();
            if (summary.getCampaignStatus() != null) {
                statusBadgeContainer.add(new StatusBadge(summary.getCampaignStatus().name()));
            }

            deliveryGrid.getDataProvider().refreshAll();

            if (summary.isTerminal()) {
                stopPolling();
                aiSummaryBtn.setEnabled(true);
                NotificationUtils.showSuccess("Campaign dispatch reached terminal status: " + summary.getCampaignStatus());
            }
        } catch (Exception ex) {
            consecutivePollFailures++;
            if (consecutivePollFailures >= 3) {
                stopPolling();
                NotificationUtils.showWarning("Telemetry polling paused due to connection errors.");
            }
        }
    }

    private void stopPolling() {
        UI currentUi = UI.getCurrent();
        if (currentUi != null) {
            currentUi.setPollInterval(-1);
        }
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
    }

    private void showAiSummaryDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("AI Campaign Performance Analysis");
        title.getStyle().set("margin", "0 0 8px 0").set("color", "var(--crm-text-primary)");

        try {
            AiCampaignSummaryResponse summary = reportApiService.getCampaignAiSummary(campaignId);
            Paragraph analysis = new Paragraph(summary != null && summary.getAiSummary() != null ? summary.getAiSummary() : "No AI analysis available.");
            analysis.getStyle().set("color", "var(--crm-text-secondary)").set("font-size", "13px").set("line-height", "1.6");
            layout.add(title, analysis);
        } catch (Exception ex) {
            Paragraph error = new Paragraph("Failed to generate AI performance summary: " + ex.getMessage());
            error.getStyle().set("color", "var(--crm-status-error-base)");
            layout.add(title, error);
        }

        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.addClassName("crm-btn-secondary");
        layout.add(closeBtn);

        dialog.add(layout);
        dialog.open();
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        stopPolling();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopPolling();
        super.onDetach(detachEvent);
    }
}
