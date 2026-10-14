package com.crm.platform.frontend.components.builder;

import com.crm.platform.segment.model.RuleField;
import com.crm.platform.segment.model.RuleOperator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visual editor component for a leaf AST condition node (field, operator, value).
 */
public class ConditionRowComponent extends HorizontalLayout {

    private final Select<RuleField> fieldSelect;
    private final Select<RuleOperator> operatorSelect;
    private final Div valueContainer;
    private Component valueField;
    private final Runnable onChange;

    public ConditionRowComponent(Consumer<ConditionRowComponent> onRemove, Runnable onChange) {
        this.onChange = onChange;
        addClassName("crm-condition-row");
        setAlignItems(FlexComponent.Alignment.CENTER);
        setWidthFull();

        fieldSelect = new Select<>();
        fieldSelect.setWidth("170px");
        fieldSelect.setItems(RuleField.values());
        fieldSelect.setItemLabelGenerator(RuleField::getJsonName);
        fieldSelect.setPlaceholder("Select Field");

        operatorSelect = new Select<>();
        operatorSelect.setWidth("200px");
        operatorSelect.setItemLabelGenerator(Enum::name);
        operatorSelect.setPlaceholder("Select Operator");

        valueContainer = new Div();
        valueContainer.getStyle().set("display", "inline-flex");

        fieldSelect.addValueChangeListener(e -> {
            updateOperatorOptions();
            rebuildValueField();
            notifyChange();
        });

        operatorSelect.addValueChangeListener(e -> {
            rebuildValueField();
            notifyChange();
        });

        // Default to CITY and EQUALS
        fieldSelect.setValue(RuleField.CITY);
        updateOperatorOptions();
        operatorSelect.setValue(RuleOperator.EQUALS);
        rebuildValueField();

        Button removeButton = new Button("✕", e -> onRemove.accept(this));
        removeButton.addClassName("crm-btn-danger");
        removeButton.getStyle().set("padding", "0 10px").set("min-width", "32px");
        removeButton.getElement().setAttribute("title", "Remove condition");

        add(fieldSelect, operatorSelect, valueContainer, removeButton);
    }

    private void updateOperatorOptions() {
        RuleField selectedField = fieldSelect.getValue();
        if (selectedField != null) {
            List<RuleOperator> operators = Arrays.stream(RuleOperator.values())
                    .filter(selectedField::supportsOperator)
                    .toList();
            operatorSelect.setItems(operators);
            if (!operators.contains(operatorSelect.getValue())) {
                operatorSelect.setValue(operators.isEmpty() ? null : operators.get(0));
            }
        } else {
            operatorSelect.setItems();
        }
    }

    private void rebuildValueField() {
        valueContainer.removeAll();
        RuleField field = fieldSelect.getValue();
        RuleOperator operator = operatorSelect.getValue();
        valueField = ValueFieldFactory.createInputField(field, operator);
        if (valueField instanceof HasValue) {
            ((HasValue<?, ?>) valueField).addValueChangeListener(e -> notifyChange());
        }
        valueContainer.add(valueField);
    }

    private void notifyChange() {
        if (onChange != null) {
            onChange.run();
        }
    }

    public ObjectNode toAstJson() {
        RuleField field = fieldSelect.getValue();
        RuleOperator operator = operatorSelect.getValue();
        if (field == null || operator == null) {
            return null;
        }

        JsonNode val = ValueFieldFactory.extractJsonValue(field, operator, valueField);
        if (val == null) {
            return null;
        }

        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("field", field.getJsonName());
        node.put("op", operator.name());
        node.set("value", val);
        return node;
    }

    public void populateFromJson(JsonNode node) {
        if (node == null || !node.has("field")) {
            return;
        }

        String fieldStr = node.get("field").asText();
        RuleField field = RuleField.fromString(fieldStr);
        fieldSelect.setValue(field);
        updateOperatorOptions();

        String opStr = node.has("op") ? node.get("op").asText() : node.get("operator").asText();
        RuleOperator operator = RuleOperator.fromString(opStr);
        operatorSelect.setValue(operator);

        rebuildValueField();
        ValueFieldFactory.setInputValue(valueField, node.get("value"));
    }
}
