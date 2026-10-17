import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import type {
  RuleField,
  RuleOperator,
  ConditionRuleNode,
  LogicalRuleNode,
  RuleNode,
  SegmentCreateRequest,
  Segment,
} from '@/types';
import {
  Plus,
  Trash2,
  FolderPlus,
  ArrowLeft,
  Save,
  AlertCircle,
  HelpCircle,
} from 'lucide-react';
import { cn } from '@/lib/utils';

const FIELD_OPTIONS: { value: RuleField; label: string; operators: RuleOperator[]; placeholder: string }[] = [
  {
    value: 'city',
    label: 'Customer City',
    operators: ['EQUALS', 'NOT_EQUALS', 'CONTAINS', 'NOT_CONTAINS', 'IN', 'NOT_IN'],
    placeholder: 'e.g. San Francisco or Mumbai',
  },
  {
    value: 'totalSpend',
    label: 'Total Spend ($)',
    operators: ['GREATER_THAN_OR_EQUAL', 'GREATER_THAN', 'LESS_THAN_OR_EQUAL', 'LESS_THAN', 'EQUALS', 'NOT_EQUALS'],
    placeholder: 'e.g. 1000.00',
  },
  {
    value: 'visitCount',
    label: 'Visit Count',
    operators: ['GREATER_THAN_OR_EQUAL', 'GREATER_THAN', 'LESS_THAN_OR_EQUAL', 'LESS_THAN', 'EQUALS', 'NOT_EQUALS'],
    placeholder: 'e.g. 5',
  },
  {
    value: 'lastActiveDate',
    label: 'Last Active Date',
    operators: ['BEFORE', 'AFTER', 'EQUALS', 'NOT_EQUALS'],
    placeholder: 'YYYY-MM-DD',
  },
  {
    value: 'tags',
    label: 'Customer Tags',
    operators: ['CONTAINS', 'NOT_CONTAINS', 'EQUALS', 'NOT_EQUALS', 'IN', 'NOT_IN'],
    placeholder: 'e.g. vip or gold',
  },
];

