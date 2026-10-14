package com.crm.platform.frontend.components.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Top-level container managing the visual AST rule tree canvas and live JSON AST preview.
 */
public class RuleTreeContainer extends VerticalLayout {

    private final RuleGroupComponent rootGroup;
    private final Pre jsonPreviewPre;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RuleTreeContainer() {
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        rootGroup = new RuleGroupComponent(1, true, null, this::updateJsonPreview);

        jsonPreviewPre = new Pre();
        jsonPreviewPre.addClassName("crm-code-box");
        jsonPreviewPre.setWidthFull();

        Details previewDetails = new Details("Live AST JSON Preview", jsonPreviewPre);
        previewDetails.setWidthFull();
        previewDetails.setOpened(false);

        add(rootGroup, previewDetails);

        updateJsonPreview();
    }

    private void updateJsonPreview() {
        ObjectNode json = rootGroup.toAstJson();
        if (json != null) {
            try {
                jsonPreviewPre.setText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
            } catch (Exception e) {
                jsonPreviewPre.setText(json.toString());
            }
        } else {
            jsonPreviewPre.setText("{\n  /* Incomplete rule tree: please complete all condition values */\n}");
        }
    }

    public ObjectNode getAstJson() {
        return rootGroup.toAstJson();
    }

    public boolean isValid() {
        return getAstJson() != null;
    }

    public void populateFromJson(JsonNode node) {
        rootGroup.populateFromJson(node);
        updateJsonPreview();
    }
}
