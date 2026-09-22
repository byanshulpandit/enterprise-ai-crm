package com.crm.platform.segment.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a composite logical group node in the AST (AND / OR) containing one or more child conditions.
 */
public final class LogicalRuleNode implements RuleNode {

    private final LogicalOperator operator;
    private final List<RuleNode> conditions;

    public LogicalRuleNode(LogicalOperator operator, List<RuleNode> conditions) {
        this.operator = Objects.requireNonNull(operator, "Logical operator must not be null");
        this.conditions = conditions != null ? Collections.unmodifiableList(conditions) : Collections.emptyList();
    }

    public LogicalOperator getOperator() {
        return operator;
    }

    public List<RuleNode> getConditions() {
        return conditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogicalRuleNode that = (LogicalRuleNode) o;
        return operator == that.operator && Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, conditions);
    }

    @Override
    public String toString() {
        return "LogicalRuleNode{" +
                "operator=" + operator +
                ", conditions=" + conditions +
                '}';
    }
}
