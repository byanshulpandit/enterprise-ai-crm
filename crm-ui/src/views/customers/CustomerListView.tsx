import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { useAuth } from '@/context/AuthContext';
import { formatCurrency } from '@/lib/utils';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Badge } from '@/components/common/Badge';
import { Modal } from '@/components/common/Modal';
import { Pagination } from '@/components/common/Pagination';
import { Skeleton } from '@/components/common/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import type { Customer, CustomerRequestDto } from '@/types';
import {
  Users,
  Search,
  Plus,
  Trash2,
  Edit2,
  Filter,
  AlertCircle,
  Tag,
} from 'lucide-react';

export const CustomerListView: React.FC = () => {
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();

  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState('');
  const [cityFilter, setCityFilter] = useState('');

  // Modals state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editCustomer, setEditCustomer] = useState<Customer | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  // Form State
  const [formData, setFormData] = useState<CustomerRequestDto>({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    city: '',
    country: '',
    totalSpend: 0,
    visitCount: 0,
    tags: [],
  });
  const [tagInput, setTagInput] = useState('');

  // Fetch customers
  const {
    data: customerResponse,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['customers', page, pageSize, searchTerm, cityFilter],
    queryFn: async () => {
      let endpoint = `customers?page=${page}&size=${pageSize}&sort=id,desc`;
      if (searchTerm.trim()) {
        endpoint += `&search=${encodeURIComponent(searchTerm.trim())}`;
      }
      if (cityFilter.trim()) {
        endpoint += `&city=${encodeURIComponent(cityFilter.trim())}`;
      }
      return await apiClient.get<Customer[]>(endpoint);
    },
  });

  const customers = customerResponse?.data || [];
  const pagination = customerResponse?.metadata?.pagination;

  // Create mutation
  const createMutation = useMutation({
    mutationFn: async (payload: CustomerRequestDto) => {
      return await apiClient.post<Customer>('customers', payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      setCreateModalOpen(false);
      resetForm();
    },
    onError: (err: any) => {
      setFormError(err?.message || 'Failed to create customer');
    },
  });

  // Edit mutation
  const editMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: number; payload: CustomerRequestDto }) => {
      return await apiClient.put<Customer>(`customers/${id}`, payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      setEditCustomer(null);
      resetForm();
    },
    onError: (err: any) => {
      setFormError(err?.message || 'Failed to update customer');
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      return await apiClient.delete(`customers/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      setDeleteConfirmId(null);
    },
    onError: (err: any) => {
      alert(`Delete failed: ${err?.message || 'Access denied'}`);
    },
  });

  const resetForm = () => {
    setFormData({
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      city: '',
      country: '',
      totalSpend: 0,
      visitCount: 0,
      tags: [],
    });
    setTagInput('');
    setFormError(null);
  };

  const handleOpenEdit = (c: Customer) => {
    setFormData({
      firstName: c.firstName,
      lastName: c.lastName,
      email: c.email,
      phone: c.phone || '',
      city: c.city || '',
      country: c.country || '',
      totalSpend: c.totalSpend,
      visitCount: c.visitCount,
      tags: c.tags || [],
    });
    setTagInput('');
    setFormError(null);
    setEditCustomer(c);
  };

  const handleAddTag = () => {
    if (!tagInput.trim()) return;
    const cleanTag = tagInput.trim().toLowerCase();
    if (!formData.tags?.includes(cleanTag)) {
      setFormData({ ...formData, tags: [...(formData.tags || []), cleanTag] });
    }
    setTagInput('');
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setFormData({
      ...formData,
      tags: (formData.tags || []).filter((t) => t !== tagToRemove),
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editCustomer) {
      editMutation.mutate({ id: editCustomer.id, payload: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  return (
    <div className="space-y-6 text-left">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-app">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-primary">Customers</h1>
          <p className="text-xs sm:text-sm text-app-secondary mt-0.5">
            Manage your customer database, segment affiliations, and historical transaction spend.
          </p>
        </div>
        <Button
          size="sm"
          onClick={() => {
            resetForm();
            setCreateModalOpen(true);
          }}
          leftIcon={<Plus className="w-4 h-4" />}
        >
          Add Customer
        </Button>
      </div>

      {/* Filter Bar */}
      <div className="p-4 rounded-xl border border-app bg-app-surface shadow-xs flex flex-col sm:flex-row items-center gap-3">
        <div className="flex-1 w-full relative">
          <Input
            placeholder="Search by name, email, or telephone..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(0);
            }}
            leftIcon={<Search className="w-4 h-4" />}
          />
        </div>
        <div className="w-full sm:w-64">
          <Input
            placeholder="Filter by city..."
            value={cityFilter}
            onChange={(e) => {
              setCityFilter(e.target.value);
              setPage(0);
            }}
            leftIcon={<Filter className="w-4 h-4" />}
          />
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>Error loading customers: {(error as Error).message}</span>
        </div>
      )}

      {/* Customers Table */}
      <div className="rounded-xl border border-app bg-app-surface shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-app-surface-subtle border-b border-app text-app-secondary font-medium">
              <tr>
                <th className="py-3 px-4">Customer Name</th>
                <th className="py-3 px-4">Email</th>
                <th className="py-3 px-4">Location</th>
                <th className="py-3 px-4">Total Spend</th>
                <th className="py-3 px-4">Visits</th>
                <th className="py-3 px-4">Tags</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-app">
              {isLoading ? (
                [...Array(6)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={7} className="py-3 px-4">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              ) : customers.length > 0 ? (
                customers.map((customer) => (
                  <tr key={customer.id} className="hover:bg-app-surface-subtle transition-colors">
                    <td className="py-3 px-4 font-semibold text-app-primary">
                      {customer.firstName} {customer.lastName}
                    </td>
                    <td className="py-3 px-4 text-app-secondary font-mono">{customer.email}</td>
                    <td className="py-3 px-4 text-app-muted">
                      {customer.city ? `${customer.city}${customer.country ? `, ${customer.country}` : ''}` : '—'}
                    </td>
                    <td className="py-3 px-4 font-semibold text-app-primary">
                      {formatCurrency(customer.totalSpend)}
                    </td>
                    <td className="py-3 px-4 text-app-secondary">{customer.visitCount}</td>
                    <td className="py-3 px-4">
                      <div className="flex flex-wrap gap-1">
                        {customer.tags && customer.tags.length > 0 ? (
                          customer.tags.map((t) => (
                            <Badge key={t} size="sm" variant="neutral">
                              {t}
                            </Badge>
                          ))
                        ) : (
                          <span className="text-app-muted">—</span>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleOpenEdit(customer)}
                          aria-label={`Edit ${customer.firstName}`}
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </Button>
                        {isAdmin && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-500 hover:text-red-600 hover:bg-red-500/10"
                            onClick={() => setDeleteConfirmId(customer.id)}
                            aria-label={`Delete ${customer.firstName}`}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="py-8">
                    <EmptyState
                      icon={<Users className="w-6 h-6" />}
                      title="No customers found"
                      description={
                        searchTerm || cityFilter
                          ? 'Try adjusting your search criteria or clear the filters.'
                          : 'Get started by creating your first customer profile or uploading a batch CSV.'
                      }
                      actionLabel={searchTerm || cityFilter ? undefined : 'Add Customer'}
                      onAction={() => {
                        resetForm();
                        setCreateModalOpen(true);
                      }}
                    />
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <Pagination metadata={pagination} onPageChange={setPage} isLoading={isLoading} />
      </div>

      {/* Create / Edit Customer Modal */}
      <Modal
        open={createModalOpen || editCustomer !== null}
        onOpenChange={(open) => {
          if (!open) {
            setCreateModalOpen(false);
            setEditCustomer(null);
            resetForm();
          }
        }}
        title={editCustomer ? 'Edit Customer' : 'Add New Customer'}
        description={
          editCustomer
            ? 'Update customer contact information and transactional parameters.'
            : 'Enter individual customer details to create a new profile in the database.'
        }
        maxWidth="lg"
      >
        {formError && (
          <div className="mb-4 p-3 rounded-lg bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="First Name"
              required
              value={formData.firstName}
              onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
            />
            <Input
              label="Last Name"
              required
              value={formData.lastName}
              onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Email Address"
              type="email"
              required
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            />
            <Input
              label="Phone Number"
              value={formData.phone || ''}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="City"
              value={formData.city || ''}
              onChange={(e) => setFormData({ ...formData, city: e.target.value })}
            />
            <Input
              label="Country"
              value={formData.country || ''}
              onChange={(e) => setFormData({ ...formData, country: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Total Spend ($)"
              type="number"
              step="0.01"
              min="0"
              value={formData.totalSpend ?? 0}
              onChange={(e) =>
                setFormData({ ...formData, totalSpend: parseFloat(e.target.value) || 0 })
              }
            />
            <Input
              label="Visit Count"
              type="number"
              min="0"
              value={formData.visitCount ?? 0}
              onChange={(e) =>
                setFormData({ ...formData, visitCount: parseInt(e.target.value) || 0 })
              }
            />
          </div>

          {/* Tags manager */}
          <div className="text-left space-y-1.5">
            <label className="text-xs font-medium text-app-secondary">Tags</label>
            <div className="flex gap-2">
              <Input
                placeholder="Add tag and click +"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddTag();
                  }
                }}
              />
              <Button type="button" variant="secondary" onClick={handleAddTag}>
                <Plus className="w-4 h-4" />
              </Button>
            </div>
            {formData.tags && formData.tags.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mt-2">
                {formData.tags.map((tag) => (
                  <span
                    key={tag}
                    className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-md bg-app-surface-subtle border border-app text-app-primary"
                  >
                    <Tag className="w-3 h-3 text-app-muted" />
                    <span>{tag}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveTag(tag)}
                      className="ml-1 text-app-muted hover:text-red-500 font-bold"
                    >
                      &times;
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div className="pt-4 border-t border-app flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setCreateModalOpen(false);
                setEditCustomer(null);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              isLoading={createMutation.isPending || editMutation.isPending}
            >
              {editCustomer ? 'Save Changes' : 'Create Customer'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={deleteConfirmId !== null}
        onOpenChange={(open) => !open && setDeleteConfirmId(null)}
        title="Confirm Customer Deletion"
        description="Are you sure you want to delete this customer record? This action cannot be undone."
        maxWidth="sm"
      >
        <div className="flex justify-end gap-3 pt-4 border-t border-app">
          <Button variant="outline" onClick={() => setDeleteConfirmId(null)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            isLoading={deleteMutation.isPending}
            onClick={() => {
              if (deleteConfirmId) {
                deleteMutation.mutate(deleteConfirmId);
              }
            }}
          >
            Delete Customer
          </Button>
        </div>
      </Modal>
    </div>
  );
};
