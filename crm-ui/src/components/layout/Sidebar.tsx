import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Logo } from '@/components/common/Logo';
import {
  LayoutDashboard,
  Users,
  Filter,
  Send,
  UploadCloud,
  Sparkles,
  BarChart3,
  ShieldAlert,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export const Sidebar: React.FC = () => {
  const { isAdmin } = useAuth();
  const [collapsed, setCollapsed] = useState(false);

  const navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { label: 'Customers', path: '/customers', icon: Users },
    { label: 'Segments', path: '/segments', icon: Filter },
    { label: 'Campaigns', path: '/campaigns', icon: Send },
    { label: 'Bulk Upload', path: '/uploads', icon: UploadCloud },
    { label: 'AI Assistant', path: '/ai', icon: Sparkles },
    { label: 'Reports', path: '/reports', icon: BarChart3 },
    ...(isAdmin
      ? [{ label: 'User Admin', path: '/admin/users', icon: ShieldAlert }]
      : []),
  ];

  return (
    <aside
      className={cn(
        'hidden md:flex flex-col border-r border-app bg-app-surface transition-all duration-200 select-none shrink-0 sticky top-0 h-screen',
        collapsed ? 'w-18' : 'w-64'
      )}
    >
      <div className="flex items-center justify-between p-4 border-b border-app h-16">
        {!collapsed ? (
          <Logo size="md" showSubtitle />
        ) : (
          <div className="w-full flex justify-center">
            <Logo size="sm" />
          </div>
        )}
      </div>

      <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors focus-ring-accent',
                  isActive
                    ? 'bg-accent-subtle text-accent-themed font-semibold'
                    : 'text-app-secondary hover:text-app-primary bg-app-surface-hover',
                  collapsed && 'justify-center px-0'
                )
              }
              title={collapsed ? item.label : undefined}
            >
              <Icon className="w-4 h-4 shrink-0" />
              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          );
        })}
      </nav>

      <div className="p-3 border-t border-app">
        <button
          type="button"
          onClick={() => setCollapsed(!collapsed)}
          className="w-full flex items-center justify-center p-2 rounded-lg text-app-muted hover:text-app-primary hover:bg-app-surface-subtle transition-colors cursor-pointer"
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
      </div>
    </aside>
  );
};
