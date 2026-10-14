package com.crm.platform.frontend.views.campaigns;

import com.crm.platform.campaign.dto.CampaignLaunchResponse;
import com.crm.platform.campaign.dto.CampaignResponse;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.service.CampaignApiService;
import com.crm.platform.frontend.service.SegmentApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.frontend.util.NotificationUtils;
import com.crm.platform.segment.dto.SegmentPreviewResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Pre-flight safety confirmation modal preventing zero-audience and accidental campaign dispatches.
 */
public class CampaignLaunchDialog extends Dialog {

    private final CampaignApiService campaignApiService;
    private final SegmentApiService segmentApiService;
    private final CampaignResponse campaign;
    private final Runnable onLaunched;

    public CampaignLaunchDialog(CampaignApiService campaignApiService, SegmentApiService segmentApiService, CampaignResponse campaign, Runnable onLaunched) {
        this.campaignApiService = campaignApiService;
        this.segmentApiService = segmentApiService;
        this.campaign = campaign;
        this.onLaunched = onLaunched;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setWidth("550px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("Pre-Flight Campaign Launch Safety Check");
        title.getStyle().set("margin", "0 0 8px 0").set("color", "var(--crm-text-primary)");

        Paragraph intro = new Paragraph("Review target audience sizing and delivery parameters before committing campaign dispatch to workers.");
        intro.getStyle().set("font-size", "13px").set("color", "var(--crm-text-secondary)").set("margin", "0 0 12px 0");

        Div detailsBox = new Div();
        detailsBox.addClassName("crm-panel");
        detailsBox.setWidthFull();

        long audienceSize = 0L;
        if (campaign.getSegmentId() != null) {
            try {
                SegmentPreviewResponse preview = segmentApiService.previewSegment(campaign.getSegmentId());
                if (preview != null && preview.getMatchedAudienceCount() != null) {
                    audienceSize = preview.getMatchedAudienceCount();
                }
            } catch (Exception ignored) {
            }
        }

        VerticalLayout rows = new VerticalLayout();
        rows.setPadding(false);
        rows.setSpacing(true);

        rows.add(createRow("Campaign Name", campaign.getName()));
        rows.add(createRow("Target Segment", campaign.getSegmentName() != null ? campaign.getSegmentName() : "#" + campaign.getSegmentId()));
        rows.add(createRow("Evaluated Audience", FormattingUtils.formatInteger(audienceSize) + " recipients"));
        rows.add(createRow("Personalization", campaign.getPersonalizationEnabled() != null && campaign.getPersonalizationEnabled() ? "Enabled (First Name / City tokens)" : "Disabled"));

        detailsBox.add(rows);

        Checkbox confirmCheck = new Checkbox("I confirm that this message is approved for immediate external dispatch.");
        confirmCheck.setWidthFull();

        Button cancelBtn = new Button("Cancel", e -> close());
        cancelBtn.addClassName("crm-btn-secondary");

        Button launchBtn = new Button("Commit Launch", VaadinIcon.PAPERPLANE.create(), e -> launch());
        launchBtn.addClassName("crm-btn-primary");
        launchBtn.setEnabled(false);

        if (audienceSize == 0) {
            Div errorWarning = new Div();
            errorWarning.addClassName("crm-status-pill");
            errorWarning.addClassName("crm-status-error");
            errorWarning.setWidthFull();
            errorWarning.add(new Span("BLOCKED: Target segment has 0 matching customers. Campaigns cannot be dispatched to empty audiences."));
            layout.add(title, intro, detailsBox, errorWarning, cancelBtn);
        } else {
            confirmCheck.addValueChangeListener(e -> launchBtn.setEnabled(Boolean.TRUE.equals(e.getValue())));

            HorizontalLayout actions = new HorizontalLayout(cancelBtn, launchBtn);
            actions.setWidthFull();
            actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

            layout.add(title, intro, detailsBox, confirmCheck, actions);
        }

        add(layout);
    }

    private void launch() {
        try {
            CampaignLaunchResponse response = campaignApiService.launchCampaign(campaign.getId());
            NotificationUtils.showSuccess("Campaign dispatched successfully! Processing outbox delivery records.");
            close();
            if (onLaunched != null) {
                onLaunched.run();
            }
            UI.getCurrent().navigate("campaigns/" + campaign.getId() + "/monitor");
        } catch (ApiException apiEx) {
            NotificationUtils.showError(apiEx.getUserFriendlyMessage());
        } catch (Exception ex) {
            NotificationUtils.showError("Failed to launch campaign: " + ex.getMessage());
        }
    }

    private HorizontalLayout createRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span l = new Span(label);
        l.getStyle().set("font-size", "12px").set("color", "var(--crm-text-secondary)");

        Span v = new Span(value);
        v.getStyle().set("font-size", "13px").set("font-weight", "600").set("color", "var(--crm-text-primary)");

        row.add(l, v);
        return row;
    }
}
