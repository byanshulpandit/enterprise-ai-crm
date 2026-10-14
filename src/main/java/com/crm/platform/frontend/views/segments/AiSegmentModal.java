package com.crm.platform.frontend.views.segments;

import com.crm.platform.ai.dto.AiRuleGenerationResponse;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.service.AiApiService;
import com.crm.platform.frontend.util.NotificationUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.function.BiConsumer;

/**
 * Natural language AI rule generation modal dialog.
 */
public class AiSegmentModal extends Dialog {

    private final AiApiService aiApiService;
    private final BiConsumer<JsonNode, Boolean> onRulesGenerated;
    private final TextArea promptArea;
    private final ProgressBar progressBar;
    private final HorizontalLayout alertBox;

    public AiSegmentModal(AiApiService aiApiService, BiConsumer<JsonNode, Boolean> onRulesGenerated) {
        this.aiApiService = aiApiService;
        this.onRulesGenerated = onRulesGenerated;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setWidth("560px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("AI Natural Language Segment Generator");
        title.getStyle().set("margin", "0 0 6px 0").set("color", "var(--crm-text-primary)");

        Paragraph desc = new Paragraph("Describe the target customer criteria in plain language. The AI engine will parse your description into a strongly-typed AST rule tree.");
        desc.getStyle().set("font-size", "13px").set("color", "var(--crm-text-secondary)").set("margin", "0 0 12px 0");

        promptArea = new TextArea("Natural Language Prompt");
        promptArea.setWidthFull();
        promptArea.setHeight("110px");
        promptArea.setPlaceholder("e.g. Customers living in Delhi or Mumbai who spent at least ₹10,000 and have visited at least 5 times");

        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidthFull();
        progressBar.setVisible(false);

        alertBox = new HorizontalLayout();
        alertBox.setVisible(false);
        alertBox.setSpacing(true);
        alertBox.setAlignItems(FlexComponent.Alignment.CENTER);

        Button cancelBtn = new Button("Cancel", e -> close());
        cancelBtn.addClassName("crm-btn-secondary");

        Button generateBtn = new Button("Generate Rules", VaadinIcon.MAGIC.create(), e -> generateRules());
        generateBtn.addClassName("crm-btn-primary");

        HorizontalLayout actions = new HorizontalLayout(cancelBtn, generateBtn);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        layout.add(title, desc, promptArea, progressBar, alertBox, actions);
        add(layout);
    }

    private void generateRules() {
        String prompt = promptArea.getValue();
        if (prompt == null || prompt.trim().isEmpty()) {
            NotificationUtils.showError("Please enter a natural language prompt.");
            return;
        }

        progressBar.setVisible(true);
        alertBox.setVisible(false);

        try {
            AiRuleGenerationResponse response = aiApiService.generateSegmentRules(prompt.trim());
            progressBar.setVisible(false);

            if (response != null && response.getRuleTree() != null) {
                if (response.isFallback()) {
                    NotificationUtils.showWarning("AI service unavailable: deterministic fallback rules applied.");
                } else {
                    NotificationUtils.showSuccess("AI successfully generated segment rules.");
                }

                if (onRulesGenerated != null) {
                    onRulesGenerated.accept(response.getRuleTree(), response.isFallback());
                }
                close();
            } else {
                NotificationUtils.showError("Failed to parse rule tree from AI response.");
            }
        } catch (ApiException apiEx) {
            progressBar.setVisible(false);
            NotificationUtils.showError(apiEx.getUserFriendlyMessage());
        } catch (Exception ex) {
            progressBar.setVisible(false);
            NotificationUtils.showError("AI generation failed: " + ex.getMessage());
        }
    }
}
