import React, { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/context/AuthContext';
import { formatDateTime } from '@/lib/utils';
import { Button } from '@/components/common/Button';
import { Badge } from '@/components/common/Badge';
import { Skeleton } from '@/components/common/Skeleton';
import type {
  AiRuleGenerationResponse,
  AiSegmentAuditDto,
  SegmentCreateRequest,
} from '@/types';
import {
  Sparkles,
  Bot,
  AlertCircle,
  CheckCircle,
  Save,
  ShieldAlert,
} from 'lucide-react';

export const AiSegmentView: React.FC = () => {
  const navigate = useNavigate();
  const { isAdmin } = useAuth();

  const [prompt, setPrompt] = useState('');
  const [segmentName, setSegmentName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [generatedResult, setGeneratedResult] = useState<AiRuleGenerationResponse | null>(null);

  // Generate rules mutation
  const generateMutation = useMutation({
    mutationFn: async (text: string) => {
      const res = await apiClient.post<AiRuleGenerationResponse>('ai/segments/generate-rules', {
        prompt: text.trim(),
      });
      return res.data;
    },
    onSuccess: (data) => {
      setGeneratedResult(data);
      setError(null);
      if (!segmentName) {
        setSegmentName('AI Segment: ' + prompt.slice(0, 30));
      }
    },
    onError: (err: any) => {
      setError(err?.message || 'AI rule generation failed on the server');
    },
  });

  // Save generated segment mutation
  const saveMutation = useMutation({
    mutationFn: async (payload: SegmentCreateRequest) => {
      return await apiClient.post('segments', payload);
    },
    onSuccess: () => {
      navigate('/segments');
    },
    onError: (err: any) => {
      setError(err?.message || 'Failed to save generated segment');
    },
  });

  // Fetch AI audit logs (Admin only)
  const { data: auditsRes, isLoading: auditsLoading } = useQuery({
    queryKey: ['ai', 'audits'],
    queryFn: async () => {
      return await apiClient.get<AiSegmentAuditDto[]>('ai/segments/audits?page=0&size=10');
    },
    enabled: isAdmin,
  });

  const audits = auditsRes?.data || [];

  const handleGenerate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) {
      setError('Please enter a natural language audience prompt.');
      return;
    }
    setError(null);
    generateMutation.mutate(prompt);
  };

  const handleSaveSegment = () => {
    if (!generatedResult) return;
    const finalName = segmentName.trim() || 'AI Generated Segment';
    saveMutation.mutate({
      name: finalName,
      description: `Generated from AI prompt: "${prompt}"`,
      rules: generatedResult.ruleTree,
    });
  };

  return (
    <div className="space-y-6 text-left max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-app">
        <div>
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-amber-500" />
            <h1 className="text-2xl font-bold tracking-tight text-app-primary">
              AI Segment Assistant
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-app-secondary mt-0.5">
            Synthesize structured AST rule trees from natural language business prompts using Google Gemini AI.
          </p>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Prompt Form Card */}
      <div className="p-6 rounded-2xl border border-app bg-app-surface shadow-xs space-y-4">
        <h2 className="text-sm font-semibold text-app-primary">Natural Language Prompt</h2>
        <form onSubmit={handleGenerate} className="space-y-4">
          <textarea
            rows={4}
            required
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="e.g. Find high value customers in San Francisco with spend greater than 1000 and at least 5 visits..."
            className="w-full text-xs font-sans rounded-xl border border-app bg-app-surface text-app-primary p-3.5 focus-ring-accent"
          />

          <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="flex items-center gap-1.5 text-xs text-app-muted">
              <Bot className="w-4 h-4" />
              <span>Uses Gemini AI with deterministic fallback when API key is unconfigured.</span>
            </div>

            <Button
              type="submit"
              size="sm"
              isLoading={generateMutation.isPending}
              leftIcon={<Sparkles className="w-4 h-4" />}
            >
              Generate Segment Rules
            </Button>
          </div>
        </form>
      </div>

      {/* Generated Result Card */}
      {generatedResult && (
        <div className="p-6 rounded-2xl border border-app bg-app-surface shadow-xs space-y-4 animate-in fade-in duration-200">
          <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-app">
            <div className="flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-emerald-500" />
              <h2 className="text-sm font-semibold text-app-primary">Generated Rule Tree</h2>
            </div>
            <div className="flex items-center gap-2">
              <Badge variant={generatedResult.fallbackUsed ? 'warning' : 'success'} size="sm">
                Provider: {generatedResult.provider}
              </Badge>
              {generatedResult.fallbackUsed && (
                <span className="text-[11px] text-amber-500 font-medium">
                  (Deterministic Rule Engine)
                </span>
              )}
            </div>
          </div>

          <p className="text-xs text-app-secondary bg-app-surface-subtle p-3 rounded-lg border border-app">
            {generatedResult.explanation}
          </p>

          <pre className="p-3 rounded-lg bg-app-surface-subtle border border-app text-[11px] font-mono text-app-primary overflow-x-auto max-h-56">
            {JSON.stringify(generatedResult.ruleTree, null, 2)}
          </pre>

          <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-3">
            <input
              type="text"
              placeholder="Segment Name"
              value={segmentName}
              onChange={(e) => setSegmentName(e.target.value)}
              className="w-full sm:w-72 text-xs rounded-lg border border-app bg-app-surface text-app-primary px-3 py-2 focus-ring-accent"
            />
            <Button
              size="sm"
              onClick={handleSaveSegment}
              isLoading={saveMutation.isPending}
              leftIcon={<Save className="w-4 h-4" />}
            >
              Save as Segment
            </Button>
          </div>
        </div>
      )}

      {/* Admin AI Audit Trail Table */}
      {isAdmin && (
        <div className="rounded-xl border border-app bg-app-surface shadow-xs overflow-hidden">
          <div className="p-4 border-b border-app">
            <div className="flex items-center gap-2">
              <ShieldAlert className="w-4 h-4 text-purple-400" />
              <h2 className="text-sm font-semibold text-app-primary">AI Synthesis Audit Log</h2>
            </div>
            <p className="text-xs text-app-secondary mt-0.5">
              Enterprise compliance log of user-initiated prompts and generative model providers.
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-app-surface-subtle border-b border-app text-app-secondary font-medium">
                <tr>
                  <th className="py-2.5 px-4">User</th>
                  <th className="py-2.5 px-4">Prompt</th>
                  <th className="py-2.5 px-4">Provider</th>
                  <th className="py-2.5 px-4">Fallback</th>
                  <th className="py-2.5 px-4 text-right">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-app">
                {auditsLoading ? (
                  [...Array(3)].map((_, i) => (
                    <tr key={i}>
                      <td colSpan={5} className="py-3 px-4">
                        <Skeleton className="h-5 w-full" />
                      </td>
                    </tr>
                  ))
                ) : audits.length > 0 ? (
                  audits.map((audit) => (
                    <tr key={audit.id} className="hover:bg-app-surface-subtle transition-colors">
                      <td className="py-2.5 px-4 font-semibold text-app-primary">
                        {audit.username || `User #${audit.userId}`}
                      </td>
                      <td className="py-2.5 px-4 text-app-secondary max-w-sm truncate" title={audit.prompt}>
                        {audit.prompt}
                      </td>
                      <td className="py-2.5 px-4 font-mono text-app-muted">{audit.provider}</td>
                      <td className="py-2.5 px-4">
                        <Badge variant={audit.fallbackUsed ? 'warning' : 'success'} size="sm">
                          {audit.fallbackUsed ? 'FALLBACK' : 'GEMINI'}
                        </Badge>
                      </td>
                      <td className="py-2.5 px-4 text-right text-app-muted font-mono">
                        {formatDateTime(audit.createdAt)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="py-6 text-center text-xs text-app-muted">
                      No AI prompt audits recorded yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
