import React from 'react';
import { useAuth } from '@/context/AuthContext';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Badge } from '@/components/common/Badge';
import { LogOut, User as UserIcon, Menu } from 'lucide-react';

interface TopBarProps {
  onMobileMenuToggle?: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({ onMobileMenuToggle }) => {
  const { user, logout, isAdmin } = useAuth();

  return (
    <header className="h-16 border-b border-app bg-app-surface px-4 md:px-6 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onMobileMenuToggle}
          className="md:hidden p-2 rounded-lg text-app-secondary hover:text-app-primary hover:bg-app-surface-subtle"
          aria-label="Toggle navigation menu"
        >
          <Menu className="w-5 h-5" />
        </button>
        <span className="text-xs uppercase tracking-wider font-semibold text-app-muted">
          Enterprise Workspace
        </span>
      </div>

      <div className="flex items-center gap-3 md:gap-4">
        <ThemeToggle />

        <div className="h-4 w-px bg-app-subtle hidden sm:block" />

        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full bg-app-surface-subtle border border-app-subtle flex items-center justify-center text-app-primary">
            <UserIcon className="w-4 h-4" />
          </div>
          <div className="hidden sm:flex flex-col text-left">
            <span className="text-xs font-semibold text-app-primary leading-tight">
              {user?.username || 'Authenticated User'}
            </span>
            <div className="mt-0.5">
              <Badge variant={isAdmin ? 'admin' : 'marketer'} size="sm">
                {isAdmin ? 'ADMIN' : 'MARKETER'}
              </Badge>
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={logout}
          className="p-2 rounded-lg text-app-muted hover:text-red-500 hover:bg-red-500/10 transition-colors focus-ring-accent cursor-pointer"
          title="Sign out"
          aria-label="Sign out"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};
