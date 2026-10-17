import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '@/lib/api-client';
import { formatDateTime } from '@/lib/utils';
import { Button } from '@/components/common/Button';
import { Skeleton } from '@/components/common/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import { Modal } from '@/components/common/Modal';
import { Pagination } from '@/components/common/Pagination';
import type { Segment, SegmentPreviewResponse } from '@/types';
import {
  Filter,
  Plus,
  Trash2,
  Eye,
  AlertCircle,
  Clock,
} from 'lucide-react';

export const SegmentListView: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [previewSegmentId, setPreviewSegmentId] = useState<number | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  // Fetch segments
  const {
    data: segmentResponse,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['segments', page, pageSize],
    queryFn: async () => {
      return await apiClient.get<Segment[]>(`segments?page=${page}&size=${pageSize}&sort=id,desc`);
    },
  });

  const segments = segmentResponse?.data || [];
  const pagination = segmentResponse?.metadata?.pagination;

  // Preview query
  const {
    data: previewData,
    isLoading: previewLoading,
  } = useQuery({
    queryKey: ['segments', 'preview', previewSegmentId],
    queryFn: async () => {
      if (!previewSegmentId) return null;
      const res = await apiClient.post<SegmentPreviewResponse>(`segments/${previewSegmentId}/preview`);
      return res.data;
    },
    enabled: previewSegmentId !== null,
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      return await apiClient.delete(`segments/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['segments'] });
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
          <h1 className="text-2xl font-bold tracking-tight text-app-primary">Audience Segments</h1>
          <p className="text-xs sm:text-sm text-app-secondary mt-0.5">
            Construct recursive logic trees to filter customers by transaction value, frequency, and location.
          </p>
        </div>
        <Button
          size="sm"
          onClick={() => navigate('/segments/new')}
          leftIcon={<Plus className="w-4 h-4" />}
        >
          New Segment
        </Button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>Error loading segments: {(error as Error).message}</span>
        </div>
      )}

      {/* Segments Table */}
      <div className="rounded-xl border border-app bg-app-surface shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-app-surface-subtle border-b border-app text-app-secondary font-medium">
              <tr>
                <th className="py-3 px-4">Segment Name</th>
                <th className="py-3 px-4">Description</th>
                <th className="py-3 px-4">Created By</th>
                <th className="py-3 px-4">Created</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-app">
              {isLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={5} className="py-3 px-4">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              ) : segments.length > 0 ? (
                segments.map((segment) => (
                  <tr key={segment.id} className="hover:bg-app-surface-subtle transition-colors">
                    <td className="py-3 px-4 font-semibold text-app-primary">
                      <div className="flex items-center gap-2">
                        <Filter className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                        <span>{segment.name}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-app-secondary max-w-xs truncate">
                      {segment.description || '—'}
                    </td>
                    <td className="py-3 px-4 text-app-muted">
                      {segment.createdByName || `User #${segment.createdBy}`}
                    </td>
                    <td className="py-3 px-4 text-app-muted font-mono">
                      {formatDateTime(segment.createdAt)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setPreviewSegmentId(segment.id)}
                          leftIcon={<Eye className="w-3.5 h-3.5" />}
                        >
                          Evaluate
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-500 hover:text-red-600 hover:bg-red-500/10"
                          onClick={() => setDeleteConfirmId(segment.id)}
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="py-8">
                    <EmptyState
                      icon={<Filter className="w-6 h-6" />}
                      title="No segments configured"
                      description="Build criteria trees to group your customers for targeted marketing broadcasts."
                      actionLabel="Create Segment"
                      onAction={() => navigate('/segments/new')}
                    />
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <Pagination metadata={pagination} onPageChange={setPage} isLoading={isLoading} />
      </div>

      {/* Audience Evaluation Modal */}
      <Modal
        open={previewSegmentId !== null}
        onOpenChange={(open) => !open && setPreviewSegmentId(null)}
        title="Audience Evaluation Result"
        description="Real-time execution of the segment AST against the live customer database."
        maxWidth="md"
      >
        <div className="py-4 space-y-4">
          {previewLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-6 w-3/4" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : previewData ? (
            <div className="p-4 rounded-xl border border-app bg-app-surface-subtle space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-app-secondary">Segment Target:</span>
                <span className="text-xs font-semibold text-app-primary">
                  {previewData.segmentName}
                </span>
              </div>
              <div className="flex items-center justify-between pt-2 border-t border-app">
                <span className="text-xs text-app-secondary">Matched Customer Count:</span>
                <span className="text-xl font-bold text-amber-500 font-mono">
                  {previewData.matchedAudienceCount}
                </span>
              </div>
              <div className="flex items-center justify-between text-[11px] text-app-muted pt-2 border-t border-app">
                <span className="flex items-center gap-1">
                  <Clock className="w-3 h-3" /> Evaluated At
                </span>
                <span>{formatDateTime(previewData.evaluatedAt)}</span>
              </div>
            </div>
          ) : (
            <p className="text-xs text-app-muted">No evaluation metrics available.</p>
          )}

          <div className="flex justify-end pt-2">
            <Button variant="outline" size="sm" onClick={() => setPreviewSegmentId(null)}>
              Close
            </Button>
          </div>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={deleteConfirmId !== null}
        onOpenChange={(open) => !open && setDeleteConfirmId(null)}
        title="Delete Segment"
        description="Are you sure you want to delete this segment rule? Existing campaigns using this segment may be affected."
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
