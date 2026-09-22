package com.crm.platform.segment.model;

import com.crm.platform.common.exception.InvalidRequestException;

public enum LogicalOperator {
    AND,
    OR;

    public static LogicalOperator fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidRequestException("Logical operator must not be blank. Supported: AND, OR");
        }
        String trimmed = value.trim().toUpperCase();
        try {
            return LogicalOperator.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unsupported logical operator: '" + value + "'. Supported: AND, OR");
        }
    }
}
