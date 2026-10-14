package com.crm.platform.frontend.components.builder;

import com.crm.platform.segment.model.LogicalOperator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visual composite component for an AST logical group (AND/OR container with nested rules).
 */
public class RuleGroupComponent extends Div {

    public static final int MAX_NESTING_DEPTH = 10;

    private final int depth;
    private final Select<LogicalOperator> operatorSelect;
    private final VerticalLayout childrenContainer;
    private final List<ConditionRowComponent> conditionRows = new ArrayList<>();
    private final List<RuleGroupComponent> childGroups = new ArrayList<>();
    private final Runnable onChange;

    public RuleGroupComponent(int depth, boolean isRoot, Consumer<RuleGroupComponent> onRemoveGroup, Runnable onChange) {
        this.depth = depth;
        this.onChange = onChange;

        addClassName("crm-rule-group-box");
        if (!isRoot) {
            addClassName("crm-tree-indent");
        }

        HorizontalLayout groupHeader = new HorizontalLayout();
        groupHeader.setWidthFull();
        groupHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        groupHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout leftControls = new HorizontalLayout();
        leftControls.setAlignItems(FlexComponent.Alignment.CENTER);

        Span label = new Span(isRoot ? "MATCH CONDITIONS:" : "SUBGROUP (Depth " + depth + "):");
        label.getStyle().set("font-size", "12px").set("font-weight", "600").set("color", "var(--crm-text-secondary)");

        operatorSelect = new Select<>();
        operatorSelect.setWidth("150px");
        operatorSelect.setItems(LogicalOperator.values());
        operatorSelect.setItemLabelGenerator(op -> op == LogicalOperator.AND ? "ALL (AND)" : "ANY (OR)");
        operatorSelect.setValue(LogicalOperator.AND);
        operatorSelect.addValueChangeListener(e -> notifyChange());

        leftControls.add(label, operatorSelect);

        HorizontalLayout rightControls = new HorizontalLayout();
        rightControls.setAlignItems(FlexComponent.Alignment.CENTER);

        Button addConditionBtn = new Button("+ Condition", e -> addConditionRow());
        addConditionBtn.addClassName("crm-btn-secondary");
        addConditionBtn.getStyle().set("font-size", "12px");

        Button addGroupBtn = new Button("+ Subgroup", e -> addSubgroup());
        addGroupBtn.addClassName("crm-btn-secondary");
        addGroupBtn.getStyle().set("font-size", "12px");
        if (depth >= MAX_NESTING_DEPTH) {
            addGroupBtn.setEnabled(false);
            addGroupBtn.getElement().setAttribute("title", "Maximum nesting depth reached (" + MAX_NESTING_DEPTH + ")");
        }

        rightControls.add(addConditionBtn, addGroupBtn);

        if (!isRoot && onRemoveGroup != null) {
            Button removeGroupBtn = new Button("Remove Group", e -> onRemoveGroup.accept(this));
            removeGroupBtn.addClassName("crm-btn-danger");
            removeGroupBtn.getStyle().set("font-size", "12px");
            rightControls.add(removeGroupBtn);
        }

        groupHeader.add(leftControls, rightControls);

        childrenContainer = new VerticalLayout();
        childrenContainer.setPadding(false);
        childrenContainer.setSpacing(true);
        childrenContainer.setWidthFull();

        add(groupHeader, childrenContainer);

        // Add 1 default condition if root
        if (isRoot) {
            addConditionRow();
        }
    }

    public void addConditionRow() {
        ConditionRowComponent row = new ConditionRowComponent(this::removeConditionRow, onChange);
        conditionRows.add(row);
        childrenContainer.add(row);
        notifyChange();
    }

    public void removeConditionRow(ConditionRowComponent row) {
        conditionRows.remove(row);
        childrenContainer.remove(row);
        notifyChange();
    }

    public void addSubgroup() {
        if (depth >= MAX_NESTING_DEPTH) {
            return;
        }
        RuleGroupComponent sub = new RuleGroupComponent(depth + 1, false, this::removeSubgroup, onChange);
        sub.addConditionRow();
        childGroups.add(sub);
        childrenContainer.add(sub);
        notifyChange();
    }

    public void removeSubgroup(RuleGroupComponent sub) {
        childGroups.remove(sub);
        childrenContainer.remove(sub);
        notifyChange();
    }

    private void notifyChange() {
        if (onChange != null) {
            onChange.run();
        }
    }

    public ObjectNode toAstJson() {
        ArrayNode conditionsArray = JsonNodeFactory.instance.arrayNode();

        for (ConditionRowComponent row : conditionRows) {
            ObjectNode conditionJson = row.toAstJson();
            if (conditionJson != null) {
                conditionsArray.add(conditionJson);
            }
        }

        for (RuleGroupComponent group : childGroups) {
            ObjectNode groupJson = group.toAstJson();
            if (groupJson != null && groupJson.has("conditions") && !groupJson.get("conditions").isEmpty()) {
                conditionsArray.add(groupJson);
            }
        }

        if (conditionsArray.isEmpty()) {
            return null;
        }

        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("operator", operatorSelect.getValue().name());
        node.set("conditions", conditionsArray);
        return node;
    }

    public void clear() {
        conditionRows.clear();
        childGroups.clear();
        childrenContainer.removeAll();
    }

    public void populateFromJson(JsonNode node) {
        clear();
        if (node == null || !node.has("conditions")) {
            return;
        }

        if (node.has("operator")) {
            try {
                operatorSelect.setValue(LogicalOperator.valueOf(node.get("operator").asText().toUpperCase()));
            } catch (Exception ignored) {
                operatorSelect.setValue(LogicalOperator.AND);
            }
        }

        JsonNode conditionsNode = node.get("conditions");
        if (conditionsNode.isArray()) {
            for (JsonNode child : conditionsNode) {
                if (child.has("conditions")) {
                    if (depth < MAX_NESTING_DEPTH) {
                        RuleGroupComponent sub = new RuleGroupComponent(depth + 1, false, this::removeSubgroup, onChange);
                        sub.populateFromJson(child);
                        childGroups.add(sub);
                        childrenContainer.add(sub);
                    }
                } else if (child.has("field")) {
                    ConditionRowComponent row = new ConditionRowComponent(this::removeConditionRow, onChange);
                    row.populateFromJson(child);
                    conditionRows.add(row);
                    childrenContainer.add(row);
                }
            }
        }

        if (conditionRows.isEmpty() && childGroups.isEmpty()) {
            addConditionRow();
        }
    }
}
