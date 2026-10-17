import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiClient } from '@/lib/api-client';

describe('ApiClient', () => {
  beforeEach(() => {
    apiClient.setToken(null);
    vi.restoreAllMocks();
  });

  it('manages in-memory token correctly', () => {
    expect(apiClient.getToken()).toBeNull();
    apiClient.setToken('sample-jwt-token-123');
    expect(apiClient.getToken()).toBe('sample-jwt-token-123');
    apiClient.setToken(null);
    expect(apiClient.getToken()).toBeNull();
  });

  it('injects authorization header when token is present', async () => {
    apiClient.setToken('auth-token-xyz');

    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ success: true, data: { status: 'UP' } }),
    });
    globalThis.fetch = mockFetch;

    await apiClient.get('actuator/health');

    expect(mockFetch).toHaveBeenCalledTimes(1);
    const headers = mockFetch.mock.calls[0][1].headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer auth-token-xyz');
    expect(headers.get('Accept')).toBe('application/json');
  });

  it('triggers onUnauthorized callback on 401 response', async () => {
    const unauthorizedSpy = vi.fn();
    apiClient.onUnauthorized(unauthorizedSpy);

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({
        success: false,
        error: { code: 'UNAUTHORIZED', message: 'Token expired' },
      }),
    });

    await expect(apiClient.get('customers')).rejects.toThrow('Token expired');
    expect(unauthorizedSpy).toHaveBeenCalledTimes(1);
  });
});
