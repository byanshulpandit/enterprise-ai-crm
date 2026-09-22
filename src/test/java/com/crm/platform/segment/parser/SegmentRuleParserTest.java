package com.crm.platform.segment.parser;

import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.segment.model.ConditionRuleNode;
import com.crm.platform.segment.model.LogicalOperator;
import com.crm.platform.segment.model.LogicalRuleNode;
import com.crm.platform.segment.model.RuleField;
import com.crm.platform.segment.model.RuleNode;
import com.crm.platform.segment.model.RuleOperator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SegmentRuleParserTest {

    private SegmentRuleParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new SegmentRuleParser(objectMapper);
    }

    @Test
    @DisplayName("Parse valid single condition leaf")
    void parse_ValidSingleLeaf() {
        String json = "{\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Mumbai\"}";
        RuleNode node = parser.parse(json);

        assertThat(node).isInstanceOf(ConditionRuleNode.class);
        ConditionRuleNode condition = (ConditionRuleNode) node;
        assertThat(condition.getField()).isEqualTo(RuleField.CITY);
        assertThat(condition.getOperator()).isEqualTo(RuleOperator.EQUALS);
        assertThat(condition.getValue()).isEqualTo("Mumbai");
    }

    @Test
    @DisplayName("Parse valid logical group with AND")
    void parse_ValidLogicalAndGroup() {
        String json = "{" +
                "\"operator\":\"AND\"," +
                "\"conditions\":[" +
                "  {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}," +
                "  {\"field\":\"totalSpend\",\"op\":\"GREATER_THAN\",\"value\":5000}" +
                "]}";
        RuleNode node = parser.parse(json);

        assertThat(node).isInstanceOf(LogicalRuleNode.class);
        LogicalRuleNode group = (LogicalRuleNode) node;
        assertThat(group.getOperator()).isEqualTo(LogicalOperator.AND);
        assertThat(group.getConditions()).hasSize(2);

        ConditionRuleNode c1 = (ConditionRuleNode) group.getConditions().get(0);
        assertThat(c1.getField()).isEqualTo(RuleField.CITY);
        assertThat(c1.getValue()).isEqualTo("Delhi");

        ConditionRuleNode c2 = (ConditionRuleNode) group.getConditions().get(1);
        assertThat(c2.getField()).isEqualTo(RuleField.TOTAL_SPEND);
        assertThat(c2.getOperator()).isEqualTo(RuleOperator.GREATER_THAN);
        assertThat(c2.getValue()).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("Parse valid nested logical group (AND with nested OR)")
    void parse_ValidNestedGroup() {
        String json = "{" +
                "\"operator\":\"AND\"," +
                "\"conditions\":[" +
                "  {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Mumbai\"}," +
                "  {" +
                "    \"operator\":\"OR\"," +
                "    \"conditions\":[" +
                "      {\"field\":\"totalSpend\",\"op\":\"GREATER_THAN\",\"value\":10000}," +
                "      {\"field\":\"visitCount\",\"op\":\"GREATER_THAN_OR_EQUAL\",\"value\":5}" +
                "    ]" +
                "  }" +
                "]}";
        RuleNode node = parser.parse(json);

        assertThat(node).isInstanceOf(LogicalRuleNode.class);
        LogicalRuleNode rootGroup = (LogicalRuleNode) node;
        assertThat(rootGroup.getOperator()).isEqualTo(LogicalOperator.AND);
        assertThat(rootGroup.getConditions()).hasSize(2);

        RuleNode secondChild = rootGroup.getConditions().get(1);
        assertThat(secondChild).isInstanceOf(LogicalRuleNode.class);
        LogicalRuleNode orGroup = (LogicalRuleNode) secondChild;
        assertThat(orGroup.getOperator()).isEqualTo(LogicalOperator.OR);
        assertThat(orGroup.getConditions()).hasSize(2);

        ConditionRuleNode spendCond = (ConditionRuleNode) orGroup.getConditions().get(0);
        assertThat(spendCond.getField()).isEqualTo(RuleField.TOTAL_SPEND);
        assertThat(spendCond.getValue()).isEqualTo(new BigDecimal("10000"));

        ConditionRuleNode visitCond = (ConditionRuleNode) orGroup.getConditions().get(1);
        assertThat(visitCond.getField()).isEqualTo(RuleField.VISIT_COUNT);
        assertThat(visitCond.getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("Parse IN operator with array values")
    void parse_InOperator_Success() {
        String json = "{\"field\":\"city\",\"op\":\"IN\",\"value\":[\"Delhi\",\"Mumbai\",\"Bangalore\"]}";
        RuleNode node = parser.parse(json);

        assertThat(node).isInstanceOf(ConditionRuleNode.class);
        ConditionRuleNode condition = (ConditionRuleNode) node;
        assertThat(condition.getOperator()).isEqualTo(RuleOperator.IN);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) condition.getValue();
        assertThat(list).containsExactly("Delhi", "Mumbai", "Bangalore");
    }

    @Test
    @DisplayName("Parse lastActiveDate with valid ISO format")
    void parse_DateSuccess() {
        String json = "{\"field\":\"lastActiveDate\",\"op\":\"GREATER_THAN\",\"value\":\"2026-01-15\"}";
        RuleNode node = parser.parse(json);

        ConditionRuleNode cond = (ConditionRuleNode) node;
        assertThat(cond.getValue()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("Reject null or empty rule JSON")
    void parse_NullOrEmpty_Throws() {
        assertThatThrownBy(() -> parser.parse((String) null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> parser.parse("{}"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Reject logical group with missing operator")
    void parse_GroupMissingOperator_Throws() {
        String json = "{\"conditions\":[{\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}]}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must specify an operator");
    }

    @Test
    @DisplayName("Reject logical group with unsupported operator")
    void parse_GroupUnsupportedOperator_Throws() {
        String json = "{\"operator\":\"XOR\",\"conditions\":[{\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}]}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported logical operator");
    }

    @Test
    @DisplayName("Reject logical group with empty conditions")
    void parse_GroupEmptyConditions_Throws() {
        String json = "{\"operator\":\"AND\",\"conditions\":[]}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    @DisplayName("Reject leaf with missing field")
    void parse_LeafMissingField_Throws() {
        String json = "{\"op\":\"EQUALS\",\"value\":\"Delhi\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Reject leaf with missing operator")
    void parse_LeafMissingOperator_Throws() {
        String json = "{\"field\":\"city\",\"value\":\"Delhi\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must specify an operator");
    }

    @Test
    @DisplayName("Reject leaf with missing value")
    void parse_LeafMissingValue_Throws() {
        String json = "{\"field\":\"city\",\"op\":\"EQUALS\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must specify a non-null 'value'");
    }

    @Test
    @DisplayName("Reject unknown field name")
    void parse_UnknownField_Throws() {
        String json = "{\"field\":\"hobbies\",\"op\":\"EQUALS\",\"value\":\"cricket\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported field");
    }

    @Test
    @DisplayName("Reject unknown operator")
    void parse_UnknownOperator_Throws() {
        String json = "{\"field\":\"city\",\"op\":\"MATCHES_REGEX\",\"value\":\"^D\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported operator");
    }

    @Test
    @DisplayName("Reject incompatible operator for field: city GREATER_THAN")
    void parse_IncompatibleOperator_Throws() {
        String json = "{\"field\":\"city\",\"op\":\"GREATER_THAN\",\"value\":\"Mumbai\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not supported for field 'city'");
    }

    @Test
    @DisplayName("Reject invalid numeric type for totalSpend: non-numeric string")
    void parse_InvalidNumericTotalSpend_Throws() {
        String json = "{\"field\":\"totalSpend\",\"op\":\"GREATER_THAN\",\"value\":\"abc\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid numeric value for field 'totalSpend'");
    }

    @Test
    @DisplayName("Reject invalid integer for visitCount: string text")
    void parse_InvalidIntegerVisitCount_Throws() {
        String json = "{\"field\":\"visitCount\",\"op\":\"EQUALS\",\"value\":\"five\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid integer value for field 'visitCount'");
    }

    @Test
    @DisplayName("Reject invalid date format for lastActiveDate")
    void parse_InvalidDate_Throws() {
        String json = "{\"field\":\"lastActiveDate\",\"op\":\"GREATER_THAN\",\"value\":\"not-a-date\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid date format for field 'lastActiveDate'");
    }

    @Test
    @DisplayName("Reject IN operator when value is scalar instead of array")
    void parse_InWithScalar_Throws() {
        String json = "{\"field\":\"city\",\"op\":\"IN\",\"value\":\"Delhi\"}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("requires a JSON array of values");
    }

    @Test
    @DisplayName("Reject IN operator when array is empty")
    void parse_InWithEmptyArray_Throws() {
        String json = "{\"field\":\"city\",\"op\":\"IN\",\"value\":[]}";
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("requires a non-empty array of values");
    }

    @Test
    @DisplayName("Reject rule exceeding maximum permitted nesting depth")
    void parse_DepthExceeded_Throws() {
        // Build 12 levels of nested AND groups
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append("{\"operator\":\"AND\",\"conditions\":[");
        }
        sb.append("{\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}");
        for (int i = 0; i < 12; i++) {
            sb.append("]}");
        }

        assertThatThrownBy(() -> parser.parse(sb.toString()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("exceeds maximum permitted nesting depth");
    }

    @Test
    @DisplayName("Accept rule with depth exactly at limit (MAX_DEPTH = 10)")
    void parse_DepthExactlyAtLimit_Success() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            sb.append("{\"operator\":\"AND\",\"conditions\":[");
        }
        sb.append("{\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}");
        for (int i = 0; i < 9; i++) {
            sb.append("]}");
        }

        RuleNode node = parser.parse(sb.toString());
        assertThat(node).isNotNull();
    }
}
