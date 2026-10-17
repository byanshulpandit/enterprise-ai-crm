import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { formatDateTime } from '@/lib/utils';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Badge } from '@/components/common/Badge';
import { Modal } from '@/components/common/Modal';
import { Skeleton } from '@/components/common/Skeleton';
import { Pagination } from '@/components/common/Pagination';
import type { UserResponse, UserCreateRequest, Role } from '@/types';
import {
  ShieldAlert,
  UserPlus,
  KeyRound,
  UserX,
  AlertCircle,
} from 'lucide-react';

export const UserManagementView: React.FC = () => {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);

  // Modals state
  const [createUserOpen, setCreateUserOpen] = useState(false);
  const [resetPassUser, setResetPassUser] = useState<UserResponse | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  // New user form state
  const [newUsername, setNewUsername] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [createPassword, setCreatePassword] = useState('');
  const [createRole, setCreateRole] = useState<Role>('ROLE_MARKETER');

  // Fetch users
  const {
    data: userResponse,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['admin', 'users', page, pageSize],
    queryFn: async () => {
      return await apiClient.get<UserResponse[]>(`users?page=${page}&size=${pageSize}`);
    },
  });

  const users = userResponse?.data || [];
  const pagination = userResponse?.metadata?.pagination;

  // Create user mutation
  const createMutation = useMutation({
    mutationFn: async (payload: UserCreateRequest) => {
      return await apiClient.post<UserResponse>('users', payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      setCreateUserOpen(false);
      resetCreateForm();
    },
    onError: (err: any) => {
      setFormError(err?.message || 'Failed to provision user');
    },
  });

  // Role toggle mutation
  const roleMutation = useMutation({
    mutationFn: async ({ id, role }: { id: number; role: Role }) => {
      return await apiClient.patch<UserResponse>(`users/${id}/role`, { role });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (err: any) => {
      alert(`Role update failed: ${err?.message}`);
    },
  });

  // Deactivate mutation
  const deactivateMutation = useMutation({
    mutationFn: async (id: number) => {
      return await apiClient.patch<UserResponse>(`users/${id}/deactivate`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (err: any) => {
      alert(`Deactivation failed: ${err?.message}`);
    },
  });

  // Reset password mutation
  const resetPasswordMutation = useMutation({
    mutationFn: async ({ id, password }: { id: number; password: string }) => {
      return await apiClient.patch(`users/${id}/password`, { password });
    },
    onSuccess: () => {
      setResetPassUser(null);
      setNewPassword('');
      alert('Password updated successfully.');
    },
    onError: (err: any) => {
      setFormError(err?.message || 'Failed to update user password');
    },
  });

  const resetCreateForm = () => {
    setNewUsername('');
    setNewEmail('');
    setCreatePassword('');
    setCreateRole('ROLE_MARKETER');
    setFormError(null);
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUsername.trim() || !newEmail.trim() || !createPassword) {
      setFormError('All fields are mandatory.');
      return;
    }
    setFormError(null);
    createMutation.mutate({
      username: newUsername.trim(),
      email: newEmail.trim().toLowerCase(),
      password: createPassword,
      role: createRole,
    });
  };

  const handleResetPasswordSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetPassUser || !newPassword) return;
    if (newPassword.length < 8) {
      setFormError('Password must be at least 8 characters.');
      return;
    }
    setFormError(null);
    resetPasswordMutation.mutate({
      id: resetPassUser.id,
      password: newPassword,
    });
  };

  return (
    <div className="space-y-6 text-left max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-app">
        <div>
          <div className="flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-purple-400" />
            <h1 className="text-2xl font-bold tracking-tight text-app-primary">
              Enterprise User Administration
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-app-secondary mt-0.5">
            Provision user accounts, configure role hierarchies, and manage administrative credentials.
          </p>
        </div>
        <Button
          size="sm"
          onClick={() => {
            resetCreateForm();
            setCreateUserOpen(true);
          }}
          leftIcon={<UserPlus className="w-4 h-4" />}
        >
          Provision User
        </Button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>Error loading user directory: {(error as Error).message}</span>
        </div>
      )}

      {/* Users Table */}
      <div className="rounded-xl border border-app bg-app-surface shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-app-surface-subtle border-b border-app text-app-secondary font-medium">
              <tr>
                <th className="py-3 px-4">User</th>
                <th className="py-3 px-4">Corporate Email</th>
                <th className="py-3 px-4">Role</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Created Date</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-app">
              {isLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={6} className="py-3 px-4">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              ) : users.length > 0 ? (
                users.map((u) => (
                  <tr key={u.id} className="hover:bg-app-surface-subtle transition-colors">
                    <td className="py-3 px-4 font-semibold text-app-primary">{u.username}</td>
                    <td className="py-3 px-4 font-mono text-app-secondary">{u.email}</td>
                    <td className="py-3 px-4">
                      <Badge variant={u.role === 'ROLE_ADMIN' ? 'admin' : 'marketer'} size="sm">
                        {u.role === 'ROLE_ADMIN' ? 'ADMIN' : 'MARKETER'}
                      </Badge>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center gap-1 text-[11px] font-medium ${
                          u.isActive ? 'text-emerald-500' : 'text-zinc-500'
                        }`}
                      >
                        <span
                          className={`w-1.5 h-1.5 rounded-full ${
                            u.isActive ? 'bg-emerald-500' : 'bg-zinc-500'
                          }`}
                        />
                        {u.isActive ? 'Active' : 'Deactivated'}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-app-muted font-mono">
                      {formatDateTime(u.createdAt)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Role toggle button */}
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            const newRole: Role =
                              u.role === 'ROLE_ADMIN' ? 'ROLE_MARKETER' : 'ROLE_ADMIN';
                            roleMutation.mutate({ id: u.id, role: newRole });
                          }}
                        >
                          Switch to {u.role === 'ROLE_ADMIN' ? 'Marketer' : 'Admin'}
                        </Button>

                        {/* Reset password button */}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setFormError(null);
                            setNewPassword('');
                            setResetPassUser(u);
                          }}
                          title="Reset Password"
                        >
                          <KeyRound className="w-3.5 h-3.5" />
                        </Button>

                        {/* Deactivate button */}
                        {u.isActive && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-500 hover:text-red-600 hover:bg-red-500/10"
                            onClick={() => {
                              if (confirm(`Deactivate user "${u.username}"?`)) {
                                deactivateMutation.mutate(u.id);
                              }
                            }}
                            title="Deactivate User"
                          >
                            <UserX className="w-3.5 h-3.5" />
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-xs text-app-muted">
                    No users registered in directory.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <Pagination metadata={pagination} onPageChange={setPage} isLoading={isLoading} />
      </div>

      {/* Provision User Modal */}
      <Modal
        open={createUserOpen}
        onOpenChange={(open) => {
          if (!open) {
            setCreateUserOpen(false);
            resetCreateForm();
          }
        }}
        title="Provision Enterprise User"
        description="Create an authenticated user account with specified platform role permissions."
        maxWidth="md"
      >
        {formError && (
          <div className="mb-4 p-3 rounded-lg bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <Input
            label="Username"
            required
            placeholder="e.g. sarah_marketer"
            value={newUsername}
            onChange={(e) => setNewUsername(e.target.value)}
          />

          <Input
            label="Email Address"
            type="email"
            required
            placeholder="sarah@crm.internal"
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
          />

          <Input
            label="Initial Password"
            type="password"
            required
            placeholder="At least 8 characters"
            value={createPassword}
            onChange={(e) => setCreatePassword(e.target.value)}
          />

          <div className="text-left space-y-1.5">
            <label className="text-xs font-medium text-app-secondary">Assigned Role</label>
            <select
              value={createRole}
              onChange={(e) => setCreateRole(e.target.value as Role)}
              className="w-full text-xs rounded-lg border border-app bg-app-surface text-app-primary px-3 py-2.5 focus-ring-accent"
            >
              <option value="ROLE_MARKETER">MARKETER (Standard Platform Operator)</option>
              <option value="ROLE_ADMIN">ADMIN (Full Security &amp; Administrative Control)</option>
            </select>
          </div>

          <div className="pt-4 border-t border-app flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                setCreateUserOpen(false);
                resetCreateForm();
              }}
            >
              Cancel
            </Button>
            <Button type="submit" size="sm" isLoading={createMutation.isPending}>
              Provision Account
            </Button>
          </div>
        </form>
      </Modal>

      {/* Reset Password Modal */}
      <Modal
        open={resetPassUser !== null}
        onOpenChange={(open) => {
          if (!open) {
            setResetPassUser(null);
            setNewPassword('');
          }
        }}
        title={`Reset Password for ${resetPassUser?.username}`}
        description="Assign a new secure password. The change will take effect immediately."
        maxWidth="sm"
      >
        {formError && (
          <div className="mb-4 p-3 rounded-lg bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        <form onSubmit={handleResetPasswordSubmit} className="space-y-4">
          <Input
            label="New Password"
            type="password"
            required
            placeholder="Min 8 characters"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />

          <div className="pt-4 border-t border-app flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                setResetPassUser(null);
                setNewPassword('');
              }}
            >
              Cancel
            </Button>
            <Button type="submit" size="sm" isLoading={resetPasswordMutation.isPending}>
              Update Password
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
