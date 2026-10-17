import React from 'react';
import type { PageMetadata } from '@/types';
import { Button } from './Button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export interface PaginationProps {
  metadata?: PageMetadata;
  onPageChange: (newPage: number) => void;
  isLoading?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
  metadata,
  onPageChange,
  isLoading = false,
}) => {
  if (!metadata || metadata.totalPages <= 1) {
    return null;
  }

  const { page, totalPages, totalElements } = metadata;

  return (
    <div className="flex items-center justify-between px-4 py-3 border-t border-app text-xs text-app-secondary">
      <div>
        Showing <span className="font-medium text-app-primary">page {page + 1}</span> of{' '}
        <span className="font-medium text-app-primary">{totalPages}</span> ({totalElements} total items)
      </div>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0 || isLoading}
          leftIcon={<ChevronLeft className="w-3.5 h-3.5" />}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1 || isLoading}
          rightIcon={<ChevronRight className="w-3.5 h-3.5" />}
        >
          Next
        </Button>
      </div>
    </div>
  );
};
