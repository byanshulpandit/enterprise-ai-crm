import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import type { Segment, CampaignCreateRequest, Campaign } from '@/types';
import {
  ArrowLeft,
  Send,
  AlertCircle,
} from 'lucide-react';

export const CampaignCreateView: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [segmentId, setSegmentId] = useState<number | ''>('');
  const [messageTemplate, setMessageTemplate] = useState('');
  const [personalizationEnabled, setPersonalizationEnabled] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch available segments
  const { data: segmentResponse, isLoading: segmentsLoading } = useQuery({
    queryKey: ['segments', 'all'],
    queryFn: async () => {
      return await apiClient.get<Segment[]>('segments?page=0&size=100&sort=name,asc');
    },
  });

  const segments = segmentResponse?.data || [];

  // Create mutation
  const createMutation = useMutation({
    mutationFn: async (payload: CampaignCreateRequest) => {
      return await apiClient.post<Campaign>('campaigns', payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
      navigate('/campaigns');
    },
    onError: (err: any) => {
      setError(err?.message || 'Failed to create campaign draft');
    },
  });

  const handleInsertToken = (token: string) => {
    setMessageTemplate((prev) => `${prev} {{${token}}}`);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Please provide a campaign name.');
      return;
    }
    if (!segmentId) {
      setError('Please select a target audience segment.');
      return;
    }
    if (!messageTemplate.trim()) {
      setError('Please provide a message template body.');
      return;
    }

    setError(null);
    createMutation.mutate({
      name: name.trim(),
      description: description.trim() || undefined,
      segmentId: Number(segmentId),
      messageTemplate: messageTemplate.trim(),
      personalizationEnabled,
    });
  };

  return (
    <div className="space-y-6 text-left max-w-3xl mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3 pb-4 border-b border-app">
        <Button variant="ghost" size="sm" onClick={() => navigate('/campaigns')}>
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-app-primary">
            Create Campaign Draft
          </h1>
          <p className="text-xs text-app-secondary mt-0.5">
            Configure broadcast parameters, audience segment, and personalized message templates.
          </p>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs space-y-4">
          <h2 className="text-sm font-semibold text-app-primary uppercase tracking-wider">
            1. Campaign Metadata
          </h2>

          <Input
            label="Campaign Name"
            required
            placeholder="e.g. Q4 Executive Product Update"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          <Input
            label="Description (Optional)"
            placeholder="Operational purpose or business rationale"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          <div className="text-left space-y-1.5">
            <label className="text-xs font-medium text-app-secondary">
              Target Audience Segment <span className="text-red-500">*</span>
            </label>
            <select
              value={segmentId}
              onChange={(e) => setSegmentId(e.target.value ? Number(e.target.value) : '')}
              required
              className="w-full text-xs rounded-lg border border-app bg-app-surface text-app-primary px-3 py-2.5 focus-ring-accent"
              disabled={segmentsLoading}
            >
              <option value="">-- Choose a target segment --</option>
              {segments.map((seg) => (
                <option key={seg.id} value={seg.id}>
                  {seg.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-app-primary uppercase tracking-wider">
              2. Message Template
            </h2>
            <div className="flex items-center gap-1.5 text-xs text-app-muted">
              <span>Personalization Tokens:</span>
              <button
                type="button"
                onClick={() => handleInsertToken('firstName')}
                className="px-2 py-0.5 rounded bg-app-surface-subtle border border-app text-amber-500 font-mono text-[11px] hover:border-amber-500/50 cursor-pointer"
              >
                + firstName
              </button>
              <button
                type="button"
                onClick={() => handleInsertToken('city')}
                className="px-2 py-0.5 rounded bg-app-surface-subtle border border-app text-amber-500 font-mono text-[11px] hover:border-amber-500/50 cursor-pointer"
              >
                + city
              </button>
            </div>
          </div>

          <div className="text-left space-y-1.5">
            <textarea
              rows={5}
              required
              placeholder="Hello {{firstName}}, we are delighted to offer exclusive benefits for our partners in {{city}}..."
              value={messageTemplate}
              onChange={(e) => setMessageTemplate(e.target.value)}
              className="w-full text-xs font-sans rounded-lg border border-app bg-app-surface text-app-primary p-3 focus-ring-accent"
            />
            <p className="text-[11px] text-app-muted">
              Tokens enclosed in double braces will be substituted per recipient during delivery execution.
            </p>
          </div>

          <div className="flex items-center gap-2 pt-2 border-t border-app">
            <input
              type="checkbox"
              id="personalization-toggle"
              checked={personalizationEnabled}
              onChange={(e) => setPersonalizationEnabled(e.target.checked)}
              className="rounded border-app text-amber-500 focus:ring-amber-500 h-4 w-4"
            />
            <label htmlFor="personalization-toggle" className="text-xs text-app-primary select-none cursor-pointer">
              Enable dynamic recipient personalization engine
            </label>
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="outline" onClick={() => navigate('/campaigns')}>
            Cancel
          </Button>
          <Button type="submit" isLoading={createMutation.isPending} leftIcon={<Send className="w-4 h-4" />}>
            Save Campaign Draft
          </Button>
        </div>
      </form>
    </div>
  );
};
