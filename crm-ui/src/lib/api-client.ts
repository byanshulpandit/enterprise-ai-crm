import type { ApiResponse } from '@/types';

class ApiClient {
  private inMemoryToken: string | null = null;
  private onUnauthorizedCallback: (() => void) | null = null;

  setToken(token: string | null) {
    this.inMemoryToken = token;
  }

  getToken(): string | null {
    return this.inMemoryToken;
  }

  onUnauthorized(callback: () => void) {
    this.onUnauthorizedCallback = callback;
  }

  async request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
    const url = endpoint.startsWith('/') ? endpoint : `/api/v1/${endpoint}`;
    
    const headers = new Headers(options.headers);
    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }

    if (this.inMemoryToken && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${this.inMemoryToken}`);
    }

    // Do not set Content-Type if body is FormData (browser will set boundary automatically)
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      if (this.onUnauthorizedCallback) {
        this.onUnauthorizedCallback();
      }
    }

    if (response.status === 204) {
      return { success: true, data: undefined as unknown as T };
    }

    const json = await response.json().catch(() => null);

    if (!response.ok) {
      const errorMsg = json?.error?.message || json?.message || `Request failed with status ${response.status}`;
      const error = new Error(errorMsg) as Error & { status?: number; code?: string };
      error.status = response.status;
      error.code = json?.error?.code;
      throw error;
    }

    return json as ApiResponse<T>;
  }

  get<T>(endpoint: string, headers?: HeadersInit) {
    return this.request<T>(endpoint, { method: 'GET', headers });
  }

  post<T>(endpoint: string, body?: unknown, headers?: HeadersInit) {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: body instanceof FormData ? body : JSON.stringify(body),
      headers,
    });
  }

  put<T>(endpoint: string, body?: unknown, headers?: HeadersInit) {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
      headers,
    });
  }

  patch<T>(endpoint: string, body?: unknown, headers?: HeadersInit) {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(body),
      headers,
    });
  }

  delete<T>(endpoint: string, headers?: HeadersInit) {
    return this.request<T>(endpoint, { method: 'DELETE', headers });
  }
}

export const apiClient = new ApiClient();
