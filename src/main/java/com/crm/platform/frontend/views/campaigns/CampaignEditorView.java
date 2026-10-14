package com.crm.platform.frontend.views.campaigns;

import com.crm.platform.campaign.dto.CampaignCreateRequest;
import com.crm.platform.campaign.dto.CampaignResponse;
import com.crm.platform.campaign.dto.CampaignUpdateRequest;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.CampaignApiService;
import com.crm.platform.frontend.service.SegmentApiService;
import com.crm.platform.frontend.util.NotificationUtils;
import com.crm.platform.segment.dto.SegmentResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.util.List;

/**
 * SCR-09: Campaign Authoring Studio with token insertion and live personalized template preview.
 */
@Route(value = "campaigns/new", layout = MainLayout.class)
@RouteAlias(value = "campaigns/:id/edit", layout = MainLayout.class)
@PageTitle("Campaign Studio | Enterprise AI-CRM")
public class CampaignEditorView extends VerticalLayout implements BeforeEnterObserver {

    private final CampaignApiService campaignApiService;
    private final SegmentApiService segmentApiService;
    private final StateViewContainer stateContainer;

    private Long campaignId;
    private final TextField nameField = new TextField("Campaign Name");
    private final TextArea descField = new TextArea("Description");
    private final Select<SegmentResponse> segmentSelect = new Select<>();
    private final Checkbox personalizationCheck = new Checkbox("Enable customer personalization tokens");
    private final TextArea templateField = new TextArea("Message Template");
    private final Div previewBox = new Div();

    public CampaignEditorView(CampaignApiService campaignApiService, SegmentApiService segmentApiService) {
        this.campaignApiService = campaignApiService;
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
        if (idParam != null) {
            try {
                this.campaignId = Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                stateContainer.showError("Invalid Campaign ID: " + idParam, () -> getUI().ifPresent(ui -> ui.navigate("campaigns")));
                return;
            }
        } else {
            this.campaignId = null;
        }

        buildUi();
    }

    private void buildUi() {
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
        boolean isEdit = campaignId != null;
        H2 title = new H2(isEdit ? "Edit Campaign #" + campaignId : "Author New Marketing Campaign");
        title.addClassName("crm-view-title");
        Paragraph sub = new Paragraph("Select target audience, craft notification copy, and configure personalized tokens.");
        sub.addClassName("crm-view-subtitle");
        headerLeft.add(title, sub);

        HorizontalLayout actions = new HorizontalLayout();

        Button saveDraftBtn = new Button("Save Draft", e -> saveCampaign(false));
        saveDraftBtn.addClassName("crm-btn-secondary");

        Button saveAndLaunchBtn = new Button("Save & Review Launch", VaadinIcon.PAPERPLANE.create(), e -> saveCampaign(true));
        saveAndLaunchBtn.addClassName("crm-btn-primary");

        actions.add(saveDraftBtn, saveAndLaunchBtn);
        header.add(headerLeft, actions);
        content.add(header);

        // Form Panels
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);

        Div formPanel = new Div();
        formPanel.addClassName("crm-panel");
        formPanel.setWidth("55%");

        H3 formTitle = new H3("Campaign Parameters");
        formTitle.addClassName("crm-panel-header");

        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setPlaceholder("e.g. Diwali Mega Fest VIP Promotion");

        descField.setWidthFull();
        descField.setHeight("70px");
        descField.setPlaceholder("Optional internal dispatch notes...");

        segmentSelect.setLabel("Target Audience Segment");
        segmentSelect.setWidthFull();
        segmentSelect.setItemLabelGenerator(SegmentResponse::getName);
        segmentSelect.setPlaceholder("Choose a segment...");

        loadSegments();

        personalizationCheck.setWidthFull();
        personalizationCheck.addValueChangeListener(e -> updatePreview());

        // Token Insertion Buttons
        Span tokenLabel = new Span("Personalization Tokens (click to insert):");
        tokenLabel.getStyle().set("font-size", "11px").set("color", "var(--crm-text-secondary)").set("display", "block").set("margin-top", "8px");

        HorizontalLayout tokenBar = new HorizontalLayout();
        tokenBar.setSpacing(true);
        tokenBar.add(
                createTokenBtn("{firstName}"),
                createTokenBtn("{lastName}"),
                createTokenBtn("{city}"),
                createTokenBtn("{totalSpend}")
        );

        templateField.setWidthFull();
        templateField.setHeight("120px");
        templateField.setRequired(true);
        templateField.setMaxLength(1000);
        templateField.setPlaceholder("e.g. Hi {firstName}, your ₹{totalSpend} milestone unlocks 15% discount in {city}!");
        templateField.addValueChangeListener(e -> updatePreview());

        formPanel.add(formTitle, nameField, descField, segmentSelect, personalizationCheck, tokenLabel, tokenBar, templateField);

        // Live Preview Panel
        Div previewPanel = new Div();
        previewPanel.addClassName("crm-panel");
        previewPanel.setWidth("45%");

        H3 previewTitle = new H3("Sample Recipient Message Preview");
        previewTitle.addClassName("crm-panel-header");

