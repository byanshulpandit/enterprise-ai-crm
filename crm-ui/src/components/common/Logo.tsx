import React from 'react';
import { cn } from '@/lib/utils';

interface LogoProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg';
  showSubtitle?: boolean;
}

export const Logo: React.FC<LogoProps> = ({ className, size = 'md', showSubtitle = false }) => {
  const sizeMap = {
    sm: { icon: 'w-6 h-6 text-xs', text: 'text-base', sub: 'text-[9px]' },
    md: { icon: 'w-8 h-8 text-sm', text: 'text-lg', sub: 'text-[10px]' },
    lg: { icon: 'w-10 h-10 text-base', text: 'text-2xl', sub: 'text-xs' },
  };

  const current = sizeMap[size];

  return (
    <div className={cn('flex items-center gap-2.5 select-none', className)}>
      <div
        className={cn(
          'flex items-center justify-center font-bold rounded-lg tracking-wider font-mono shadow-sm',
          current.icon
        )}
        style={{
          backgroundColor: 'var(--accent-primary)',
          color: 'var(--accent-contrast)',
        }}
        aria-hidden="true"
      >
        AI
      </div>
      <div className="flex flex-col text-left">
        <span className={cn('font-bold tracking-tight leading-none text-app-primary', current.text)}>
          AI-CRM
        </span>
        {showSubtitle && (
          <span className={cn('text-app-muted uppercase font-medium tracking-wider mt-0.5', current.sub)}>
            Enterprise Platform
          </span>
        )}
      </div>
    </div>
  );
};
