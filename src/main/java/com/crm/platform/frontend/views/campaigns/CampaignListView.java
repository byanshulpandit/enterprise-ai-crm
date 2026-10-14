package com.crm.platform.frontend.views.campaigns;

import com.crm.platform.campaign.dto.CampaignResponse;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.common.ConfirmDialog;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.service.CampaignApiService;
import com.crm.platform.frontend.service.SegmentApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.frontend.util.NotificationUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.ZoneId;

/**
 * SCR-08: Campaign Portfolio Console with status filtering and pre-flight launch actions.
 */
@Route(value = "campaigns", layout = MainLayout.class)
@PageTitle("Campaign Portfolio | Enterprise AI-CRM")
public class CampaignListView extends VerticalLayout {

    private final CampaignApiService campaignApiService;
    private final SegmentApiService segmentApiService;
    private final Grid<CampaignResponse> grid = new Grid<>();
    private final Select<String> statusFilter;

    public CampaignListView(CampaignApiService campaignApiService, SegmentApiService segmentApiService) {
        this.campaignApiService = campaignApiService;
        this.segmentApiService = segmentApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        grid.setWidthFull();
        grid.setHeight("600px");

        statusFilter = new Select<>();
        statusFilter.setWidth("160px");
        statusFilter.setItems("ALL", "DRAFT", "RUNNING", "COMPLETED", "FAILED");
        statusFilter.setValue("ALL");
        statusFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        createHeader();
        createFilterBar();
        setupGrid();

        add(grid);
    }

    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Div left = new Div();
        H2 title = new H2("Campaign Portfolio");
        title.addClassName("crm-view-title");
        Paragraph sub = new Paragraph("Manage marketing dispatches, author message templates, and monitor telemetry.");
        sub.addClassName("crm-view-subtitle");
        left.add(title, sub);

        Button newCampaignBtn = new Button("New Campaign", VaadinIcon.PLUS.create(), e -> {
            getUI().ifPresent(ui -> ui.navigate("campaigns/new"));
        });
        newCampaignBtn.addClassName("crm-btn-primary");

        header.add(left, newCampaignBtn);
        add(header);
    }

    private void createFilterBar() {
        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.addClassName("crm-filter-bar");
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.CENTER);

        filterBar.add(statusFilter);
        add(filterBar);
    }

    private void setupGrid() {
        CallbackDataProvider<CampaignResponse, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;

                    String selectedStatus = statusFilter.getValue();
                    CampaignStatus status = null;
                    if (selectedStatus != null && !"ALL".equalsIgnoreCase(selectedStatus)) {
                        try {
                            status = CampaignStatus.valueOf(selectedStatus);
                        } catch (Exception ignored) {}
                    }

                    PageResult<CampaignResponse> result = campaignApiService.listCampaigns(status, page, limit);
                    return result.getContent().stream();
                },
                query -> {
                    String selectedStatus = statusFilter.getValue();
                    CampaignStatus status = null;
                    if (selectedStatus != null && !"ALL".equalsIgnoreCase(selectedStatus)) {
                        try {
                            status = CampaignStatus.valueOf(selectedStatus);
                        } catch (Exception ignored) {}
                    }

                    PageResult<CampaignResponse> result = campaignApiService.listCampaigns(status, 0, 1);
                    return (int) result.getTotalElements();
                }
        );

        grid.setDataProvider(dataProvider);

        grid.addColumn(c -> "#" + c.getId())
                .setHeader("ID")
                .setWidth("80px")
                .setFlexGrow(0)
                .setClassNameGenerator(c -> "crm-mono-cell");

        grid.addComponentColumn(c -> {
            Button nameLink = new Button(c.getName(), e -> {
                getUI().ifPresent(ui -> ui.navigate("campaigns/" + c.getId() + "/monitor"));
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

        grid.addColumn(c -> c.getSegmentName() != null ? c.getSegmentName() : "#" + c.getSegmentId())
                .setHeader("Target Segment")
                .setWidth("180px")
                .setFlexGrow(0);

        grid.addComponentColumn(c -> new StatusBadge(c.getStatus() != null ? c.getStatus().name() : "DRAFT"))
                .setHeader("Status")
                .setWidth("130px")
                .setFlexGrow(0);

        grid.addColumn(c -> c.getCreatedByName() != null ? c.getCreatedByName() : "System")
                .setHeader("Author")
                .setWidth("120px")
                .setFlexGrow(0);

        grid.addColumn(c -> c.getCreatedAt() != null ?
                FormattingUtils.formatDateTime(c.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—")
                .setHeader("Created")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addComponentColumn(c -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            boolean isDraft = c.getStatus() == CampaignStatus.DRAFT;

            if (isDraft) {
                Button launchBtn = new Button("Launch", VaadinIcon.PAPERPLANE.create(), e -> {
                    CampaignLaunchDialog dialog = new CampaignLaunchDialog(campaignApiService, segmentApiService, c, () -> grid.getDataProvider().refreshAll());
                    dialog.open();
                });
                launchBtn.addClassName("crm-btn-primary");
                launchBtn.getStyle().set("padding", "2px 8px").set("font-size", "11px");
                actions.add(launchBtn);

                Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> {
                    getUI().ifPresent(ui -> ui.navigate("campaigns/" + c.getId() + "/edit"));
                });
                editBtn.addClassName("crm-btn-secondary");
                editBtn.getStyle().set("padding", "2px 8px");
                actions.add(editBtn);
            } else {
                Button monitorBtn = new Button("Monitor", VaadinIcon.CHART_LINE.create(), e -> {
                    getUI().ifPresent(ui -> ui.navigate("campaigns/" + c.getId() + "/monitor"));
                });
                monitorBtn.addClassName("crm-btn-secondary");
                monitorBtn.getStyle().set("padding", "2px 8px").set("font-size", "11px");
                actions.add(monitorBtn);
            }

            if (FrontendSecurityContext.isAdmin()) {
                Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                    ConfirmDialog confirm = new ConfirmDialog(
                            "Delete Campaign",
                            "Are you sure you want to delete campaign '" + c.getName() + "'?",
                            "Delete",
                            true,
                            () -> {
                                try {
                                    campaignApiService.deleteCampaign(c.getId());
                                    NotificationUtils.showSuccess("Campaign deleted.");
                                    grid.getDataProvider().refreshAll();
                                } catch (ApiException apiEx) {
                                    NotificationUtils.showError(apiEx.getUserFriendlyMessage());
                                }
                            }
                    );
                    confirm.open();
                });
                deleteBtn.addClassName("crm-btn-danger");
                deleteBtn.getStyle().set("padding", "2px 8px");
                actions.add(deleteBtn);
            }

            return actions;
        }).setHeader("Actions").setWidth("210px").setFlexGrow(0);
    }
}
