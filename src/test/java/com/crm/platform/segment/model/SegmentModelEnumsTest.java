package com.crm.platform.segment.model;

import com.crm.platform.common.exception.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SegmentModelEnumsTest {

    @Test
    @DisplayName("LogicalOperator fromString resolves AND and OR case-insensitively")
    void testLogicalOperator_FromString() {
        assertThat(LogicalOperator.fromString("AND")).isEqualTo(LogicalOperator.AND);
        assertThat(LogicalOperator.fromString("and")).isEqualTo(LogicalOperator.AND);
        assertThat(LogicalOperator.fromString("OR")).isEqualTo(LogicalOperator.OR);
        assertThat(LogicalOperator.fromString("or ")).isEqualTo(LogicalOperator.OR);

        assertThatThrownBy(() -> LogicalOperator.fromString(null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> LogicalOperator.fromString("   "))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> LogicalOperator.fromString("XOR"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("RuleOperator fromString resolves all 9 documented operators case-insensitively")
    void testRuleOperator_FromString() {
        for (RuleOperator op : RuleOperator.values()) {
            assertThat(RuleOperator.fromString(op.name())).isEqualTo(op);
            assertThat(RuleOperator.fromString(op.name().toLowerCase())).isEqualTo(op);
        }

        assertThatThrownBy(() -> RuleOperator.fromString(null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> RuleOperator.fromString(" "))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> RuleOperator.fromString("LIKE"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("RuleField fromString resolves canonical fields and documented aliases")
    void testRuleField_FromString() {
        assertThat(RuleField.fromString("city")).isEqualTo(RuleField.CITY);
        assertThat(RuleField.fromString("location")).isEqualTo(RuleField.CITY);

        assertThat(RuleField.fromString("totalSpend")).isEqualTo(RuleField.TOTAL_SPEND);
        assertThat(RuleField.fromString("total_spend")).isEqualTo(RuleField.TOTAL_SPEND);

        assertThat(RuleField.fromString("visitCount")).isEqualTo(RuleField.VISIT_COUNT);
        assertThat(RuleField.fromString("orderCount")).isEqualTo(RuleField.VISIT_COUNT);

        assertThat(RuleField.fromString("lastActiveDate")).isEqualTo(RuleField.LAST_ACTIVE_DATE);
        assertThat(RuleField.fromString("lastOrderDate")).isEqualTo(RuleField.LAST_ACTIVE_DATE);

        assertThat(RuleField.fromString("tags")).isEqualTo(RuleField.TAGS);
        assertThat(RuleField.fromString("tag")).isEqualTo(RuleField.TAGS);

        assertThatThrownBy(() -> RuleField.fromString(null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> RuleField.fromString(" "))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> RuleField.fromString("unknownField"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("RuleField operator compatibility validations")
    void testRuleField_OperatorCompatibility() {
        // CITY: supports EQUALS, NOT_EQUALS, CONTAINS, IN, NOT_IN; rejects numeric operators
        assertThat(RuleField.CITY.supportsOperator(RuleOperator.EQUALS)).isTrue();
        assertThat(RuleField.CITY.supportsOperator(RuleOperator.CONTAINS)).isTrue();
        assertThat(RuleField.CITY.supportsOperator(RuleOperator.IN)).isTrue();
        assertThat(RuleField.CITY.supportsOperator(RuleOperator.GREATER_THAN)).isFalse();

        assertThatThrownBy(() -> RuleField.CITY.validateOperator(RuleOperator.GREATER_THAN))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not supported for field 'city'");

        // TOTAL_SPEND: supports numeric operators; rejects CONTAINS
        assertThat(RuleField.TOTAL_SPEND.supportsOperator(RuleOperator.GREATER_THAN)).isTrue();
        assertThat(RuleField.TOTAL_SPEND.supportsOperator(RuleOperator.CONTAINS)).isFalse();

        assertThatThrownBy(() -> RuleField.TOTAL_SPEND.validateOperator(RuleOperator.CONTAINS))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not supported for field 'totalSpend'");
    }
}