        previewBox.addClassName("crm-code-box");
        previewBox.getStyle().set("white-space", "pre-wrap").set("min-height", "180px");

        Paragraph sampleNotice = new Paragraph("Preview simulated for customer: John Doe, Delhi, ₹12,500 spend.");
        sampleNotice.getStyle().set("font-size", "11px").set("color", "var(--crm-text-muted)").set("margin-top", "8px");

        previewPanel.add(previewTitle, previewBox, sampleNotice);

        row.add(formPanel, previewPanel);
        content.add(row);

        stateContainer.setContent(content);

        updatePreview();

        if (campaignId != null) {
            loadExistingCampaign();
        }
    }

    private Button createTokenBtn(String token) {
        Button btn = new Button(token, e -> {
            String current = templateField.getValue() != null ? templateField.getValue() : "";
            templateField.setValue(current + token);
            updatePreview();
        });
        btn.addClassName("crm-btn-secondary");
        btn.getStyle().set("font-size", "11px").set("padding", "2px 8px").set("font-family", "var(--lumo-font-family-monospace)");
        return btn;
    }

    private void updatePreview() {
        String template = templateField.getValue();
        if (template == null || template.isBlank()) {
            previewBox.setText("Message template is empty.");
            return;
        }

        if (Boolean.TRUE.equals(personalizationCheck.getValue())) {
            String resolved = template
                    .replace("{firstName}", "John")
                    .replace("{lastName}", "Doe")
                    .replace("{city}", "Delhi")
                    .replace("{totalSpend}", "12,500.00");
            previewBox.setText(resolved);
        } else {
            previewBox.setText(template);
        }
    }

    private void loadSegments() {
        try {
            PageResult<SegmentResponse> result = segmentApiService.listSegments(0, 100);
            List<SegmentResponse> segments = result.getContent();
            segmentSelect.setItems(segments);
        } catch (Exception ex) {
            NotificationUtils.showError("Failed to load audience segments: " + ex.getMessage());
        }
    }

    private void loadExistingCampaign() {
        try {
            CampaignResponse campaign = campaignApiService.getCampaignById(campaignId);
            if (campaign != null) {
                nameField.setValue(campaign.getName() != null ? campaign.getName() : "");
                descField.setValue(campaign.getDescription() != null ? campaign.getDescription() : "");
                if (campaign.getPersonalizationEnabled() != null) {
                    personalizationCheck.setValue(campaign.getPersonalizationEnabled());
                }
                templateField.setValue(campaign.getMessageTemplate() != null ? campaign.getMessageTemplate() : "");

                if (campaign.getSegmentId() != null) {
                    segmentSelect.getListDataView().getItems()
                            .filter(s -> s.getId().equals(campaign.getSegmentId()))
                            .findFirst()
                            .ifPresent(segmentSelect::setValue);
                }
                updatePreview();
            }
        } catch (ApiException apiEx) {
            NotificationUtils.showError(apiEx.getUserFriendlyMessage());
        } catch (Exception ex) {
            NotificationUtils.showError("Failed to load campaign: " + ex.getMessage());
        }
    }

    private void saveCampaign(boolean andLaunch) {
        String name = nameField.getValue();
        if (name == null || name.trim().isEmpty()) {
            NotificationUtils.showError("Campaign name is required.");
            return;
        }

        SegmentResponse selectedSegment = segmentSelect.getValue();
        if (selectedSegment == null) {
            NotificationUtils.showError("Please select a target audience segment.");
            return;
        }

        String template = templateField.getValue();
        if (template == null || template.trim().isEmpty()) {
            NotificationUtils.showError("Message template is required.");
            return;
        }

        try {
            CampaignResponse saved;
            if (campaignId == null) {
                CampaignCreateRequest req = new CampaignCreateRequest(
                        name.trim(),
                        descField.getValue(),
                        selectedSegment.getId(),
                        template.trim(),
                        personalizationCheck.getValue()
                );
                saved = campaignApiService.createCampaign(req);
                NotificationUtils.showSuccess("Campaign draft created.");
            } else {
                CampaignUpdateRequest req = new CampaignUpdateRequest(
                        name.trim(),
                        descField.getValue(),
                        selectedSegment.getId(),
                        template.trim(),
                        personalizationCheck.getValue()
                );
                saved = campaignApiService.updateCampaign(campaignId, req);
                NotificationUtils.showSuccess("Campaign updated.");
            }

            if (andLaunch) {
                CampaignLaunchDialog dialog = new CampaignLaunchDialog(
                        campaignApiService, segmentApiService, saved,
                        () -> getUI().ifPresent(ui -> ui.navigate("campaigns/" + saved.getId() + "/monitor"))
                );
                dialog.open();
            } else {
                getUI().ifPresent(ui -> ui.navigate("campaigns"));
            }
        } catch (ApiException apiEx) {
            NotificationUtils.showError(apiEx.getUserFriendlyMessage());
        } catch (Exception ex) {
            NotificationUtils.showError("Failed to save campaign: " + ex.getMessage());
        }
    }
}
