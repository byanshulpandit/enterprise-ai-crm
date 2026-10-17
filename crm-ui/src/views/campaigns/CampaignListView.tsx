import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/context/AuthContext';
import { formatDateTime } from '@/lib/utils';
import { Button } from '@/components/common/Button';
import { Badge } from '@/components/common/Badge';
import { Skeleton } from '@/components/common/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import { Modal } from '@/components/common/Modal';
import { Pagination } from '@/components/common/Pagination';
import type { Campaign, CampaignLaunchResponse } from '@/types';
import {
  Send,
  Plus,
  Play,
  Activity,
  Trash2,
  AlertCircle,
} from 'lucide-react';

export const CampaignListView: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();

  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<string>('');

  const [launchConfirmCampaign, setLaunchConfirmCampaign] = useState<Campaign | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Fetch campaigns
  const {
    data: campaignResponse,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['campaigns', page, pageSize, statusFilter],
    queryFn: async () => {
      let endpoint = `campaigns?page=${page}&size=${pageSize}&sort=id,desc`;
      if (statusFilter) {
        endpoint += `&status=${statusFilter}`;
      }
      return await apiClient.get<Campaign[]>(endpoint);
    },
  });

  const campaigns = campaignResponse?.data || [];
  const pagination = campaignResponse?.metadata?.pagination;

  // Launch mutation
  const launchMutation = useMutation({
    mutationFn: async (id: number) => {
      return await apiClient.post<CampaignLaunchResponse>(`campaigns/${id}/launch`);
    },
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
      setLaunchConfirmCampaign(null);
      navigate(`/campaigns/${id}/monitor`);
    },
    onError: (err: any) => {
      setActionError(err?.message || 'Failed to launch campaign');
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      return await apiClient.delete(`campaigns/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
      setDeleteConfirmId(null);
    },
    onError: (err: any) => {
      alert(`Delete failed: ${err?.message || 'Access denied'}`);
    },
  });

  return (
    <div className="space-y-6 text-left">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-app">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-primary">Campaign Dispatcher</h1>
          <p className="text-xs sm:text-sm text-app-secondary mt-0.5">
            Orchestrate email dispatches, monitor delivery outbox telemetry, and track customer engagement.
          </p>
        </div>
        <Button
          size="sm"
          onClick={() => navigate('/campaigns/new')}
          leftIcon={<Plus className="w-4 h-4" />}
        >
          New Campaign
        </Button>
      </div>

      {/* Filter Tabs */}
      <div className="flex flex-wrap items-center gap-2">
        {['', 'DRAFT', 'RUNNING', 'COMPLETED', 'FAILED'].map((st) => (
          <button
            key={st || 'ALL'}
            type="button"
            onClick={() => {
              setStatusFilter(st);
              setPage(0);
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors cursor-pointer ${
              statusFilter === st
                ? 'bg-accent-subtle text-accent-themed border-amber-500/40 font-semibold'
                : 'bg-app-surface text-app-secondary border-app hover:bg-app-surface-subtle'
            }`}
          >
            {st ? st : 'ALL CAMPAIGNS'}
          </button>
        ))}
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>Error loading campaigns: {(error as Error).message}</span>
        </div>
      )}

      {/* Campaign Table */}
      <div className="rounded-xl border border-app bg-app-surface shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-app-surface-subtle border-b border-app text-app-secondary font-medium">
              <tr>
                <th className="py-3 px-4">Campaign Name</th>
                <th className="py-3 px-4">Target Segment</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Created By</th>
                <th className="py-3 px-4">Created Date</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-app">
              {isLoading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={6} className="py-3 px-4">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              ) : campaigns.length > 0 ? (
                campaigns.map((camp) => (
                  <tr key={camp.id} className="hover:bg-app-surface-subtle transition-colors">
                    <td className="py-3 px-4 font-semibold text-app-primary">
                      <div className="flex items-center gap-2">
                        <Send className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                        <span>{camp.name}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-app-secondary">{camp.segmentName}</td>
                    <td className="py-3 px-4">
                      <Badge variant={camp.status.toLowerCase() as any} size="sm">
                        {camp.status}
                      </Badge>
                    </td>
                    <td className="py-3 px-4 text-app-muted">
                      {camp.createdByName || `User #${camp.createdBy}`}
                    </td>
                    <td className="py-3 px-4 text-app-muted font-mono">
                      {formatDateTime(camp.createdAt)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {camp.status === 'DRAFT' && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => {
                              setActionError(null);
                              setLaunchConfirmCampaign(camp);
                            }}
                            leftIcon={<Play className="w-3.5 h-3.5" />}
                          >
                            Launch
                          </Button>
                        )}

                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/campaigns/${camp.id}/monitor`)}
                          leftIcon={<Activity className="w-3.5 h-3.5" />}
                        >
                          Monitor
                        </Button>

                        {isAdmin && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-500 hover:text-red-600 hover:bg-red-500/10"
                            onClick={() => setDeleteConfirmId(camp.id)}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="py-8">
                    <EmptyState
                      icon={<Send className="w-6 h-6" />}
                      title="No campaigns found"
                      description="Initialize an email dispatch campaign connected to one of your defined audience segments."
                      actionLabel="Create Campaign"
                      onAction={() => navigate('/campaigns/new')}
                    />
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <Pagination metadata={pagination} onPageChange={setPage} isLoading={isLoading} />
      </div>

      {/* Launch Confirmation Modal */}
      <Modal
        open={launchConfirmCampaign !== null}
        onOpenChange={(open) => !open && setLaunchConfirmCampaign(null)}
        title="Confirm Campaign Dispatch"
        description="Verify audience targets before transitioning campaign to RUNNING state."
        maxWidth="md"
      >
        <div className="py-3 space-y-4 text-left">
          {actionError && (
            <div className="p-3 rounded-lg bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{actionError}</span>
            </div>
          )}

          <div className="p-4 rounded-xl border border-app bg-app-surface-subtle space-y-2 text-xs">
            <div className="flex justify-between">
              <span className="text-app-secondary">Campaign Name:</span>
              <span className="font-semibold text-app-primary">{launchConfirmCampaign?.name}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-app-secondary">Target Segment:</span>
              <span className="font-semibold text-app-primary">{launchConfirmCampaign?.segmentName}</span>
            </div>
            <div className="flex justify-between pt-2 border-t border-app">
              <span className="text-app-secondary">Personalization Tokens:</span>
              <span className="font-mono text-amber-500">
                {launchConfirmCampaign?.personalizationEnabled ? 'Enabled ({{firstName}}, {{city}})' : 'Disabled'}
              </span>
            </div>
          </div>

          <p className="text-xs text-app-secondary">
            Launching will evaluate segment membership, populate the campaign delivery outbox, and push delivery jobs to the Redis queue.
          </p>

          <div className="flex justify-end gap-3 pt-3 border-t border-app">
            <Button variant="outline" size="sm" onClick={() => setLaunchConfirmCampaign(null)}>
              Cancel
            </Button>
            <Button
              size="sm"
              isLoading={launchMutation.isPending}
              onClick={() => {
                if (launchConfirmCampaign) {
                  launchMutation.mutate(launchConfirmCampaign.id);
                }
              }}
              leftIcon={<Play className="w-3.5 h-3.5" />}
            >
              Confirm &amp; Launch
            </Button>
          </div>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={deleteConfirmId !== null}
        onOpenChange={(open) => !open && setDeleteConfirmId(null)}
        title="Delete Campaign"
        description="Are you sure you want to delete this campaign? All outbox and delivery logs will be purged."
        maxWidth="sm"
      >
        <div className="flex justify-end gap-3 pt-4 border-t border-app">
          <Button variant="outline" size="sm" onClick={() => setDeleteConfirmId(null)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            size="sm"
            isLoading={deleteMutation.isPending}
            onClick={() => {
              if (deleteConfirmId) {
                deleteMutation.mutate(deleteConfirmId);
              }
            }}
          >
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  );
};
