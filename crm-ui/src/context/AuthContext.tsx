import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiClient } from '@/lib/api-client';
import type { AuthTokenResponse, AuthUser } from '@/types';

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isMarketer: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    // Register auto-logout handler when API client receives a 401
    apiClient.onUnauthorized(() => {
      apiClient.setToken(null);
      setUser(null);
    });
  }, []);

  const login = async (username: string, password: string) => {
    const response = await apiClient.post<AuthTokenResponse>('/api/v1/auth/login', {
      username: username.trim(),
      password,
    });

    if (response.success && response.data?.token) {
      apiClient.setToken(response.data.token);
      setUser(response.data.user);
    } else {
      throw new Error('Invalid authentication response from server');
    }
  };

  const logout = () => {
    apiClient.setToken(null);
    setUser(null);
  };

  const isAuthenticated = !!user && !!apiClient.getToken();
  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isMarketer = user?.role === 'ROLE_MARKETER';

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isAdmin,
        isMarketer,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
