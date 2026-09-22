package com.crm.platform.segment.model;

import java.util.Objects;

/**
 * Represents an atomic leaf condition in the AST: (field operator value).
 */
public final class ConditionRuleNode implements RuleNode {

    private final RuleField field;
    private final RuleOperator operator;
    private final Object value;

    public ConditionRuleNode(RuleField field, RuleOperator operator, Object value) {
        this.field = Objects.requireNonNull(field, "Field must not be null");
        this.operator = Objects.requireNonNull(operator, "Operator must not be null");
        this.value = Objects.requireNonNull(value, "Value must not be null");
    }

    public RuleField getField() {
        return field;
    }

    public RuleOperator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionRuleNode that = (ConditionRuleNode) o;
        return field == that.field && operator == that.operator && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, operator, value);
    }

    @Override
    public String toString() {
        return "ConditionRuleNode{" +
                "field=" + field +
                ", operator=" + operator +
                ", value=" + value +
                '}';
    }
}
