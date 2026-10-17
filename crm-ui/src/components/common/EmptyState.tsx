import React from 'react';
import { cn } from '@/lib/utils';
import { Button } from './Button';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  className,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center p-8 text-center border border-dashed border-app rounded-xl bg-app-surface/50 my-4',
        className
      )}
    >
      {icon && (
        <div className="w-12 h-12 rounded-full bg-app-surface-subtle border border-app-subtle flex items-center justify-center text-app-muted mb-3">
          {icon}
        </div>
      )}
      <h3 className="text-base font-semibold text-app-primary">{title}</h3>
      {description && (
        <p className="text-xs text-app-secondary max-w-sm mt-1 mb-4">{description}</p>
      )}
      {actionLabel && onAction && (
        <Button size="sm" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
};
