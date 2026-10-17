import { describe, it, expect } from 'vitest';
import type { LogicalRuleNode, ConditionRuleNode, RuleNode } from '@/types';

// Helper function to calculate rule tree depth
const calculateDepth = (node: RuleNode): number => {
  if ('conditions' in node) {
    if (node.conditions.length === 0) return 1;
    return 1 + Math.max(...node.conditions.map(calculateDepth));
  }
  return 1;
};

describe('Segment AST Rule Logic', () => {
  it('constructs and computes depth of nested rule trees correctly', () => {
    const leaf1: ConditionRuleNode = { field: 'totalSpend', op: 'GREATER_THAN', value: 1000 };
    const leaf2: ConditionRuleNode = { field: 'city', op: 'EQUALS', value: 'New York' };

    const tree: LogicalRuleNode = {
      combinator: 'AND',
      conditions: [leaf1, leaf2],
    };

    expect(calculateDepth(tree)).toBe(2);
  });

  it('detects depth exceeding maximum allowed depth 10', () => {
    let current: LogicalRuleNode = {
      combinator: 'AND',
      conditions: [{ field: 'totalSpend', op: 'GREATER_THAN', value: 50 }],
    };

    for (let i = 0; i < 11; i++) {
      current = {
        combinator: 'OR',
        conditions: [current],
      };
    }

    const depth = calculateDepth(current);
    expect(depth).toBeGreaterThan(10);
  });

  it('serializes and deserializes AST matching backend expectations', () => {
    const originalAst: LogicalRuleNode = {
      combinator: 'AND',
      conditions: [
        { field: 'visitCount', op: 'GREATER_THAN_OR_EQUAL', value: 5 },
        {
          combinator: 'OR',
          conditions: [
            { field: 'city', op: 'EQUALS', value: 'San Francisco' },
            { field: 'city', op: 'EQUALS', value: 'Seattle' },
          ],
        },
      ],
    };

    const serialized = JSON.stringify(originalAst);
    const parsed = JSON.parse(serialized);

    expect(parsed.combinator).toBe('AND');
    expect(parsed.conditions.length).toBe(2);
    expect(parsed.conditions[1].combinator).toBe('OR');
    expect(parsed.conditions[1].conditions[0].value).toBe('San Francisco');
  });
});
