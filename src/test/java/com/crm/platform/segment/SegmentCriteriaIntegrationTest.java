package com.crm.platform.segment;

import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.segment.compiler.SegmentCriteriaCompiler;
import com.crm.platform.segment.model.RuleNode;
import com.crm.platform.segment.parser.SegmentRuleParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SegmentCriteriaIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SegmentRuleParser ruleParser;

    @Autowired
    private SegmentCriteriaCompiler criteriaCompiler;

    private Customer customerA;
    private Customer customerB;
    private Customer customerC;
    private Customer customerDSoftDeleted;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();

        // Customer A: Delhi, high spend, high visits, recent, tags: vip, delhi-club
        customerA = new Customer();
        customerA.setFirstName("Alice");
        customerA.setLastName("Sharma");
        customerA.setEmail("alice@crm.internal");
        customerA.setCity("Delhi");
        customerA.setTotalSpend(new BigDecimal("12000.00"));
        customerA.setVisitCount(6);
        customerA.setLastActiveDate(LocalDate.of(2026, 3, 1));
        customerA.setTagsFromStrings(Set.of("vip", "delhi-club"));
        customerA = customerRepository.saveAndFlush(customerA);

        // Customer B: Delhi, low spend, low visits, older, tags: regular
        customerB = new Customer();
        customerB.setFirstName("Bob");
        customerB.setLastName("Verma");
        customerB.setEmail("bob@crm.internal");
        customerB.setCity("Delhi");
        customerB.setTotalSpend(new BigDecimal("5000.00"));
        customerB.setVisitCount(2);
        customerB.setLastActiveDate(LocalDate.of(2025, 11, 10));
        customerB.setTagsFromStrings(Set.of("regular"));
        customerB = customerRepository.saveAndFlush(customerB);

        // Customer C: Mumbai, high spend, high visits, recent, tags: vip
        customerC = new Customer();
        customerC.setFirstName("Charlie");
        customerC.setLastName("Patel");
        customerC.setEmail("charlie@crm.internal");
        customerC.setCity("Mumbai");
        customerC.setTotalSpend(new BigDecimal("15000.00"));
        customerC.setVisitCount(7);
        customerC.setLastActiveDate(LocalDate.of(2026, 3, 5));
        customerC.setTagsFromStrings(Set.of("vip"));
        customerC = customerRepository.saveAndFlush(customerC);

        // Customer D: Soft-deleted (deleted_at IS NOT NULL), matching all filters
        customerDSoftDeleted = new Customer();
        customerDSoftDeleted.setFirstName("Deleted");
        customerDSoftDeleted.setLastName("User");
        customerDSoftDeleted.setEmail("deleted@crm.internal");
        customerDSoftDeleted.setCity("Delhi");
        customerDSoftDeleted.setTotalSpend(new BigDecimal("25000.00"));
        customerDSoftDeleted.setVisitCount(10);
        customerDSoftDeleted.setLastActiveDate(LocalDate.of(2026, 3, 5));
        customerDSoftDeleted.setDeletedAt(Instant.now());
        customerDSoftDeleted.setTagsFromStrings(Set.of("vip"));
        customerDSoftDeleted = customerRepository.saveAndFlush(customerDSoftDeleted);
    }

    @AfterEach
    void tearDown() {
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("AND rule: city = Delhi AND totalSpend > 10000 matches Alice only")
    void testAndRule() {
        String ruleJson = "{" +
                "\"operator\":\"AND\"," +
                "\"conditions\":[" +
                "  {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}," +
                "  {\"field\":\"totalSpend\",\"op\":\"GREATER_THAN\",\"value\":10000}" +
                "]}";

        RuleNode ast = ruleParser.parse(ruleJson);
        Specification<Customer> spec = criteriaCompiler.compile(ast);

        long count = customerRepository.count(spec);
        List<Customer> members = customerRepository.findAll(spec);

        assertThat(count).isEqualTo(1);
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getEmail()).isEqualTo("alice@crm.internal");
    }

    @Test
    @DisplayName("OR rule: city = Delhi OR city = Mumbai matches Alice, Bob, Charlie (excludes soft-deleted)")
    void testOrRule() {
        String ruleJson = "{" +
                "\"operator\":\"OR\"," +
                "\"conditions\":[" +
                "  {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}," +
                "  {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Mumbai\"}" +
                "]}";

        RuleNode ast = ruleParser.parse(ruleJson);
        Specification<Customer> spec = criteriaCompiler.compile(ast);

        long count = customerRepository.count(spec);
        List<Customer> members = customerRepository.findAll(spec);

        assertThat(count).isEqualTo(3);
        assertThat(members).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("alice@crm.internal", "bob@crm.internal", "charlie@crm.internal")
                .doesNotContain("deleted@crm.internal");
    }

    @Test
    @DisplayName("Nested rule: city = Delhi AND (totalSpend > 10000 OR visitCount >= 5)")
    void testNestedRule() {
        String ruleJson = "{" +
                "\"operator\":\"AND\"," +
                "\"conditions\":[" +
                "  {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}," +
                "  {" +
                "    \"operator\":\"OR\"," +
                "    \"conditions\":[" +
                "      {\"field\":\"totalSpend\",\"op\":\"GREATER_THAN\",\"value\":10000}," +
                "      {\"field\":\"visitCount\",\"op\":\"GREATER_THAN_OR_EQUAL\",\"value\":5}" +
                "    ]" +
                "  }" +
                "]}";

        RuleNode ast = ruleParser.parse(ruleJson);
        Specification<Customer> spec = criteriaCompiler.compile(ast);

        long count = customerRepository.count(spec);
        List<Customer> members = customerRepository.findAll(spec);

        assertThat(count).isEqualTo(1);
        assertThat(members.get(0).getEmail()).isEqualTo("alice@crm.internal");
    }

    @Test
    @DisplayName("Numeric comparisons: totalSpend and visitCount")
    void testNumericComparisons() {
        String ruleJson = "{\"field\":\"totalSpend\",\"op\":\"GREATER_THAN_OR_EQUAL\",\"value\":12000}";
        RuleNode ast = ruleParser.parse(ruleJson);
        Specification<Customer> spec = criteriaCompiler.compile(ast);

        List<Customer> members = customerRepository.findAll(spec);
        assertThat(members).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("alice@crm.internal", "charlie@crm.internal");

        String visitRule = "{\"field\":\"visitCount\",\"op\":\"LESS_THAN\",\"value\":5}";
        RuleNode visitAst = ruleParser.parse(visitRule);
        Specification<Customer> visitSpec = criteriaCompiler.compile(visitAst);

        List<Customer> visitMembers = customerRepository.findAll(visitSpec);
        assertThat(visitMembers).extracting(Customer::getEmail)
                .containsExactly("bob@crm.internal");

        // LESS_THAN_OR_EQUAL: visitCount <= 2 -> Bob
        String lteRule = "{\"field\":\"visitCount\",\"op\":\"LESS_THAN_OR_EQUAL\",\"value\":2}";
        RuleNode lteAst = ruleParser.parse(lteRule);
        List<Customer> lteMembers = customerRepository.findAll(criteriaCompiler.compile(lteAst));
        assertThat(lteMembers).extracting(Customer::getEmail)
                .containsExactly("bob@crm.internal");
    }

    @Test
    @DisplayName("Scalar String operators: NOT_EQUALS, CONTAINS, IN, NOT_IN on city")
    void testStringScalarOperators() {
        // NOT_EQUALS: city != Delhi -> Charlie
        RuleNode neqAst = ruleParser.parse("{\"field\":\"city\",\"op\":\"NOT_EQUALS\",\"value\":\"Delhi\"}");
        List<Customer> neqMembers = customerRepository.findAll(criteriaCompiler.compile(neqAst));
        assertThat(neqMembers).extracting(Customer::getEmail)
                .containsExactly("charlie@crm.internal");

        // CONTAINS: city CONTAINS 'umb' -> Charlie (Mumbai)
        RuleNode containsAst = ruleParser.parse("{\"field\":\"city\",\"op\":\"CONTAINS\",\"value\":\"umb\"}");
        List<Customer> containsMembers = customerRepository.findAll(criteriaCompiler.compile(containsAst));
        assertThat(containsMembers).extracting(Customer::getEmail)
                .containsExactly("charlie@crm.internal");

        // IN: city IN ['Mumbai', 'Chennai'] -> Charlie
        RuleNode inAst = ruleParser.parse("{\"field\":\"city\",\"op\":\"IN\",\"value\":[\"Mumbai\",\"Chennai\"]}");
        List<Customer> inMembers = customerRepository.findAll(criteriaCompiler.compile(inAst));
        assertThat(inMembers).extracting(Customer::getEmail)
                .containsExactly("charlie@crm.internal");

        // NOT_IN: city NOT_IN ['Mumbai'] -> Alice, Bob
        RuleNode notInAst = ruleParser.parse("{\"field\":\"city\",\"op\":\"NOT_IN\",\"value\":[\"Mumbai\"]}");
        List<Customer> notInMembers = customerRepository.findAll(criteriaCompiler.compile(notInAst));
        assertThat(notInMembers).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("alice@crm.internal", "bob@crm.internal");
    }

    @Test
    @DisplayName("Complex nested combinations: nested AND inside OR, and nested OR inside OR")
    void testComplexNesting() {
        // nested AND inside OR: (city = Delhi AND totalSpend >= 10000) OR (city = Mumbai AND visitCount <= 5)
        // Alice: Delhi, spend 12000 -> matches branch 1
        // Charlie: Mumbai, visitCount 7 -> does NOT match branch 2 (visitCount > 5)
        // Bob: Delhi, spend 5000 -> does NOT match branch 1
        String andInsideOrJson = "{" +
                "\"operator\":\"OR\"," +
                "\"conditions\":[" +
                "  {" +
                "    \"operator\":\"AND\"," +
                "    \"conditions\":[" +
                "      {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}," +
                "      {\"field\":\"totalSpend\",\"op\":\"GREATER_THAN_OR_EQUAL\",\"value\":10000}" +
                "    ]" +
                "  }," +
                "  {" +
                "    \"operator\":\"AND\"," +
                "    \"conditions\":[" +
                "      {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Mumbai\"}," +
                "      {\"field\":\"visitCount\",\"op\":\"LESS_THAN_OR_EQUAL\",\"value\":5}" +
                "    ]" +
                "  }" +
                "]}";
        RuleNode andInsideOrAst = ruleParser.parse(andInsideOrJson);
        List<Customer> andInsideOrMembers = customerRepository.findAll(criteriaCompiler.compile(andInsideOrAst));
        assertThat(andInsideOrMembers).extracting(Customer::getEmail)
                .containsExactly("alice@crm.internal");

        // nested OR inside OR: (city = Delhi OR city = Pune) OR (city = Mumbai)
        String orInsideOrJson = "{" +
                "\"operator\":\"OR\"," +
                "\"conditions\":[" +
                "  {" +
                "    \"operator\":\"OR\"," +
                "    \"conditions\":[" +
                "      {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}," +
                "      {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Pune\"}" +
                "    ]" +
                "  }," +
                "  {\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Mumbai\"}" +
                "]}";
        RuleNode orInsideOrAst = ruleParser.parse(orInsideOrJson);
        List<Customer> orInsideOrMembers = customerRepository.findAll(criteriaCompiler.compile(orInsideOrAst));
        assertThat(orInsideOrMembers).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("alice@crm.internal", "bob@crm.internal", "charlie@crm.internal");
    }

    @Test
    @DisplayName("Date comparisons on lastActiveDate")
    void testDateComparisons() {
        String ruleJson = "{\"field\":\"lastActiveDate\",\"op\":\"GREATER_THAN_OR_EQUAL\",\"value\":\"2026-01-01\"}";
        RuleNode ast = ruleParser.parse(ruleJson);
        Specification<Customer> spec = criteriaCompiler.compile(ast);

        List<Customer> members = customerRepository.findAll(spec);
        assertThat(members).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("alice@crm.internal", "charlie@crm.internal");
    }

    @Test
    @DisplayName("Tag rules: EQUALS, NOT_EQUALS, IN, NOT_IN, CONTAINS")
    void testTagRules() {
        // EQUALS vip -> Alice, Charlie
        RuleNode eqAst = ruleParser.parse("{\"field\":\"tags\",\"op\":\"EQUALS\",\"value\":\"vip\"}");
        List<Customer> vipCustomers = customerRepository.findAll(criteriaCompiler.compile(eqAst));
        assertThat(vipCustomers).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("alice@crm.internal", "charlie@crm.internal");

        // NOT_EQUALS vip -> Bob
        RuleNode neqAst = ruleParser.parse("{\"field\":\"tags\",\"op\":\"NOT_EQUALS\",\"value\":\"vip\"}");
        List<Customer> nonVip = customerRepository.findAll(criteriaCompiler.compile(neqAst));
        assertThat(nonVip).extracting(Customer::getEmail)
                .containsExactly("bob@crm.internal");

        // IN [regular, vip] -> Alice, Bob, Charlie (no duplicates for Alice despite 2 tags)
        RuleNode inAst = ruleParser.parse("{\"field\":\"tags\",\"op\":\"IN\",\"value\":[\"regular\",\"vip\"]}");
        List<Customer> inCustomers = customerRepository.findAll(criteriaCompiler.compile(inAst));
        assertThat(inCustomers).hasSize(3);
        assertThat(inCustomers).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("alice@crm.internal", "bob@crm.internal", "charlie@crm.internal");

        // CONTAINS club -> Alice
        RuleNode containsAst = ruleParser.parse("{\"field\":\"tags\",\"op\":\"CONTAINS\",\"value\":\"club\"}");
        List<Customer> containsCustomers = customerRepository.findAll(criteriaCompiler.compile(containsAst));
        assertThat(containsCustomers).extracting(Customer::getEmail)
                .containsExactly("alice@crm.internal");

        // NOT_IN [vip, delhi-club] -> Bob only
        RuleNode notInAst = ruleParser.parse("{\"field\":\"tags\",\"op\":\"NOT_IN\",\"value\":[\"vip\",\"delhi-club\"]}");
        List<Customer> notInCustomers = customerRepository.findAll(criteriaCompiler.compile(notInAst));
        assertThat(notInCustomers).extracting(Customer::getEmail)
                .containsExactly("bob@crm.internal");
    }

    @Test
    @DisplayName("Count and pagination totalElements consistency")
    void testCountAndPaginationConsistency() {
        String ruleJson = "{\"field\":\"tags\",\"op\":\"IN\",\"value\":[\"vip\",\"delhi-club\",\"regular\"]}";
        RuleNode ast = ruleParser.parse(ruleJson);
        Specification<Customer> spec = criteriaCompiler.compile(ast);

        long previewCount = customerRepository.count(spec);
        Page<Customer> firstPage = customerRepository.findAll(spec, PageRequest.of(0, 2, Sort.by("id")));

        assertThat(previewCount).isEqualTo(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(previewCount);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
    }
}
