package com.crm.platform.frontend;

import com.crm.platform.segment.model.ConditionRuleNode;
import com.crm.platform.segment.model.LogicalOperator;
import com.crm.platform.segment.model.LogicalRuleNode;
import com.crm.platform.segment.model.RuleField;
import com.crm.platform.segment.model.RuleNode;
import com.crm.platform.segment.model.RuleOperator;
import com.crm.platform.segment.parser.SegmentRuleParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Frontend Segment AST Serialization & Backend Parser Compatibility Test")
class SegmentAstBuilderIntegrationTest {

    private SegmentRuleParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new SegmentRuleParser(objectMapper);
    }

    @Test
    @DisplayName("Should successfully serialize and parse single-level rule group")
    void shouldParseSingleLevelGroup() {
        // Construct AST matching RuleTreeContainer output
        ObjectNode group = JsonNodeFactory.instance.objectNode();
        group.put("operator", "AND");

        ArrayNode conditions = group.putArray("conditions");

        ObjectNode cond1 = JsonNodeFactory.instance.objectNode();
        cond1.put("field", "city");
        cond1.put("op", "EQUALS");
        cond1.put("value", "Delhi");
        conditions.add(cond1);

        ObjectNode cond2 = JsonNodeFactory.instance.objectNode();
        cond2.put("field", "totalSpend");
        cond2.put("op", "GREATER_THAN_OR_EQUAL");
        cond2.put("value", 10000);
        conditions.add(cond2);

        String json = group.toString();
        RuleNode node = parser.parse(json);

        assertNotNull(node);
        assertInstanceOf(LogicalRuleNode.class, node);
        LogicalRuleNode logicalNode = (LogicalRuleNode) node;
        assertEquals(LogicalOperator.AND, logicalNode.getOperator());
        assertEquals(2, logicalNode.getConditions().size());

        ConditionRuleNode c1 = (ConditionRuleNode) logicalNode.getConditions().get(0);
        assertEquals(RuleField.CITY, c1.getField());
        assertEquals(RuleOperator.EQUALS, c1.getOperator());
        assertEquals("Delhi", c1.getValue());

        ConditionRuleNode c2 = (ConditionRuleNode) logicalNode.getConditions().get(1);
        assertEquals(RuleField.TOTAL_SPEND, c2.getField());
        assertEquals(RuleOperator.GREATER_THAN_OR_EQUAL, c2.getOperator());
        assertEquals(new BigDecimal("10000"), c2.getValue());
    }

    @Test
    @DisplayName("Should successfully serialize and parse nested rule group")
    void shouldParseNestedRuleGroup() {
        // Top-level AND group
        ObjectNode rootGroup = JsonNodeFactory.instance.objectNode();
        rootGroup.put("operator", "AND");
        ArrayNode rootConditions = rootGroup.putArray("conditions");

        // Condition 1: City is Delhi
        ObjectNode cityCond = JsonNodeFactory.instance.objectNode();
        cityCond.put("field", "city");
        cityCond.put("op", "EQUALS");
        cityCond.put("value", "Delhi");
        rootConditions.add(cityCond);

        // Subgroup: OR
        ObjectNode subGroup = JsonNodeFactory.instance.objectNode();
        subGroup.put("operator", "OR");
        ArrayNode subConditions = subGroup.putArray("conditions");

        ObjectNode visitCond = JsonNodeFactory.instance.objectNode();
        visitCond.put("field", "visitCount");
        visitCond.put("op", "LESS_THAN");
        visitCond.put("value", 5);
        subConditions.add(visitCond);

        ObjectNode tagCond = JsonNodeFactory.instance.objectNode();
        tagCond.put("field", "tags");
        tagCond.put("op", "CONTAINS");
        tagCond.put("value", "vip");
        subConditions.add(tagCond);

        rootConditions.add(subGroup);

        String json = rootGroup.toString();
        RuleNode node = parser.parse(json);

        assertNotNull(node);
        assertInstanceOf(LogicalRuleNode.class, node);
        LogicalRuleNode rootLogical = (LogicalRuleNode) node;
        assertEquals(2, rootLogical.getConditions().size());

        RuleNode secondChild = rootLogical.getConditions().get(1);
        assertInstanceOf(LogicalRuleNode.class, secondChild);
        LogicalRuleNode subLogical = (LogicalRuleNode) secondChild;
        assertEquals(LogicalOperator.OR, subLogical.getOperator());
        assertEquals(2, subLogical.getConditions().size());
    }
}
