package com.crm.platform.segment.compiler;

import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.entity.CustomerTag;
import com.crm.platform.segment.model.ConditionRuleNode;
import com.crm.platform.segment.model.LogicalOperator;
import com.crm.platform.segment.model.LogicalRuleNode;
import com.crm.platform.segment.model.RuleField;
import com.crm.platform.segment.model.RuleNode;
import com.crm.platform.segment.model.RuleOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Compiles a validated AST {@link RuleNode} into a type-safe JPA {@link Specification} for {@link Customer}.
 * Guarantees zero SQL concatenation, strict attribute whitelisting, and mandatory soft-delete filtering.
 */
@Component
public class SegmentCriteriaCompiler {

    /**
     * Compiles an AST {@link RuleNode} into a {@link Specification<Customer>}.
     */
    public Specification<Customer> compile(RuleNode rootNode) {
        return (root, query, cb) -> {
            // Mandatory invariant: Soft-deleted customers must NEVER match
            Predicate activeCustomerPredicate = cb.isNull(root.get("deletedAt"));

            if (rootNode == null) {
                return activeCustomerPredicate;
            }

            Predicate rulePredicate = buildPredicate(rootNode, root, query, cb);
            return cb.and(activeCustomerPredicate, rulePredicate);
        };
    }

    private Predicate buildPredicate(RuleNode node, Root<Customer> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (node instanceof LogicalRuleNode logicalNode) {
            List<Predicate> childPredicates = new ArrayList<>();
            for (RuleNode child : logicalNode.getConditions()) {
                childPredicates.add(buildPredicate(child, root, query, cb));
            }

            if (childPredicates.isEmpty()) {
                return cb.conjunction();
            }

            if (logicalNode.getOperator() == LogicalOperator.AND) {
                return cb.and(childPredicates.toArray(new Predicate[0]));
            } else {
                return cb.or(childPredicates.toArray(new Predicate[0]));
            }
        } else if (node instanceof ConditionRuleNode conditionNode) {
            return buildConditionPredicate(conditionNode, root, query, cb);
        }

        throw new IllegalArgumentException("Unsupported RuleNode type: " + node.getClass().getName());
    }

    private Predicate buildConditionPredicate(ConditionRuleNode node, Root<Customer> root,
                                               CriteriaQuery<?> query, CriteriaBuilder cb) {
        RuleField field = node.getField();
        RuleOperator op = node.getOperator();
        Object value = node.getValue();

        if (field == RuleField.TAGS) {
            return buildTagPredicate(op, value, root, query, cb);
        }

        return switch (field) {
            case CITY -> buildStringPredicate(root.get("city"), op, value, cb);
            case TOTAL_SPEND -> buildComparablePredicate(root.<BigDecimal>get("totalSpend"), op, value, cb);
            case VISIT_COUNT -> buildComparablePredicate(root.<Integer>get("visitCount"), op, value, cb);
            case LAST_ACTIVE_DATE -> buildComparablePredicate(root.<LocalDate>get("lastActiveDate"), op, value, cb);
            default -> throw new IllegalArgumentException("Unhandled field: " + field);
        };
    }

    private Predicate buildStringPredicate(Path<String> path, RuleOperator op,
                                            Object value, CriteriaBuilder cb) {
        return switch (op) {
            case EQUALS -> cb.equal(cb.lower(path), ((String) value).toLowerCase());
            case NOT_EQUALS -> cb.or(cb.isNull(path), cb.notEqual(cb.lower(path), ((String) value).toLowerCase()));
            case CONTAINS -> cb.like(cb.lower(path), "%" + ((String) value).toLowerCase() + "%");
            case IN -> {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                List<String> lowerList = list.stream().map(String::toLowerCase).toList();
                yield cb.lower(path).in(lowerList);
            }
            case NOT_IN -> {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                List<String> lowerList = list.stream().map(String::toLowerCase).toList();
                yield cb.or(cb.isNull(path), cb.not(cb.lower(path).in(lowerList)));
            }
            default -> throw new IllegalArgumentException("Unsupported operator for String field: " + op);
        };
    }

    @SuppressWarnings("unchecked")
    private <Y extends Comparable<? super Y>> Predicate buildComparablePredicate(
            Path<Y> path, RuleOperator op, Object value, CriteriaBuilder cb) {
        return switch (op) {
            case EQUALS -> cb.equal(path, (Y) value);
            case NOT_EQUALS -> cb.or(cb.isNull(path), cb.notEqual(path, (Y) value));
            case GREATER_THAN -> cb.greaterThan(path, (Y) value);
            case LESS_THAN -> cb.lessThan(path, (Y) value);
            case GREATER_THAN_OR_EQUAL -> cb.greaterThanOrEqualTo(path, (Y) value);
            case LESS_THAN_OR_EQUAL -> cb.lessThanOrEqualTo(path, (Y) value);
            case IN -> {
                List<Y> list = (List<Y>) value;
                yield path.in(list);
            }
            case NOT_IN -> {
                List<Y> list = (List<Y>) value;
                yield cb.or(cb.isNull(path), cb.not(path.in(list)));
            }
            default -> throw new IllegalArgumentException("Unsupported operator for Comparable field: " + op);
        };
    }

    /**
     * Builds tag queries via a subquery to {@link CustomerTag}.
     * This guarantees that Customer rows are never duplicated and that NOT_EQUALS / NOT_IN semantics
     * are evaluated accurately against the entire set of tags owned by each customer.
     */
    private Predicate buildTagPredicate(RuleOperator op, Object value, Root<Customer> root,
                                        CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<CustomerTag> tagRoot = subquery.from(CustomerTag.class);
        subquery.select(tagRoot.get("customer").get("id"));

        Predicate correlationPredicate = cb.equal(tagRoot.get("customer"), root);

        Predicate tagFilterPredicate = switch (op) {
            case EQUALS -> cb.equal(cb.lower(tagRoot.get("tag")), ((String) value).toLowerCase());
            case NOT_EQUALS -> cb.equal(cb.lower(tagRoot.get("tag")), ((String) value).toLowerCase());
            case CONTAINS -> cb.like(cb.lower(tagRoot.get("tag")), "%" + ((String) value).toLowerCase() + "%");
            case IN -> {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                List<String> lowerList = list.stream().map(String::toLowerCase).toList();
                yield cb.lower(tagRoot.get("tag")).in(lowerList);
            }
            case NOT_IN -> {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                List<String> lowerList = list.stream().map(String::toLowerCase).toList();
                yield cb.lower(tagRoot.get("tag")).in(lowerList);
            }
            default -> throw new IllegalArgumentException("Unsupported operator for tags field: " + op);
        };

        subquery.where(cb.and(correlationPredicate, tagFilterPredicate));

        // For NOT_EQUALS and NOT_IN, the condition is satisfied if NO matching tag exists
        if (op == RuleOperator.NOT_EQUALS || op == RuleOperator.NOT_IN) {
            return cb.not(cb.exists(subquery));
        } else {
            return cb.exists(subquery);
        }
    }
}
