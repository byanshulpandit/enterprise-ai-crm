import React, { useState } from 'react';
import { Outlet, NavLink } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { Logo } from '@/components/common/Logo';
import { useAuth } from '@/context/AuthContext';
import {
  LayoutDashboard,
  Users,
  Filter,
  Send,
  UploadCloud,
  Sparkles,
  BarChart3,
  ShieldAlert,
  X,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export const AppLayout: React.FC = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { isAdmin } = useAuth();

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
    <div className="min-h-screen flex bg-app-main text-app-primary">
      {/* Desktop Sidebar */}
      <Sidebar />

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-xs"
            onClick={() => setMobileOpen(false)}
            aria-hidden="true"
          />
          <div className="relative w-64 max-w-[80%] bg-app-surface border-r border-app p-4 flex flex-col z-10 animate-in slide-in-from-left duration-200">
            <div className="flex items-center justify-between pb-4 border-b border-app">
              <Logo size="sm" showSubtitle />
              <button
                type="button"
                onClick={() => setMobileOpen(false)}
                className="p-1 rounded-md text-app-muted hover:text-app-primary"
                aria-label="Close menu"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <nav className="flex-1 py-4 space-y-1 overflow-y-auto">
              {navItems.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    onClick={() => setMobileOpen(false)}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                        isActive
                          ? 'bg-accent-subtle text-accent-themed font-semibold'
                          : 'text-app-secondary hover:text-app-primary hover:bg-app-surface-subtle'
                      )
                    }
                  >
                    <Icon className="w-4 h-4 shrink-0" />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </nav>
          </div>
        </div>
      )}

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        <TopBar onMobileMenuToggle={() => setMobileOpen(true)} />
        <main className="flex-1 p-4 md:p-6 lg:p-8 overflow-y-auto max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