export const SegmentBuilderView: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Initial Root Logical Group (Depth 1)
  const [ruleTree, setRuleTree] = useState<LogicalRuleNode>({
    operator: 'AND',
    conditions: [
      {
        field: 'city',
        op: 'EQUALS',
        value: 'San Francisco',
      },
    ],
  });

  // Calculate current maximum tree depth to enforce max depth 10
  const calculateDepth = (node: RuleNode): number => {
    if (!('conditions' in node)) return 1;
    if (node.conditions.length === 0) return 1;
    return 1 + Math.max(...node.conditions.map(calculateDepth));
  };

  const currentDepth = calculateDepth(ruleTree);

  // Mutation to persist segment
  const createMutation = useMutation({
    mutationFn: async (payload: SegmentCreateRequest) => {
      return await apiClient.post<Segment>('segments', payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['segments'] });
      navigate('/segments');
    },
    onError: (err: any) => {
      setError(err?.message || 'Failed to save segment criteria');
    },
  });

  const handleSave = () => {
    if (!name.trim()) {
      setError('Please provide a name for this segment.');
      return;
    }

    if (currentDepth > 10) {
      setError('Rule tree exceeds the maximum permitted nesting depth of 10.');
      return;
    }

    setError(null);
    createMutation.mutate({
      name: name.trim(),
      description: description.trim() || undefined,
      rules: ruleTree,
    });
  };

  // Node manipulators
  const updateOperator = (node: LogicalRuleNode, op: 'AND' | 'OR') => {
    node.operator = op;
    setRuleTree({ ...ruleTree });
  };

  const addCondition = (group: LogicalRuleNode) => {
    group.conditions.push({
      field: 'totalSpend',
      op: 'GREATER_THAN_OR_EQUAL',
      value: 500,
    });
    setRuleTree({ ...ruleTree });
  };

  const addSubGroup = (group: LogicalRuleNode) => {
    if (currentDepth >= 10) {
      setError('Cannot add subgroup: maximum tree depth of 10 reached.');
      return;
    }
    group.conditions.push({
      operator: 'OR',
      conditions: [
        {
          field: 'city',
          op: 'EQUALS',
          value: 'New York',
        },
      ],
    });
    setRuleTree({ ...ruleTree });
  };

  const removeChild = (parent: LogicalRuleNode, index: number) => {
    parent.conditions.splice(index, 1);
    setRuleTree({ ...ruleTree });
  };

  const updateLeaf = (
    leaf: ConditionRuleNode,
    field: RuleField,
    op: RuleOperator,
    val: string | number | string[]
  ) => {
    leaf.field = field;
    leaf.op = op;
    leaf.value = val;
    setRuleTree({ ...ruleTree });
  };

  // Recursive renderer for rule tree
  const renderNode = (
    node: RuleNode,
    parent: LogicalRuleNode | null,
    index: number,
    depth: number
  ): React.ReactNode => {
    if ('conditions' in node) {
      // Logical Group
      return (
        <div
          key={`group-${depth}-${index}`}
          className={cn(
            'p-4 rounded-xl border border-app bg-app-surface shadow-xs space-y-3 relative',
            depth > 1 && 'ml-4 sm:ml-6 mt-3 border-l-4 border-l-amber-500'
          )}
        >
          <div className="flex flex-wrap items-center justify-between gap-3 pb-2 border-b border-app">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-app-secondary">Match:</span>
              <div className="inline-flex rounded-lg border border-app p-0.5 bg-app-surface-subtle">
                <button
                  type="button"
                  onClick={() => updateOperator(node, 'AND')}
                  className={cn(
                    'px-2.5 py-1 text-xs font-bold rounded-md transition-colors cursor-pointer',
                    node.operator === 'AND'
                      ? 'bg-amber-500 text-white shadow-xs'
                      : 'text-app-muted hover:text-app-primary'
                  )}
                >
                  ALL (AND)
                </button>
                <button
                  type="button"
                  onClick={() => updateOperator(node, 'OR')}
                  className={cn(
                    'px-2.5 py-1 text-xs font-bold rounded-md transition-colors cursor-pointer',
                    node.operator === 'OR'
                      ? 'bg-amber-500 text-white shadow-xs'
                      : 'text-app-muted hover:text-app-primary'
                  )}
                >
                  ANY (OR)
                </button>
              </div>
              <span className="text-[11px] text-app-muted">Depth: {depth}/10</span>
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => addCondition(node)}
                leftIcon={<Plus className="w-3.5 h-3.5" />}
              >
                Add Condition
              </Button>
              {depth < 10 && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => addSubGroup(node)}
                  leftIcon={<FolderPlus className="w-3.5 h-3.5" />}
                >
                  Add Group
                </Button>
              )}
              {parent && (
                <button
                  type="button"
                  onClick={() => removeChild(parent, index)}
                  className="p-1.5 rounded-md text-app-muted hover:text-red-500 hover:bg-red-500/10 cursor-pointer"
                  title="Remove Group"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>

          <div className="space-y-2">
            {node.conditions.length === 0 ? (
              <p className="text-xs text-app-muted italic p-2">
                No conditions in this group. Click &ldquo;Add Condition&rdquo; above.
              </p>
            ) : (
              node.conditions.map((child, childIdx) =>
                renderNode(child, node, childIdx, depth + 1)
              )
            )}
          </div>
        </div>
      );
    } else {
      // Leaf Condition
      const fieldConfig = FIELD_OPTIONS.find((f) => f.value === node.field) || FIELD_OPTIONS[0];

      return (
        <div
          key={`leaf-${depth}-${index}`}
          className="flex flex-wrap items-center gap-2.5 p-3 rounded-lg border border-app bg-app-surface-subtle"
        >
          {/* Field Select */}
          <select
            value={node.field}
            onChange={(e) => {
              const newField = e.target.value as RuleField;
              const newConfig = FIELD_OPTIONS.find((f) => f.value === newField) || FIELD_OPTIONS[0];
              updateLeaf(node, newField, newConfig.operators[0], node.value);
            }}
            className="text-xs font-semibold rounded-lg border border-app bg-app-surface text-app-primary px-3 py-2 focus-ring-accent"
          >
            {FIELD_OPTIONS.map((f) => (
              <option key={f.value} value={f.value}>
                {f.label}
              </option>
            ))}
          </select>

          {/* Operator Select */}
          <select
            value={node.op}
            onChange={(e) => {
              updateLeaf(node, node.field, e.target.value as RuleOperator, node.value);
            }}
            className="text-xs font-mono rounded-lg border border-app bg-app-surface text-amber-500 px-3 py-2 focus-ring-accent"
          >
            {fieldConfig.operators.map((op) => (
              <option key={op} value={op}>
                {op}
              </option>
            ))}
          </select>

          {/* Value Input */}
          <input
            type={node.field === 'totalSpend' || node.field === 'visitCount' ? 'number' : 'text'}
            value={node.value ?? ''}
            placeholder={fieldConfig.placeholder}
            onChange={(e) => {
              const val =
                node.field === 'totalSpend'
                  ? parseFloat(e.target.value) || 0
                  : node.field === 'visitCount'
                  ? parseInt(e.target.value) || 0
                  : e.target.value;
              updateLeaf(node, node.field, node.op, val);
            }}
            className="flex-1 min-w-[140px] text-xs rounded-lg border border-app bg-app-surface text-app-primary px-3 py-2 focus-ring-accent"
          />

          {/* Delete Leaf */}
          {parent && (
            <button
              type="button"
              onClick={() => removeChild(parent, index)}
              className="p-2 rounded-lg text-app-muted hover:text-red-500 hover:bg-red-500/10 cursor-pointer"
              title="Remove Condition"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>
      );
    }
  };

  return (
    <div className="space-y-6 text-left max-w-4xl mx-auto">
      {/* Top Header */}
      <div className="flex items-center justify-between pb-4 border-b border-app">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/segments')}>
            <ArrowLeft className="w-4 h-4" />
          </Button>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-app-primary">
              AST Segment Studio
            </h1>
            <p className="text-xs text-app-secondary mt-0.5">
              Construct recursive Boolean trees parsed directly by SegmentRuleParser.
            </p>
          </div>
        </div>

        <Button
          size="sm"
          onClick={handleSave}
          isLoading={createMutation.isPending}
          leftIcon={<Save className="w-4 h-4" />}
        >
          Save Segment
        </Button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Metadata Section */}
      <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs space-y-4">
        <h2 className="text-sm font-semibold text-app-primary uppercase tracking-wider">
          1. Segment Profile
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Segment Name"
            required
            placeholder="e.g. VIP California Spenders"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <Input
            label="Description"
            placeholder="Brief rationale for this target audience"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>
      </div>

      {/* Logic Tree Builder Section */}
      <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-app-primary uppercase tracking-wider">
            2. Logic Hierarchy (AST Tree)
          </h2>
          <div className="flex items-center gap-1.5 text-xs text-app-muted">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>Maximum depth: 10 levels</span>
          </div>
        </div>

        {renderNode(ruleTree, null, 0, 1)}
      </div>

      {/* Raw AST Preview */}
      <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs space-y-2">
        <h2 className="text-xs font-semibold text-app-secondary uppercase tracking-wider font-mono">
          AST JSON Serialization Preview
        </h2>
        <pre className="p-3 rounded-lg bg-app-surface-subtle border border-app text-[11px] font-mono text-app-primary overflow-x-auto max-h-48">
          {JSON.stringify(ruleTree, null, 2)}
        </pre>
      </div>
    </div>
  );
};
