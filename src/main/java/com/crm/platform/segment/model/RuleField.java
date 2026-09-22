package com.crm.platform.segment.model;

import com.crm.platform.common.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public enum RuleField {
    CITY("city", String.class, EnumSet.of(
            RuleOperator.EQUALS,
            RuleOperator.NOT_EQUALS,
            RuleOperator.CONTAINS,
            RuleOperator.IN,
            RuleOperator.NOT_IN
    )),
    TOTAL_SPEND("totalSpend", BigDecimal.class, EnumSet.of(
            RuleOperator.EQUALS,
            RuleOperator.NOT_EQUALS,
            RuleOperator.GREATER_THAN,
            RuleOperator.LESS_THAN,
            RuleOperator.GREATER_THAN_OR_EQUAL,
            RuleOperator.LESS_THAN_OR_EQUAL,
            RuleOperator.IN,
            RuleOperator.NOT_IN
    )),
    VISIT_COUNT("visitCount", Integer.class, EnumSet.of(
            RuleOperator.EQUALS,
            RuleOperator.NOT_EQUALS,
            RuleOperator.GREATER_THAN,
            RuleOperator.LESS_THAN,
            RuleOperator.GREATER_THAN_OR_EQUAL,
            RuleOperator.LESS_THAN_OR_EQUAL,
            RuleOperator.IN,
            RuleOperator.NOT_IN
    )),
    LAST_ACTIVE_DATE("lastActiveDate", LocalDate.class, EnumSet.of(
            RuleOperator.EQUALS,
            RuleOperator.NOT_EQUALS,
            RuleOperator.GREATER_THAN,
            RuleOperator.LESS_THAN,
            RuleOperator.GREATER_THAN_OR_EQUAL,
            RuleOperator.LESS_THAN_OR_EQUAL,
            RuleOperator.IN,
            RuleOperator.NOT_IN
    )),
    TAGS("tags", String.class, EnumSet.of(
            RuleOperator.EQUALS,
            RuleOperator.NOT_EQUALS,
            RuleOperator.CONTAINS,
            RuleOperator.IN,
            RuleOperator.NOT_IN
    ));

    private final String jsonName;
    private final Class<?> targetType;
    private final Set<RuleOperator> supportedOperators;

    RuleField(String jsonName, Class<?> targetType, Set<RuleOperator> supportedOperators) {
        this.jsonName = jsonName;
        this.targetType = targetType;
        this.supportedOperators = supportedOperators;
    }

    public String getJsonName() {
        return jsonName;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public boolean supportsOperator(RuleOperator op) {
        return supportedOperators.contains(op);
    }

    public void validateOperator(RuleOperator op) {
        if (!supportsOperator(op)) {
            throw new InvalidRequestException("Operator " + op + " is not supported for field '" + jsonName +
                    "'. Supported operators: " + supportedOperators);
        }
    }

    public static RuleField fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidRequestException("Field name must not be blank");
        }
        String normalized = value.trim();

        // Canonical names and documented aliases
        if (normalized.equalsIgnoreCase("city") || normalized.equalsIgnoreCase("location")) {
            return CITY;
        }
        if (normalized.equalsIgnoreCase("totalSpend") || normalized.equalsIgnoreCase("total_spend")) {
            return TOTAL_SPEND;
        }
        if (normalized.equalsIgnoreCase("visitCount") || normalized.equalsIgnoreCase("visit_count") ||
                normalized.equalsIgnoreCase("orderCount") || normalized.equalsIgnoreCase("order_count")) {
            return VISIT_COUNT;
        }
        if (normalized.equalsIgnoreCase("lastActiveDate") || normalized.equalsIgnoreCase("last_active_date") ||
                normalized.equalsIgnoreCase("lastOrderDate") || normalized.equalsIgnoreCase("last_order_date")) {
            return LAST_ACTIVE_DATE;
        }
        if (normalized.equalsIgnoreCase("tags") || normalized.equalsIgnoreCase("tag")) {
            return TAGS;
        }

        throw new InvalidRequestException("Unsupported field: '" + value +
                "'. Supported customer fields: city, totalSpend, visitCount, lastActiveDate, tags");
    }
}
