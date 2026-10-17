import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '@/lib/query-client';
import { AuthProvider } from '@/context/AuthContext';
import { ThemeProvider } from '@/context/ThemeContext';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';

import { LoginView } from '@/views/auth/LoginView';
import { DashboardView } from '@/views/dashboard/DashboardView';
import { CustomerListView } from '@/views/customers/CustomerListView';
import { SegmentListView } from '@/views/segments/SegmentListView';
import { SegmentBuilderView } from '@/views/segments/SegmentBuilderView';
import { CampaignListView } from '@/views/campaigns/CampaignListView';
import { CampaignCreateView } from '@/views/campaigns/CampaignCreateView';
import { CampaignMonitorView } from '@/views/campaigns/CampaignMonitorView';
import { UploadView } from '@/views/uploads/UploadView';
import { AiSegmentView } from '@/views/ai/AiSegmentView';
import { ReportsView } from '@/views/reports/ReportsView';
import { UserManagementView } from '@/views/admin/UserManagementView';
import { NotFoundView } from '@/views/error/NotFoundView';
import { UnauthorizedView } from '@/views/error/UnauthorizedView';

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              {/* Public Routes */}
              <Route path="/login" element={<LoginView />} />
              <Route path="/unauthorized" element={<UnauthorizedView />} />

              {/* Protected Authenticated Routes */}
              <Route
                element={
                  <ProtectedRoute>
                    <AppLayout />
                  </ProtectedRoute>
                }
              >
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<DashboardView />} />
                <Route path="/customers" element={<CustomerListView />} />
                <Route path="/segments" element={<SegmentListView />} />
                <Route path="/segments/new" element={<SegmentBuilderView />} />
                <Route path="/campaigns" element={<CampaignListView />} />
                <Route path="/campaigns/new" element={<CampaignCreateView />} />
                <Route path="/campaigns/:id/monitor" element={<CampaignMonitorView />} />
                <Route path="/uploads" element={<UploadView />} />
                <Route path="/ai" element={<AiSegmentView />} />
                <Route path="/reports" element={<ReportsView />} />

                {/* Protected Administrative Routes (ROLE_ADMIN only) */}
                <Route
                  path="/admin/users"
                  element={
                    <ProtectedRoute requiredRole="ROLE_ADMIN">
                      <UserManagementView />
                    </ProtectedRoute>
                  }
                />
              </Route>

              {/* 404 Fallback */}
              <Route path="*" element={<NotFoundView />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

export default App;
