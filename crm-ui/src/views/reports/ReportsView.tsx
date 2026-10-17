import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { formatCurrency, formatDateTime } from '@/lib/utils';
import { Badge } from '@/components/common/Badge';
import { Skeleton } from '@/components/common/Skeleton';
import type { CustomerOverviewReportResponse, CampaignHistoryItemResponse } from '@/types';
import { AlertCircle } from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';

export const ReportsView: React.FC = () => {
  // Fetch overview aggregates
  const {
    data: overviewRes,
    isLoading: overviewLoading,
    error: overviewError,
  } = useQuery({
    queryKey: ['reports', 'customers', 'overview'],
    queryFn: async () => {
      const res = await apiClient.get<CustomerOverviewReportResponse>('reports/customers/overview');
      return res.data;
    },
  });

  // Fetch campaign history
  const {
    data: historyRes,
    isLoading: historyLoading,
    error: historyError,
  } = useQuery({
    queryKey: ['reports', 'campaigns', 'history'],
    queryFn: async () => {
      const res = await apiClient.get<CampaignHistoryItemResponse[]>('reports/campaigns/history?page=0&size=20');
      return res.data;
    },
  });

  const overview = overviewRes;
  const history = historyRes || [];

  const chartColors = ['#d97706', '#f59e0b', '#fbbf24', '#fcd34d', '#78350f'];

  return (
    <div className="space-y-6 text-left max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-app">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-primary">
            Intelligence &amp; Reporting
          </h1>
          <p className="text-xs sm:text-sm text-app-secondary mt-0.5">
            Holistic portfolio health, regional customer density, and historical campaign dispatch analytics.
          </p>
        </div>
      </div>

      {(overviewError || historyError) && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>Error loading analytical reports from backend services.</span>
        </div>
      )}

      {/* KPI Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs">
          <span className="text-xs text-app-secondary font-medium">Customer Base</span>
          {overviewLoading ? (
            <Skeleton className="h-8 w-20 mt-2" />
          ) : (
            <div className="mt-2 text-2xl font-bold text-app-primary">
              {overview?.totalActiveCustomers ?? 0}
            </div>
          )}
        </div>

        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs">
          <span className="text-xs text-app-secondary font-medium">Gross Portfolio Spend</span>
          {overviewLoading ? (
            <Skeleton className="h-8 w-28 mt-2" />
          ) : (
            <div className="mt-2 text-2xl font-bold text-emerald-500">
              {formatCurrency(overview?.grossCustomerSpend)}
            </div>
          )}
        </div>

        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs">
          <span className="text-xs text-app-secondary font-medium">Average Value / Customer</span>
          {overviewLoading ? (
            <Skeleton className="h-8 w-28 mt-2" />
          ) : (
            <div className="mt-2 text-2xl font-bold text-amber-500">
              {formatCurrency(overview?.averageSpendPerCustomer)}
            </div>
          )}
        </div>

        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs">
          <span className="text-xs text-app-secondary font-medium">Recorded Visits</span>
          {overviewLoading ? (
            <Skeleton className="h-8 w-20 mt-2" />
          ) : (
            <div className="mt-2 text-2xl font-bold text-purple-400">
              {overview?.totalVisits ?? 0}
            </div>
          )}
        </div>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* City Breakdown Bar Chart */}
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs space-y-3">
          <h2 className="text-sm font-semibold text-app-primary">Regional Audience Density</h2>
          <p className="text-xs text-app-secondary">Customer volume partitioned by metropolitan hub</p>

          <div className="h-64 w-full flex items-center justify-center">
            {overviewLoading ? (
              <Skeleton className="w-full h-full" />
            ) : overview?.topLocations && overview.topLocations.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={overview.topLocations} margin={{ left: -10, right: 10, top: 10, bottom: 20 }}>
                  <XAxis dataKey="city" stroke="#71717a" fontSize={11} />
                  <YAxis stroke="#71717a" fontSize={11} allowDecimals={false} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'var(--bg-surface)',
                      borderColor: 'var(--border-color)',
                      color: 'var(--text-primary)',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                  />
                  <Bar dataKey="customerCount" radius={[4, 4, 0, 0]}>
                    {overview.topLocations.map((_, index) => (
                      <Cell key={`bar-${index}`} fill={chartColors[index % chartColors.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-xs text-app-muted">No geographical data registered.</p>
            )}
          </div>
        </div>

        {/* Campaign Execution Outcomes Pie Chart */}
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs space-y-3">
          <h2 className="text-sm font-semibold text-app-primary">Campaign Dispatch Performance</h2>
          <p className="text-xs text-app-secondary">Aggregated sent vs failed delivery status</p>

          <div className="h-64 w-full flex items-center justify-center">
            {historyLoading ? (
              <Skeleton className="w-full h-full" />
            ) : history.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={history.slice(0, 6)}
                  margin={{ left: -10, right: 10, top: 10, bottom: 20 }}
                >
                  <XAxis dataKey="name" stroke="#71717a" fontSize={10} interval={0} />
                  <YAxis stroke="#71717a" fontSize={11} allowDecimals={false} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'var(--bg-surface)',
                      borderColor: 'var(--border-color)',
                      color: 'var(--text-primary)',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                  />
                  <Bar dataKey="sentCount" fill="#22c55e" name="Sent" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="failedCount" fill="#ef4444" name="Failed" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-xs text-app-muted">No historical campaign metrics logged.</p>
            )}
          </div>
        </div>
      </div>

      {/* Historical Campaign Performance Table */}
      <div className="rounded-xl border border-app bg-app-surface shadow-xs overflow-hidden">
        <div className="p-4 border-b border-app">
          <h2 className="text-sm font-semibold text-app-primary">Historical Broadcast Log</h2>
          <p className="text-xs text-app-secondary mt-0.5">
            Audit ledger of launched broadcasts, audience targets, and transmission success metrics.
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-app-surface-subtle border-b border-app text-app-secondary font-medium">
              <tr>
                <th className="py-2.5 px-4">Campaign</th>
                <th className="py-2.5 px-4">Status</th>
                <th className="py-2.5 px-4">Target Audience</th>
                <th className="py-2.5 px-4">Dispatched</th>
                <th className="py-2.5 px-4">Delivery Rate</th>
                <th className="py-2.5 px-4 text-right">Launched</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-app">
              {historyLoading ? (
                [...Array(3)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={6} className="py-3 px-4">
                      <Skeleton className="h-5 w-full" />
                    </td>
                  </tr>
                ))
              ) : history.length > 0 ? (
                history.map((item) => {
                  const rate =
                    item.targetAudienceSize > 0
                      ? ((item.sentCount / item.targetAudienceSize) * 100).toFixed(1) + '%'
                      : '—';
                  return (
                    <tr key={item.id} className="hover:bg-app-surface-subtle transition-colors">
                      <td className="py-2.5 px-4 font-semibold text-app-primary">{item.name}</td>
                      <td className="py-2.5 px-4">
                        <Badge variant={item.status.toLowerCase() as any} size="sm">
                          {item.status}
                        </Badge>
                      </td>
                      <td className="py-2.5 px-4 font-mono text-app-secondary">
                        {item.targetAudienceSize}
                      </td>
                      <td className="py-2.5 px-4 font-mono text-emerald-500">{item.sentCount}</td>
                      <td className="py-2.5 px-4 font-mono font-semibold text-amber-500">{rate}</td>
                      <td className="py-2.5 px-4 text-right text-app-muted font-mono">
                        {formatDateTime(item.launchedAt)}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={6} className="py-6 text-center text-xs text-app-muted">
                    No historical campaign records available.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
