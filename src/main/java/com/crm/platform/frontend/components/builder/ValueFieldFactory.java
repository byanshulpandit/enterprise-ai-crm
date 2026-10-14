package com.crm.platform.frontend.components.builder;

import com.crm.platform.segment.model.RuleField;
import com.crm.platform.segment.model.RuleOperator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory creating type-appropriate Vaadin input components based on RuleField and RuleOperator.
 */
public final class ValueFieldFactory {

    private ValueFieldFactory() {
    }

    public static Component createInputField(RuleField field, RuleOperator operator) {
        if (operator == RuleOperator.IN || operator == RuleOperator.NOT_IN) {
            TextField tf = new TextField();
            tf.setPlaceholder("Comma-separated values (e.g. Delhi, Mumbai)");
            tf.setWidth("240px");
            return tf;
        }

        if (field == null) {
            TextField tf = new TextField();
            tf.setWidth("180px");
            return tf;
        }

        switch (field) {
            case TOTAL_SPEND -> {
                BigDecimalField bdf = new BigDecimalField();
                bdf.setPlaceholder("0.00");
                bdf.setWidth("150px");
                return bdf;
            }
            case VISIT_COUNT -> {
                IntegerField inf = new IntegerField();
                inf.setPlaceholder("0");
                inf.setMin(0);
                inf.setWidth("120px");
                return inf;
            }
            case LAST_ACTIVE_DATE -> {
                DatePicker dp = new DatePicker();
                dp.setPlaceholder("YYYY-MM-DD");
                dp.setWidth("160px");
                return dp;
            }
            default -> {
                TextField tf = new TextField();
                tf.setPlaceholder("Enter text");
                tf.setWidth("180px");
                return tf;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static JsonNode extractJsonValue(RuleField field, RuleOperator operator, Component inputComponent) {
        if (inputComponent instanceof HasValue) {
            Object rawVal = ((HasValue<?, ?>) inputComponent).getValue();
            if (rawVal == null) {
                return null;
            }

            if (operator == RuleOperator.IN || operator == RuleOperator.NOT_IN) {
                String str = rawVal.toString().trim();
                if (str.isEmpty()) return null;
                String[] parts = str.split(",");
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                for (String part : parts) {
                    String clean = part.trim();
                    if (!clean.isEmpty()) {
                        if (field == RuleField.TOTAL_SPEND) {
                            try {
                                arrayNode.add(new BigDecimal(clean));
                            } catch (Exception e) {
                                arrayNode.add(clean);
                            }
                        } else if (field == RuleField.VISIT_COUNT) {
                            try {
                                arrayNode.add(Integer.parseInt(clean));
                            } catch (Exception e) {
                                arrayNode.add(clean);
                            }
                        } else {
                            arrayNode.add(clean);
                        }
                    }
                }
                return arrayNode;
            }

            if (rawVal instanceof BigDecimal bd) {
                return JsonNodeFactory.instance.numberNode(bd);
            } else if (rawVal instanceof Integer in) {
                return JsonNodeFactory.instance.numberNode(in);
            } else if (rawVal instanceof LocalDate ld) {
                return JsonNodeFactory.instance.textNode(ld.toString());
            } else {
                String str = rawVal.toString().trim();
                return str.isEmpty() ? null : JsonNodeFactory.instance.textNode(str);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void setInputValue(Component inputComponent, JsonNode valueNode) {
        if (inputComponent == null || valueNode == null || valueNode.isNull()) {
            return;
        }

        if (inputComponent instanceof BigDecimalField bdf) {
            if (valueNode.isNumber()) {
                bdf.setValue(BigDecimal.valueOf(valueNode.asDouble()));
            } else {
                try {
                    bdf.setValue(new BigDecimal(valueNode.asText()));
                } catch (Exception ignored) {}
            }
        } else if (inputComponent instanceof IntegerField inf) {
            if (valueNode.isNumber()) {
                inf.setValue(valueNode.asInt());
            } else {
                try {
                    inf.setValue(Integer.parseInt(valueNode.asText()));
                } catch (Exception ignored) {}
            }
        } else if (inputComponent instanceof DatePicker dp) {
            try {
                dp.setValue(LocalDate.parse(valueNode.asText()));
            } catch (Exception ignored) {}
        } else if (inputComponent instanceof TextField tf) {
            if (valueNode.isArray()) {
                List<String> items = new ArrayList<>();
                for (JsonNode item : valueNode) {
                    items.add(item.asText());
                }
                tf.setValue(String.join(", ", items));
            } else {
                tf.setValue(valueNode.asText());
            }
        }
    }
}
