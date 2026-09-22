package com.crm.platform.segment.parser;

import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.segment.model.ConditionRuleNode;
import com.crm.platform.segment.model.LogicalOperator;
import com.crm.platform.segment.model.LogicalRuleNode;
import com.crm.platform.segment.model.RuleField;
import com.crm.platform.segment.model.RuleNode;
import com.crm.platform.segment.model.RuleOperator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates and parses JSON rule trees into strongly-typed AST {@link RuleNode} representations.
 */
@Component
public class SegmentRuleParser {

    public static final int MAX_DEPTH = 10;
    private final ObjectMapper objectMapper;

    public SegmentRuleParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Parses a raw JSON string into a validated {@link RuleNode} AST.
     */
    public RuleNode parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new InvalidRequestException("Rule tree JSON must not be null or empty");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            return parse(root);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException("Malformed rule tree JSON: " + e.getOriginalMessage());
        }
    }

    /**
     * Parses and validates a {@link JsonNode} into a {@link RuleNode} AST.
     */
    public RuleNode parse(JsonNode rootNode) {
        if (rootNode == null || rootNode.isNull()) {
            throw new InvalidRequestException("Rule tree must not be null");
        }
        if (rootNode.isObject() && rootNode.isEmpty()) {
            throw new InvalidRequestException("Rule tree must not be empty");
        }
        return parseNode(rootNode, 1);
    }

    private RuleNode parseNode(JsonNode node, int depth) {
        if (depth > MAX_DEPTH) {
            throw new InvalidRequestException("Rule tree exceeds maximum permitted nesting depth of " + MAX_DEPTH);
        }
        if (node == null || node.isNull()) {
            throw new InvalidRequestException("Rule condition node must not be null");
        }
        if (!node.isObject()) {
            throw new InvalidRequestException("Rule node must be a JSON object, got: " + node.getNodeType());
        }

        boolean hasConditions = node.has("conditions");
        boolean hasField = node.has("field");

        // Malformed: node cannot be both a logical group and a condition leaf
        if (hasConditions && hasField) {
            throw new InvalidRequestException("Malformed rule: node cannot contain both 'conditions' and 'field'");
        }

        if (hasConditions) {
            return parseLogicalGroup(node, depth);
        } else if (hasField) {
            return parseConditionLeaf(node);
        } else if (node.has("operator") || node.has("combinator")) {
            // Group missing conditions
            throw new InvalidRequestException("Logical group must contain a non-empty 'conditions' array");
        } else {
            throw new InvalidRequestException("Malformed rule node: expected a logical group with 'conditions' or a condition leaf with 'field' and 'op'");
        }
    }

    private LogicalRuleNode parseLogicalGroup(JsonNode node, int depth) {
        JsonNode opNode = node.has("operator") ? node.get("operator") :
                (node.has("combinator") ? node.get("combinator") : node.get("op"));

        if (opNode == null || opNode.isNull() || opNode.asText().trim().isEmpty()) {
            throw new InvalidRequestException("Logical group must specify an operator (AND or OR)");
        }

        LogicalOperator logicalOp = LogicalOperator.fromString(opNode.asText());

        JsonNode conditionsNode = node.get("conditions");
        if (!conditionsNode.isArray()) {
            throw new InvalidRequestException("Logical group 'conditions' must be an array");
        }
        if (conditionsNode.isEmpty()) {
            throw new InvalidRequestException("Logical group 'conditions' array must not be empty");
        }

        List<RuleNode> children = new ArrayList<>(conditionsNode.size());
        for (JsonNode child : conditionsNode) {
            children.add(parseNode(child, depth + 1));
        }

        return new LogicalRuleNode(logicalOp, children);
    }

    private ConditionRuleNode parseConditionLeaf(JsonNode node) {
        JsonNode fieldNode = node.get("field");
        if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().trim().isEmpty()) {
            throw new InvalidRequestException("Condition leaf must specify a non-blank 'field'");
        }
        RuleField field = RuleField.fromString(fieldNode.asText());

        JsonNode opNode = node.has("op") ? node.get("op") : node.get("operator");
        if (opNode == null || opNode.isNull() || opNode.asText().trim().isEmpty()) {
            throw new InvalidRequestException("Condition leaf for field '" + field.getJsonName() + "' must specify an operator ('op')");
        }
        RuleOperator operator = RuleOperator.fromString(opNode.asText());

        // Validate field-operator compatibility
        field.validateOperator(operator);

        // Validate and convert value
        if (!node.has("value") || node.get("value").isNull()) {
            throw new InvalidRequestException("Condition leaf must specify a non-null 'value' for field: " + field.getJsonName());
        }

        Object typedValue = parseAndConvertValue(field, operator, node.get("value"));
        return new ConditionRuleNode(field, operator, typedValue);
    }

    private Object parseAndConvertValue(RuleField field, RuleOperator operator, JsonNode valueNode) {
        if (operator == RuleOperator.IN || operator == RuleOperator.NOT_IN) {
            if (!valueNode.isArray()) {
                throw new InvalidRequestException("Operator " + operator + " for field '" + field.getJsonName() +
                        "' requires a JSON array of values, got: " + valueNode.getNodeType());
            }
            if (valueNode.isEmpty()) {
                throw new InvalidRequestException("Operator " + operator + " for field '" + field.getJsonName() +
                        "' requires a non-empty array of values");
            }
            List<Object> convertedList = new ArrayList<>(valueNode.size());
            for (JsonNode elem : valueNode) {
                if (elem.isNull()) {
                    throw new InvalidRequestException("Elements of " + operator + " array for field '" +
                            field.getJsonName() + "' must not be null");
                }
                convertedList.add(convertScalarValue(field, operator, elem));
            }
            return convertedList;
        }

        if (valueNode.isArray() || valueNode.isObject()) {
            throw new InvalidRequestException("Scalar operator " + operator + " for field '" + field.getJsonName() +
                    "' requires a scalar value, got: " + valueNode.getNodeType());
        }

        return convertScalarValue(field, operator, valueNode);
    }

    private Object convertScalarValue(RuleField field, RuleOperator operator, JsonNode node) {
        return switch (field) {
            case CITY -> {
                if (node.isBoolean() || node.isNumber()) {
                    throw new InvalidRequestException("Invalid value for field 'city': expected a string, got " + node.getNodeType());
                }
                String text = node.asText().trim();
                if (text.isEmpty()) {
                    throw new InvalidRequestException("Value for field 'city' must not be blank");
                }
                yield text;
            }
            case TOTAL_SPEND -> {
                if (node.isBoolean()) {
                    throw new InvalidRequestException("Invalid numeric value for field 'totalSpend': boolean is not allowed");
                }
                try {
                    String raw = node.asText().trim();
                    yield new BigDecimal(raw);
                } catch (NumberFormatException e) {
                    throw new InvalidRequestException("Invalid numeric value for field 'totalSpend': '" + node.asText() + "'");
                }
            }
            case VISIT_COUNT -> {
                if (node.isBoolean()) {
                    throw new InvalidRequestException("Invalid integer value for field 'visitCount': boolean is not allowed");
                }
                if (node.isFloatingPointNumber()) {
                    throw new InvalidRequestException("Invalid integer value for field 'visitCount': fractional numbers are not allowed: " + node.asText());
                }
                try {
                    String raw = node.asText().trim();
                    yield Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    throw new InvalidRequestException("Invalid integer value for field 'visitCount': '" + node.asText() + "'");
                }
            }
            case LAST_ACTIVE_DATE -> {
                if (!node.isTextual()) {
                    throw new InvalidRequestException("Invalid date value for field 'lastActiveDate': expected ISO date string (YYYY-MM-DD), got " + node.getNodeType());
                }
                try {
                    yield LocalDate.parse(node.asText().trim());
                } catch (DateTimeParseException e) {
                    throw new InvalidRequestException("Invalid date format for field 'lastActiveDate': '" + node.asText() + "'. Expected format: YYYY-MM-DD");
                }
            }
            case TAGS -> {
                if (node.isBoolean() || node.isNumber()) {
                    throw new InvalidRequestException("Invalid value for field 'tags': expected a string tag, got " + node.getNodeType());
                }
                String text = node.asText().trim();
                if (text.isEmpty()) {
                    throw new InvalidRequestException("Tag value must not be blank");
                }
                yield text;
            }
        };
    }
}
