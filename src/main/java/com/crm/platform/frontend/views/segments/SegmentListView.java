package com.crm.platform.frontend.views.segments;

import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.common.ConfirmDialog;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.SegmentApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.frontend.util.NotificationUtils;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.ZoneId;

/**
 * SCR-05: Audience Segment Catalog displaying configured segmentation rules.
 */
@Route(value = "segments", layout = MainLayout.class)
@PageTitle("Audience Segments | Enterprise AI-CRM")
public class SegmentListView extends VerticalLayout {

    private final SegmentApiService segmentApiService;
    private final Grid<SegmentResponse> grid;

    public SegmentListView(SegmentApiService segmentApiService) {
        this.segmentApiService = segmentApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        createHeader();

        grid = new Grid<>();
        grid.setWidthFull();
        grid.setHeight("600px");
        setupGrid();

        add(grid);
    }

    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Div left = new Div();
        H2 title = new H2("Audience Segments");
        title.addClassName("crm-view-title");
        Paragraph sub = new Paragraph("Define dynamic customer cohorts using logical AST criteria and AI prompts.");
        sub.addClassName("crm-view-subtitle");
        left.add(title, sub);

        Button newSegmentBtn = new Button("New Segment", VaadinIcon.PLUS.create(), e -> {
            getUI().ifPresent(ui -> ui.navigate("segments/new"));
        });
        newSegmentBtn.addClassName("crm-btn-primary");

        header.add(left, newSegmentBtn);
        add(header);
    }

    private void setupGrid() {
        CallbackDataProvider<SegmentResponse, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;

                    PageResult<SegmentResponse> result = segmentApiService.listSegments(page, limit);
                    return result.getContent().stream();
                },
                query -> {
                    PageResult<SegmentResponse> result = segmentApiService.listSegments(0, 1);
                    return (int) result.getTotalElements();
                }
        );

        grid.setDataProvider(dataProvider);

        grid.addColumn(s -> "#" + s.getId())
                .setHeader("ID")
                .setWidth("80px")
                .setFlexGrow(0)
                .setClassNameGenerator(s -> "crm-mono-cell");

        grid.addComponentColumn(s -> {
            Button nameLink = new Button(s.getName(), e -> {
                getUI().ifPresent(ui -> ui.navigate("segments/" + s.getId() + "/members"));
            });
            nameLink.getStyle()
                    .set("background", "none")
                    .set("border", "none")
                    .set("padding", "0")
                    .set("font-weight", "600")
                    .set("color", "var(--crm-text-primary)")
                    .set("cursor", "pointer");
            return nameLink;
        }).setHeader("Segment Name").setFlexGrow(2);

        grid.addColumn(s -> s.getDescription() != null ? s.getDescription() : "—")
                .setHeader("Description")
                .setFlexGrow(2);

        grid.addColumn(s -> s.getCreatedByName() != null ? s.getCreatedByName() : "System")
                .setHeader("Author")
                .setWidth("130px")
                .setFlexGrow(0);

        grid.addColumn(s -> s.getCreatedAt() != null ?
                FormattingUtils.formatDateTime(s.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—")
                .setHeader("Created")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addComponentColumn(s -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button membersBtn = new Button("Audience", VaadinIcon.USERS.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate("segments/" + s.getId() + "/members"));
            });
            membersBtn.addClassName("crm-btn-secondary");
            membersBtn.getStyle().set("padding", "2px 8px").set("font-size", "12px");
            actions.add(membersBtn);

            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate("segments/" + s.getId() + "/edit"));
            });
            editBtn.addClassName("crm-btn-secondary");
            editBtn.getStyle().set("padding", "2px 8px");
            actions.add(editBtn);

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                ConfirmDialog confirm = new ConfirmDialog(
                        "Delete Segment",
                        "Are you sure you want to delete segment '" + s.getName() + "'? This operation cannot be undone.",
                        "Delete",
                        true,
                        () -> {
                            try {
                                segmentApiService.deleteSegment(s.getId());
                                NotificationUtils.showSuccess("Segment deleted.");
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

            return actions;
        }).setHeader("Actions").setWidth("210px").setFlexGrow(0);
    }
}
