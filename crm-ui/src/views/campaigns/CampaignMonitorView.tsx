import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { formatDateTime, formatPercent } from '@/lib/utils';
import { Button } from '@/components/common/Button';
import { Badge } from '@/components/common/Badge';
import { Skeleton } from '@/components/common/Skeleton';
import { Pagination } from '@/components/common/Pagination';
import type { DeliverySummaryResponse, DeliveryRecordResponse, Campaign } from '@/types';
import {
  ArrowLeft,
  Radio,
  RefreshCw,
} from 'lucide-react';

export const CampaignMonitorView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const campaignId = Number(id);

  const [page, setPage] = useState(0);
  const [pageSize] = useState(15);
  const [statusFilter, setStatusFilter] = useState('');

  // Fetch campaign details
  const { data: campaignData } = useQuery({
    queryKey: ['campaign', campaignId],
    queryFn: async () => {
      const res = await apiClient.get<Campaign>(`campaigns/${campaignId}`);
      return res.data;
    },
    enabled: !isNaN(campaignId),
  });

  // Fetch telemetry delivery summary with controlled polling
  const {
    data: summary,
    isLoading: summaryLoading,
    refetch: refetchSummary,
  } = useQuery({
    queryKey: ['campaigns', campaignId, 'summary'],
    queryFn: async () => {
      const res = await apiClient.get<DeliverySummaryResponse>(
        `campaigns/${campaignId}/delivery-summary`
      );
      return res.data;
    },
    enabled: !isNaN(campaignId),
    refetchInterval: (query) => {
      const data = query.state.data;
      // Stop polling once the campaign has reached terminal state
      if (data && data.terminal) {
        return false;
      }
      return 2500; // Poll every 2.5s while active
    },
  });

  // Fetch recipient delivery ledger
  const {
    data: deliveryRecordsRes,
    isLoading: deliveriesLoading,
    refetch: refetchDeliveries,
  } = useQuery({
    queryKey: ['campaigns', campaignId, 'deliveries', page, pageSize, statusFilter],
    queryFn: async () => {
      let endpoint = `campaigns/${campaignId}/deliveries?page=${page}&size=${pageSize}&sort=id,desc`;
      if (statusFilter) {
        endpoint += `&status=${statusFilter}`;
      }
      return await apiClient.get<DeliveryRecordResponse[]>(endpoint);
    },
    enabled: !isNaN(campaignId),
  });

  const records = deliveryRecordsRes?.data || [];
  const pagination = deliveryRecordsRes?.metadata?.pagination;

  return (
    <div className="space-y-6 text-left max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-app">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/campaigns')}>
            <ArrowLeft className="w-4 h-4" />
          </Button>
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-app-primary">
                {campaignData?.name || `Campaign #${campaignId}`}
              </h1>
              {summary && (
                <Badge variant={summary.campaignStatus.toLowerCase() as any} size="sm">
                  {summary.campaignStatus}
                </Badge>
              )}
            </div>
            <p className="text-xs text-app-secondary mt-0.5">
              Target Audience: {campaignData?.segmentName || 'Custom AST'} &bull; Real-time delivery worker telemetry
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {summary && !summary.terminal && (
            <div className="flex items-center gap-1.5 text-xs text-amber-500 font-mono animate-pulse">
              <Radio className="w-4 h-4" />
              <span>LIVE POLLING</span>
            </div>
          )}
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              refetchSummary();
              refetchDeliveries();
            }}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Refresh
          </Button>
        </div>
      </div>

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {/* Total Audience */}
        <div className="p-4 rounded-xl border border-app bg-app-surface shadow-xs">
          <span className="text-xs text-app-secondary font-medium">Audience Size</span>
          {summaryLoading ? (
            <Skeleton className="h-7 w-16 mt-2" />
          ) : (
            <div className="mt-2 text-2xl font-bold text-app-primary">
              {summary?.targetAudienceSize ?? 0}
            </div>
          )}
        </div>

        {/* Sent */}
        <div className="p-4 rounded-xl border border-app bg-app-surface shadow-xs">
          <span className="text-xs text-emerald-500 font-medium">Dispatched (Sent)</span>
          {summaryLoading ? (
            <Skeleton className="h-7 w-16 mt-2" />
          ) : (
            <div className="mt-2 text-2xl font-bold text-emerald-500">
              {summary?.sentCount ?? 0}
            </div>
          )}
        </div>

        {/* Failed */}
        <div className="p-4 rounded-xl border border-app bg-app-surface shadow-xs">
          <span className="text-xs text-red-500 font-medium">Failed</span>
          {summaryLoading ? (
            <Skeleton className="h-7 w-16 mt-2" />
          ) : (
            <div className="mt-2 text-2xl font-bold text-red-500">
              {summary?.failedCount ?? 0}
            </div>
          )}
        </div>

        {/* Pending */}
        <div className="p-4 rounded-xl border border-app bg-app-surface shadow-xs">
          <span className="text-xs text-amber-500 font-medium">In Queue (Pending)</span>
          {summaryLoading ? (
            <Skeleton className="h-7 w-16 mt-2" />
          ) : (
            <div className="mt-2 text-2xl font-bold text-amber-500">
              {summary?.pendingCount ?? 0}
            </div>
          )}
        </div>
      </div>

      {/* Progress Bar */}
      {summary && (
        <div className="p-4 rounded-xl border border-app bg-app-surface shadow-xs space-y-2">
          <div className="flex justify-between text-xs">
            <span className="font-semibold text-app-primary">Execution Completion</span>
            <span className="font-mono text-amber-500 font-bold">
              {formatPercent(summary.completionPercentage)}
            </span>
          </div>
          <div className="w-full h-2 rounded-full bg-app-surface-subtle overflow-hidden border border-app-subtle">
            <div
              className="h-full bg-amber-500 transition-all duration-300 rounded-full"
              style={{ width: `${Math.min(100, summary.completionPercentage)}%` }}
            />
          </div>
        </div>
      )}

      {/* Delivery Recipient Ledger */}
      <div className="rounded-xl border border-app bg-app-surface shadow-xs overflow-hidden">
        <div className="p-4 border-b border-app flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold text-app-primary">Delivery Audit Ledger</h2>
            <p className="text-xs text-app-secondary mt-0.5">
              Individual recipient dispatch records, rendered message templates, and transmission receipts.
            </p>
          </div>

          <div className="flex items-center gap-2">
            {['', 'SENT', 'FAILED', 'PENDING'].map((st) => (
              <button
                key={st || 'ALL'}
                type="button"
                onClick={() => {
                  setStatusFilter(st);
                  setPage(0);
                }}
                className={`px-2.5 py-1 rounded-md text-[11px] font-medium border transition-colors cursor-pointer ${
                  statusFilter === st
                    ? 'bg-amber-500 text-white border-amber-600 font-semibold'
                    : 'bg-app-surface text-app-secondary border-app hover:bg-app-surface-subtle'
                }`}
              >
                {st || 'ALL'}
              </button>
            ))}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-app-surface-subtle border-b border-app text-app-secondary font-medium">
              <tr>
                <th className="py-3 px-4">Recipient</th>
                <th className="py-3 px-4">Email</th>
                <th className="py-3 px-4">Rendered Content</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Processed At</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-app">
              {deliveriesLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={5} className="py-3 px-4">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              ) : records.length > 0 ? (
                records.map((rec) => (
                  <tr key={rec.id} className="hover:bg-app-surface-subtle transition-colors">
                    <td className="py-3 px-4 font-semibold text-app-primary">
                      {rec.customerName || `Customer #${rec.customerId}`}
                    </td>
                    <td className="py-3 px-4 font-mono text-app-secondary">{rec.customerEmail}</td>
                    <td className="py-3 px-4 max-w-sm truncate text-app-secondary" title={rec.message}>
                      {rec.message}
                    </td>
                    <td className="py-3 px-4">
                      <Badge variant={rec.status.toLowerCase() as any} size="sm">
                        {rec.status}
                      </Badge>
                      {rec.failureReason && (
                        <p className="text-[10px] text-red-500 mt-0.5">{rec.failureReason}</p>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right text-app-muted font-mono">
                      {formatDateTime(rec.processedAt || rec.createdAt)}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="py-8 text-center">
                    <p className="text-xs text-app-muted">
                      No delivery recipient records found for this view.
                    </p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <Pagination metadata={pagination} onPageChange={setPage} isLoading={deliveriesLoading} />
      </div>
    </div>
  );
};
