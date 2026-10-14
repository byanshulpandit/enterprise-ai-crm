package com.crm.platform.frontend.views.segments;

import com.crm.platform.ai.service.AiService;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.components.builder.RuleTreeContainer;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.AiApiService;
import com.crm.platform.frontend.service.SegmentApiService;
import com.crm.platform.frontend.util.NotificationUtils;
import com.crm.platform.segment.dto.SegmentCreateRequest;
import com.crm.platform.segment.dto.SegmentResponse;
import com.crm.platform.segment.dto.SegmentUpdateRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

/**
 * SCR-06: Segment Builder View providing visual AST canvas, live AST preview, and AI rule synthesis.
 */
@Route(value = "segments/new", layout = MainLayout.class)
@RouteAlias(value = "segments/:id/edit", layout = MainLayout.class)
@PageTitle("Segment Studio | Enterprise AI-CRM")
public class SegmentBuilderView extends VerticalLayout implements BeforeEnterObserver {

    private final SegmentApiService segmentApiService;
    private final AiApiService aiApiService;
    private final StateViewContainer stateContainer;

    private Long segmentId;
    private final TextField nameField = new TextField("Segment Name");
    private final TextArea descField = new TextArea("Description");
    private final RuleTreeContainer ruleTreeContainer = new RuleTreeContainer();
    private final HorizontalLayout aiStatusBanner = new HorizontalLayout();

    public SegmentBuilderView(SegmentApiService segmentApiService, AiApiService aiApiService) {
        this.segmentApiService = segmentApiService;
        this.aiApiService = aiApiService;

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
        if (idParam != null) {
            try {
                this.segmentId = Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                stateContainer.showError("Invalid Segment ID: " + idParam, () -> getUI().ifPresent(ui -> ui.navigate("segments")));
                return;
            }
        } else {
            this.segmentId = null;
        }

        buildUi();
    }

    private void buildUi() {
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
        boolean isEdit = segmentId != null;
        H2 title = new H2(isEdit ? "Edit Audience Segment #" + segmentId : "Create Audience Segment");
        title.addClassName("crm-view-title");
        Paragraph sub = new Paragraph("Build recursive AND/OR condition trees. Criteria will match customers during evaluation.");
        sub.addClassName("crm-view-subtitle");
        headerLeft.add(title, sub);

        HorizontalLayout actionButtons = new HorizontalLayout();

        Button aiButton = new Button("Generate with AI", VaadinIcon.MAGIC.create(), e -> {
            AiSegmentModal modal = new AiSegmentModal(aiApiService, (rules, isFallback) -> {
                ruleTreeContainer.populateFromJson(rules);
                aiStatusBanner.removeAll();
                if (isFallback) {
                    aiStatusBanner.add(new StatusBadge("FALLBACK"));
                    Paragraph fallbackNote = new Paragraph("Rules generated via deterministic rule engine fallback.");
                    fallbackNote.getStyle().set("color", "var(--crm-status-warning-base)").set("font-size", "12px");
                    aiStatusBanner.add(fallbackNote);
                    aiStatusBanner.setVisible(true);
                } else {
                    aiStatusBanner.add(new StatusBadge("ACTIVE"));
                    Paragraph aiNote = new Paragraph("Rules synthesized by Gemini AI model.");
                    aiNote.getStyle().set("color", "var(--crm-status-success-base)").set("font-size", "12px");
                    aiStatusBanner.add(aiNote);
                    aiStatusBanner.setVisible(true);
                }
            });
            modal.open();
        });
        aiButton.addClassName("crm-btn-secondary");

        Button saveBtn = new Button("Save Segment", e -> saveSegment(false));
        saveBtn.addClassName("crm-btn-secondary");

        Button saveAndPreviewBtn = new Button("Save & Preview Audience", VaadinIcon.EYE.create(), e -> saveSegment(true));
        saveAndPreviewBtn.addClassName("crm-btn-primary");

        actionButtons.add(aiButton, saveBtn, saveAndPreviewBtn);
        header.add(headerLeft, actionButtons);
        content.add(header);

        aiStatusBanner.setVisible(false);
        aiStatusBanner.setAlignItems(FlexComponent.Alignment.CENTER);
        aiStatusBanner.addClassName("crm-panel");
        content.add(aiStatusBanner);

        // Metadata Panel
        Div metadataPanel = new Div();
        metadataPanel.addClassName("crm-panel");
        metadataPanel.setWidthFull();

        H3 metaTitle = new H3("Segment Identity");
        metaTitle.addClassName("crm-panel-header");

        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setPlaceholder("e.g. High-Value Metro Customers");

        descField.setWidthFull();
        descField.setHeight("80px");
        descField.setPlaceholder("Describe the target marketing audience...");

        metadataPanel.add(metaTitle, nameField, descField);
        content.add(metadataPanel);

        // Visual AST Builder Panel
        Div builderPanel = new Div();
        builderPanel.addClassName("crm-panel");
        builderPanel.setWidthFull();

        H3 builderTitle = new H3("Criteria Rule Builder (AST Canvas)");
        builderTitle.addClassName("crm-panel-header");

        builderPanel.add(builderTitle, ruleTreeContainer);
        content.add(builderPanel);

        stateContainer.setContent(content);

        if (segmentId != null) {
            loadExistingSegment();
        }
    }

    private void loadExistingSegment() {
        try {
            SegmentResponse segment = segmentApiService.getSegmentById(segmentId);
            if (segment != null) {
                nameField.setValue(segment.getName() != null ? segment.getName() : "");
                descField.setValue(segment.getDescription() != null ? segment.getDescription() : "");
                if (segment.getRules() != null) {
                    ruleTreeContainer.populateFromJson(segment.getRules());
                }
            }
        } catch (ApiException apiEx) {
            NotificationUtils.showError(apiEx.getUserFriendlyMessage());
        } catch (Exception ex) {
            NotificationUtils.showError("Failed to load segment: " + ex.getMessage());
        }
    }

    private void saveSegment(boolean andPreview) {
        String name = nameField.getValue();
        if (name == null || name.trim().isEmpty()) {
            NotificationUtils.showError("Segment name is required.");
            return;
        }

        ObjectNode astJson = ruleTreeContainer.getAstJson();
        if (astJson == null) {
            NotificationUtils.showError("Please specify valid criteria. All condition values must be completed.");
            return;
        }

        try {
            SegmentResponse result;
            if (segmentId == null) {
                SegmentCreateRequest req = new SegmentCreateRequest(name.trim(), descField.getValue(), astJson);
                result = segmentApiService.createSegment(req);
                NotificationUtils.showSuccess("Segment created successfully.");
            } else {
                SegmentUpdateRequest req = new SegmentUpdateRequest(name.trim(), descField.getValue(), astJson);
                result = segmentApiService.updateSegment(segmentId, req);
                NotificationUtils.showSuccess("Segment updated successfully.");
            }

            if (andPreview) {
                getUI().ifPresent(ui -> ui.navigate("segments/" + result.getId() + "/members"));
            } else {
                getUI().ifPresent(ui -> ui.navigate("segments"));
            }
        } catch (ApiException apiEx) {
            NotificationUtils.showError(apiEx.getUserFriendlyMessage());
        } catch (Exception ex) {
            NotificationUtils.showError("Failed to save segment: " + ex.getMessage());
        }
    }
}
