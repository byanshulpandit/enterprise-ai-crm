import React from 'react';
import { cn } from '@/lib/utils';

export interface BadgeProps {
  children: React.ReactNode;
  variant?:
    | 'neutral'
    | 'admin'
    | 'marketer'
    | 'draft'
    | 'running'
    | 'completed'
    | 'failed'
    | 'success'
    | 'warning';
  size?: 'sm' | 'md';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  size = 'md',
  className,
}) => {
  const variantStyles = {
    neutral: 'bg-app-surface-subtle text-app-secondary border border-app',
    admin: 'bg-purple-950/40 text-purple-300 border border-purple-800/50',
    marketer: 'bg-blue-950/40 text-blue-300 border border-blue-800/50',
    draft: 'bg-zinc-800/40 text-zinc-300 border border-zinc-700/50',
    running: 'bg-amber-950/40 text-amber-300 border border-amber-800/50',
    completed: 'bg-emerald-950/40 text-emerald-300 border border-emerald-800/50',
    failed: 'bg-red-950/40 text-red-300 border border-red-800/50',
    success: 'bg-emerald-950/40 text-emerald-300 border border-emerald-800/50',
    warning: 'bg-amber-950/40 text-amber-300 border border-amber-800/50',
  };

  const sizeStyles = {
    sm: 'text-[10px] px-1.5 py-0.5 font-medium',
    md: 'text-xs px-2.5 py-0.5 font-medium',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center justify-center rounded-full uppercase tracking-wider font-mono select-none',
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
    >
      {children}
    </span>
  );
};
