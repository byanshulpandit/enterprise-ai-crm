package com.crm.platform.segment.compiler;

import com.crm.platform.customer.entity.Customer;
import com.crm.platform.segment.model.ConditionRuleNode;
import com.crm.platform.segment.model.LogicalOperator;
import com.crm.platform.segment.model.LogicalRuleNode;
import com.crm.platform.segment.model.RuleField;
import com.crm.platform.segment.model.RuleOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SegmentCriteriaCompilerTest {

    private SegmentCriteriaCompiler compiler;

    @BeforeEach
    void setUp() {
        compiler = new SegmentCriteriaCompiler();
    }

    @Test
    @DisplayName("compile returns non-null Specification for null AST root (active customers only)")
    void compile_NullRoot_ReturnsSpecification() {
        Specification<Customer> spec = compiler.compile(null);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("compile returns non-null Specification for single condition leaf")
    void compile_SingleLeaf_ReturnsSpecification() {
        ConditionRuleNode leaf = new ConditionRuleNode(RuleField.CITY, RuleOperator.EQUALS, "Delhi");
        Specification<Customer> spec = compiler.compile(leaf);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("compile returns non-null Specification for logical group")
    void compile_LogicalGroup_ReturnsSpecification() {
        ConditionRuleNode leaf1 = new ConditionRuleNode(RuleField.CITY, RuleOperator.EQUALS, "Delhi");
        ConditionRuleNode leaf2 = new ConditionRuleNode(RuleField.TOTAL_SPEND, RuleOperator.GREATER_THAN, new BigDecimal("5000"));
        LogicalRuleNode group = new LogicalRuleNode(LogicalOperator.AND, List.of(leaf1, leaf2));

        Specification<Customer> spec = compiler.compile(group);
        assertThat(spec).isNotNull();
    }
}
