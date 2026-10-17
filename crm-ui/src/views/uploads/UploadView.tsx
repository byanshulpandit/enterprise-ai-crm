import React, { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { formatDateTime } from '@/lib/utils';
import { Button } from '@/components/common/Button';
import { Badge } from '@/components/common/Badge';
import { Skeleton } from '@/components/common/Skeleton';
import { Pagination } from '@/components/common/Pagination';
import type { UploadResultResponse, UploadHistoryDto } from '@/types';
import {
  UploadCloud,
  FileSpreadsheet,
  AlertTriangle,
  CheckCircle2,
  FileText,
  AlertCircle,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export const UploadView: React.FC = () => {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadResultResponse | null>(null);

  const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

  // Fetch upload history
  const {
    data: historyResponse,
    isLoading: historyLoading,
    error: _historyError,
  } = useQuery({
    queryKey: ['uploads', 'history', page, pageSize],
    queryFn: async () => {
      return await apiClient.get<UploadHistoryDto[]>(`uploads/history?page=${page}&size=${pageSize}`);
    },
  });

  const uploadHistory = historyResponse?.data || [];
  const pagination = historyResponse?.metadata?.pagination;

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      const res = await apiClient.post<UploadResultResponse>('uploads/bulk', formData);
      return res.data;
    },
    onSuccess: (result) => {
      setUploadResult(result);
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      queryClient.invalidateQueries({ queryKey: ['uploads', 'history'] });
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      queryClient.invalidateQueries({ queryKey: ['reports'] });
    },
    onError: (err: any) => {
      setFileError(err?.message || 'Upload processing failed on the server');
    },
  });

  const handleValidateFile = (file: File) => {
    setFileError(null);
    setUploadResult(null);

    const validExtensions = ['.csv', '.xlsx', '.xls'];
    const fileName = file.name.toLowerCase();
    const hasValidExt = validExtensions.some((ext) => fileName.endsWith(ext));

    if (!hasValidExt) {
      setFileError('Invalid file format. Please upload a standard CSV or XLSX spreadsheet.');
      return false;
    }

    if (file.size === 0) {
      setFileError('The selected file is empty (0 bytes). Please upload a valid data file.');
      return false;
    }

    if (file.size > MAX_FILE_SIZE) {
      setFileError('File size exceeds the maximum permitted limit of 10 MB.');
      return false;
    }

    setSelectedFile(file);
    return true;
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleValidateFile(e.dataTransfer.files[0]);
    }
  };

  const handleExecuteUpload = () => {
    if (!selectedFile) return;
    uploadMutation.mutate(selectedFile);
  };

  return (
    <div className="space-y-6 text-left max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-app">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-primary">
            Bulk Customer Ingestion
          </h1>
          <p className="text-xs sm:text-sm text-app-secondary mt-0.5">
            Import bulk customer records from CSV or Excel spreadsheets with transactional safety and row-level validation.
          </p>
        </div>
      </div>

      {fileError && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{fileError}</span>
        </div>
      )}

      {/* Drag & Drop Upload Zone */}
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragActive(true);
        }}
        onDragLeave={() => setDragActive(false)}
        onDrop={handleDrop}
        className={cn(
          'p-8 rounded-2xl border-2 border-dashed transition-all flex flex-col items-center justify-center text-center bg-app-surface shadow-xs',
          dragActive
            ? 'border-amber-500 bg-amber-500/5'
            : 'border-app hover:border-zinc-400 dark:hover:border-zinc-600'
        )}
      >
        <div className="w-14 h-14 rounded-full bg-app-surface-subtle border border-app flex items-center justify-center text-amber-500 mb-3">
          <UploadCloud className="w-7 h-7" />
        </div>

        <h3 className="text-base font-semibold text-app-primary">
          Select or drag and drop customer spreadsheet
        </h3>
        <p className="text-xs text-app-secondary max-w-sm mt-1 mb-4">
          Accepts standard CSV or XLSX files up to 10 MB. Requires column headers: firstName, lastName, email.
        </p>

        <input
          ref={fileInputRef}
          type="file"
          accept=".csv,.xlsx,.xls"
          onChange={(e) => {
            if (e.target.files && e.target.files.length > 0) {
              handleValidateFile(e.target.files[0]);
            }
          }}
          className="hidden"
          id="bulk-file-input"
        />

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={() => fileInputRef.current?.click()}
            leftIcon={<FileSpreadsheet className="w-4 h-4" />}
          >
            Browse Files
          </Button>

          {selectedFile && (
            <Button
              size="sm"
              onClick={handleExecuteUpload}
              isLoading={uploadMutation.isPending}
            >
              Upload {selectedFile.name}
            </Button>
          )}
        </div>

        {selectedFile && !uploadMutation.isPending && (
          <div className="mt-3 flex items-center gap-2 text-xs font-mono text-amber-500">
            <FileText className="w-3.5 h-3.5" />
            <span>
              {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
            </span>
          </div>
        )}
      </div>

      {/* Upload Execution Result Banner */}
      {uploadResult && (
        <div className="p-5 rounded-xl border border-app bg-app-surface shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              {uploadResult.status === 'SUCCESS' ? (
                <CheckCircle2 className="w-5 h-5 text-emerald-500" />
              ) : (
                <AlertTriangle className="w-5 h-5 text-amber-500" />
              )}
              <h2 className="text-sm font-semibold text-app-primary">
                Ingestion Completed: {uploadResult.status}
              </h2>
            </div>
            <Badge
              variant={uploadResult.status === 'SUCCESS' ? 'success' : 'warning'}
              size="sm"
            >
              {uploadResult.status}
            </Badge>
          </div>

          <div className="grid grid-cols-3 gap-3 text-center">
            <div className="p-3 rounded-lg bg-app-surface-subtle border border-app">
              <span className="text-[11px] text-app-secondary">Total Processed</span>
              <p className="text-lg font-bold text-app-primary mt-0.5">{uploadResult.totalRecords}</p>
            </div>
            <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20">
              <span className="text-[11px] text-emerald-600 dark:text-emerald-400">Successfully Imported</span>
              <p className="text-lg font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">
                {uploadResult.successfulRecords}
              </p>
            </div>
            <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20">
              <span className="text-[11px] text-red-600 dark:text-red-400">Row Failures</span>
              <p className="text-lg font-bold text-red-600 dark:text-red-400 mt-0.5">
                {uploadResult.failedRecords}
              </p>
            </div>
          </div>

          {/* Row-Level Errors Table */}
          {uploadResult.errors && uploadResult.errors.length > 0 && (
            <div className="pt-2">
              <h3 className="text-xs font-semibold text-red-500 mb-2">
                Row-Level Rejection Details:
              </h3>
              <div className="rounded-lg border border-red-900/30 overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-red-950/20 text-red-400 font-medium">
                    <tr>
                      <th className="py-2 px-3">Row #</th>
                      <th className="py-2 px-3">Email Identifier</th>
                      <th className="py-2 px-3">Failure Reason</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-red-900/20 bg-app-surface">
                    {uploadResult.errors.map((err, i) => (
                      <tr key={i}>
                        <td className="py-2 px-3 font-mono text-app-muted">{err.row}</td>
                        <td className="py-2 px-3 font-mono text-app-secondary">{err.email || '—'}</td>
                        <td className="py-2 px-3 text-red-500">{err.reason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Upload History Table */}
      <div className="rounded-xl border border-app bg-app-surface shadow-xs overflow-hidden">
        <div className="p-4 border-b border-app">
          <h2 className="text-sm font-semibold text-app-primary">Historical Upload Audit Trail</h2>
          <p className="text-xs text-app-secondary mt-0.5">
            Log of previously executed batch uploads, record counts, and execution status.
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-app-surface-subtle border-b border-app text-app-secondary font-medium">
              <tr>
                <th className="py-3 px-4">File Name</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Total Rows</th>
                <th className="py-3 px-4">Imported</th>
                <th className="py-3 px-4">Rejected</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-app">
              {historyLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={7} className="py-3 px-4">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              ) : uploadHistory.length > 0 ? (
                uploadHistory.map((hist) => (
                  <tr key={hist.id} className="hover:bg-app-surface-subtle transition-colors">
                    <td className="py-3 px-4 font-semibold text-app-primary">{hist.fileName}</td>
                    <td className="py-3 px-4 font-mono text-app-muted">{hist.fileType}</td>
                    <td className="py-3 px-4 text-app-secondary font-mono">{hist.totalRows}</td>
                    <td className="py-3 px-4 font-mono text-emerald-500">{hist.successCount}</td>
                    <td className="py-3 px-4 font-mono text-red-500">{hist.failureCount}</td>
                    <td className="py-3 px-4">
                      <Badge
                        variant={
                          hist.status === 'SUCCESS'
                            ? 'success'
                            : hist.status === 'PARTIAL_SUCCESS'
                            ? 'warning'
                            : 'failed'
                        }
                        size="sm"
                      >
                        {hist.status}
                      </Badge>
                    </td>
                    <td className="py-3 px-4 text-right text-app-muted font-mono">
                      {formatDateTime(hist.createdAt)}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="py-8 text-center">
                    <p className="text-xs text-app-muted">No historical upload audits logged yet.</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <Pagination metadata={pagination} onPageChange={setPage} isLoading={historyLoading} />
      </div>
    </div>
  );
};
