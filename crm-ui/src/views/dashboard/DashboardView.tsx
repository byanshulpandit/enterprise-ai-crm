import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { formatCurrency, formatDateTime } from '@/lib/utils';
import { Badge } from '@/components/common/Badge';
import { Skeleton } from '@/components/common/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/common/Button';
import { useNavigate } from 'react-router-dom';
import type { CustomerOverviewReportResponse, Campaign } from '@/types';
import {
  Users,
  DollarSign,
  TrendingUp,
  Activity,
  Send,
  Plus,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';

export const DashboardView: React.FC = () => {
  const navigate = useNavigate();

  const {
    data: overview,
    isLoading: overviewLoading,
    error: overviewError,
    refetch: refetchOverview,
  } = useQuery({
    queryKey: ['reports', 'customers', 'overview'],
    queryFn: async () => {
      const res = await apiClient.get<CustomerOverviewReportResponse>('reports/customers/overview');
      return res.data;
    },
  });

  const {
    data: recentCampaigns,
    isLoading: campaignsLoading,
    error: campaignsError,
    refetch: refetchCampaigns,
  } = useQuery({
    queryKey: ['campaigns', 'recent'],
    queryFn: async () => {
      const res = await apiClient.get<Campaign[]>('campaigns?page=0&size=5&sort=id,desc');
      return res.data;
    },
  });

  const chartColors = ['#d97706', '#f59e0b', '#fbbf24', '#fcd34d', '#78350f'];

  return (
    <div className="space-y-6 text-left">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-app">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-primary">Dashboard</h1>
          <p className="text-xs sm:text-sm text-app-secondary mt-0.5">
            Real-time customer engagement metrics and campaign telemetry.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              refetchOverview();
              refetchCampaigns();
            }}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Refresh
          </Button>
          <Button
            size="sm"
            onClick={() => navigate('/campaigns/new')}
            leftIcon={<Plus className="w-3.5 h-3.5" />}
          >
            Create Campaign
          </Button>
        </div>
      </div>

      {overviewError && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>Failed to load customer metrics: {(overviewError as Error).message}</span>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Customers */}
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-app-secondary">Active Customers</span>
            <div className="p-2 rounded-lg bg-amber-500/10 text-amber-500">
              <Users className="w-4 h-4" />
            </div>
          </div>
          {overviewLoading ? (
            <Skeleton className="h-8 w-24 mt-3" />
          ) : (
            <div className="mt-3">
              <span className="text-2xl font-bold text-app-primary">
                {overview?.totalActiveCustomers ?? 0}
              </span>
              <p className="text-[11px] text-app-muted mt-1">Verified customer profiles</p>
            </div>
          )}
        </div>

        {/* Gross Customer Spend */}
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-app-secondary">Gross Revenue</span>
            <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-500">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          {overviewLoading ? (
            <Skeleton className="h-8 w-28 mt-3" />
          ) : (
            <div className="mt-3">
              <span className="text-2xl font-bold text-app-primary">
                {formatCurrency(overview?.grossCustomerSpend)}
              </span>
              <p className="text-[11px] text-app-muted mt-1">Total portfolio transaction value</p>
            </div>
          )}
        </div>

        {/* Average Spend */}
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-app-secondary">Avg Spend / Customer</span>
            <div className="p-2 rounded-lg bg-blue-500/10 text-blue-500">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          {overviewLoading ? (
            <Skeleton className="h-8 w-28 mt-3" />
          ) : (
            <div className="mt-3">
              <span className="text-2xl font-bold text-app-primary">
                {formatCurrency(overview?.averageSpendPerCustomer)}
              </span>
              <p className="text-[11px] text-app-muted mt-1">Mean customer value</p>
            </div>
          )}
        </div>

        {/* Total Engagements */}
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-app-secondary">Total Visits</span>
            <div className="p-2 rounded-lg bg-purple-500/10 text-purple-500">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          {overviewLoading ? (
            <Skeleton className="h-8 w-20 mt-3" />
          ) : (
            <div className="mt-3">
              <span className="text-2xl font-bold text-app-primary">
                {overview?.totalVisits ?? 0}
              </span>
              <p className="text-[11px] text-app-muted mt-1">
                Avg {overview?.averageVisitsPerCustomer?.toFixed(1) ?? '0.0'} visits/user
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Main Grid: Chart & Recent Campaigns */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Customer Location Distribution Chart */}
        <div className="lg:col-span-1 p-5 rounded-xl border border-app bg-app-surface shadow-xs flex flex-col justify-between">
          <div>
            <h2 className="text-base font-semibold text-app-primary">Top Customer Locations</h2>
            <p className="text-xs text-app-secondary mt-0.5">Audience concentration by metropolitan region</p>
          </div>

          <div className="h-64 mt-4 w-full flex items-center justify-center">
            {overviewLoading ? (
              <Skeleton className="w-full h-full" />
            ) : overview?.topLocations && overview.topLocations.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={overview.topLocations} layout="vertical" margin={{ left: 10, right: 10, top: 10, bottom: 10 }}>
                  <XAxis type="number" allowDecimals={false} stroke="#71717a" fontSize={11} />
                  <YAxis type="category" dataKey="city" stroke="#71717a" fontSize={11} width={80} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'var(--bg-surface)',
                      borderColor: 'var(--border-color)',
                      color: 'var(--text-primary)',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                  />
                  <Bar dataKey="customerCount" radius={[0, 4, 4, 0]}>
                    {overview.topLocations.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={chartColors[index % chartColors.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-xs text-app-muted">No location data available</p>
            )}
          </div>
        </div>

        {/* Recent Campaigns */}
        <div className="lg:col-span-2 p-5 rounded-xl border border-app bg-app-surface shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-semibold text-app-primary">Recent Campaigns</h2>
              <p className="text-xs text-app-secondary mt-0.5">Live status and reach of recently dispatched communications</p>
            </div>
            <Button variant="ghost" size="sm" onClick={() => navigate('/campaigns')}>
              View All
            </Button>
          </div>

          <div className="flex-1 overflow-x-auto">
            {campaignsLoading ? (
              <div className="space-y-3">
                {[...Array(4)].map((_, i) => (
                  <Skeleton key={i} className="h-12 w-full" />
                ))}
              </div>
            ) : campaignsError ? (
              <div className="p-4 text-xs text-red-500 bg-red-950/20 border border-red-800/40 rounded-lg">
                Failed to load campaigns: {(campaignsError as Error).message}
              </div>
            ) : recentCampaigns && recentCampaigns.length > 0 ? (
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-app text-app-secondary">
                    <th className="py-2.5 font-medium">Campaign Name</th>
                    <th className="py-2.5 font-medium">Segment</th>
                    <th className="py-2.5 font-medium">Status</th>
                    <th className="py-2.5 font-medium">Created</th>
                    <th className="py-2.5 font-medium text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-app">
                  {recentCampaigns.map((camp) => (
                    <tr key={camp.id} className="hover:bg-app-surface-subtle transition-colors">
                      <td className="py-3 font-semibold text-app-primary">{camp.name}</td>
                      <td className="py-3 text-app-secondary">{camp.segmentName}</td>
                      <td className="py-3">
                        <Badge variant={camp.status.toLowerCase() as any} size="sm">
                          {camp.status}
                        </Badge>
                      </td>
                      <td className="py-3 text-app-muted">{formatDateTime(camp.createdAt)}</td>
                      <td className="py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/campaigns/${camp.id}/monitor`)}
                        >
                          Telemetry
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <EmptyState
                icon={<Send className="w-6 h-6" />}
                title="No campaigns initiated yet"
                description="Draft and dispatch your first marketing or transactional broadcast."
                actionLabel="Create Campaign"
                onAction={() => navigate('/campaigns/new')}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
