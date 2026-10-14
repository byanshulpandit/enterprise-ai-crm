package com.crm.platform.frontend.views.segments;

import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.common.KpiStatCard;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.SegmentApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.segment.dto.SegmentPreviewResponse;
import com.crm.platform.segment.dto.SegmentResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * SCR-07: Evaluated Segment Members Inspector showing live preview audience count and member roster.
 */
@Route(value = "segments/:id/members", layout = MainLayout.class)
@PageTitle("Segment Audience | Enterprise AI-CRM")
public class SegmentMembersView extends VerticalLayout implements BeforeEnterObserver {

    private final SegmentApiService segmentApiService;
    private final StateViewContainer stateContainer;
    private Long segmentId;

    public SegmentMembersView(SegmentApiService segmentApiService) {
        this.segmentApiService = segmentApiService;

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
            stateContainer.showError("Segment ID parameter missing", () -> getUI().ifPresent(ui -> ui.navigate("segments")));
            return;
        }

        try {
            this.segmentId = Long.parseLong(idParam);
            loadMembersView();
        } catch (NumberFormatException e) {
            stateContainer.showError("Invalid Segment ID: " + idParam, () -> getUI().ifPresent(ui -> ui.navigate("segments")));
        }
    }

    private void loadMembersView() {
        stateContainer.showLoading("Evaluating segment criteria against active customer directory...");

        try {
            SegmentResponse segment = segmentApiService.getSegmentById(segmentId);
            SegmentPreviewResponse preview = segmentApiService.previewSegment(segmentId);

            VerticalLayout content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(true);
            content.setWidthFull();

            Button backBtn = new Button("← Back to Segments", e -> getUI().ifPresent(ui -> ui.navigate("segments")));
            backBtn.addClassName("crm-btn-secondary");
            content.add(backBtn);

            // Header
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            header.setAlignItems(FlexComponent.Alignment.CENTER);

            Div headerLeft = new Div();
            H2 title = new H2("Audience Cohort: " + (segment != null ? segment.getName() : "Segment #" + segmentId));
            title.addClassName("crm-view-title");
            Paragraph sub = new Paragraph("Live evaluation results. Members match all criteria specified in the segment rule tree.");
            sub.addClassName("crm-view-subtitle");
            headerLeft.add(title, sub);

            Button editBtn = new Button("Edit Rules", VaadinIcon.EDIT.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate("segments/" + segmentId + "/edit"));
            });
            editBtn.addClassName("crm-btn-secondary");

            header.add(headerLeft, editBtn);
            content.add(header);

            // Audience Count KPI
            Div kpiGrid = new Div();
            kpiGrid.addClassName("crm-kpi-grid");

            long count = preview != null && preview.getMatchedAudienceCount() != null ? preview.getMatchedAudienceCount() : 0L;
            kpiGrid.add(new KpiStatCard(
                    "Matched Audience Size",
                    FormattingUtils.formatInteger(count),
                    "Customers actively qualifying for this segment"
            ));
            content.add(kpiGrid);

            // Members Table
            Grid<CustomerResponseDto> grid = new Grid<>();
            grid.setWidthFull();
            grid.setHeight("550px");

            CallbackDataProvider<CustomerResponseDto, Void> dataProvider = DataProvider.fromCallbacks(
                    query -> {
                        int offset = query.getOffset();
                        int limit = query.getLimit();
                        int page = offset / limit;
                        PageResult<CustomerResponseDto> result = segmentApiService.getSegmentMembers(segmentId, page, limit);
                        return result.getContent().stream();
                    },
                    query -> (int) count
            );
            grid.setDataProvider(dataProvider);

            grid.addColumn(c -> "#" + c.getId())
                    .setHeader("ID")
                    .setWidth("80px")
                    .setFlexGrow(0)
                    .setClassNameGenerator(c -> "crm-mono-cell");

            grid.addColumn(c -> c.getFirstName() + " " + c.getLastName())
                    .setHeader("Customer Name")
                    .setFlexGrow(2);

            grid.addColumn(CustomerResponseDto::getEmail)
                    .setHeader("Email")
                    .setFlexGrow(2)
                    .setClassNameGenerator(c -> "crm-mono-cell");

            grid.addColumn(c -> c.getCity() != null ? c.getCity() : "—")
                    .setHeader("City")
                    .setWidth("130px")
                    .setFlexGrow(0);

            grid.addColumn(c -> FormattingUtils.formatCurrency(c.getTotalSpend()))
                    .setHeader("Spend")
                    .setWidth("120px")
                    .setFlexGrow(0)
                    .setClassNameGenerator(c -> "crm-numeric-cell");

            grid.addColumn(c -> FormattingUtils.formatInteger(c.getVisitCount()))
                    .setHeader("Visits")
                    .setWidth("80px")
                    .setFlexGrow(0)
                    .setClassNameGenerator(c -> "crm-numeric-cell");

            grid.addColumn(c -> FormattingUtils.formatDate(c.getLastActiveDate()))
                    .setHeader("Last Active")
                    .setWidth("120px")
                    .setFlexGrow(0);

            content.add(grid);
            stateContainer.setContent(content);

        } catch (Exception ex) {
            stateContainer.showError("Failed to evaluate segment audience: " + ex.getMessage(), this::loadMembersView);
        }
    }
}
