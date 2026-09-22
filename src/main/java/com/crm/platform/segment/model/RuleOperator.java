package com.crm.platform.segment.model;

import com.crm.platform.common.exception.InvalidRequestException;

public enum RuleOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL,
    CONTAINS,
    IN,
    NOT_IN;

    public static RuleOperator fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidRequestException("Operator must not be blank");
        }
        String trimmed = value.trim().toUpperCase();
        try {
            return RuleOperator.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unsupported operator: '" + value + "'. Supported operators: " +
                    "EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, CONTAINS, IN, NOT_IN");
        }
    }
}
