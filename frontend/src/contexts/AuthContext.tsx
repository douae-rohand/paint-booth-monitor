import React, { createContext, useState, useEffect, useContext } from 'react';
import { useNavigate } from '@tanstack/react-router';
import * as authApi from '../api/auth/index';
import type { AuthUser } from '../api/auth/index';
import { resolveAuthState, invalidateAuthCache } from '../lib/auth-check';
import { registerAuthFailureHandler } from '../lib/auth-failure';

export type { AuthUser as User };

export interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<AuthUser>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  isAdmin: boolean;
  mustChangePassword: boolean;
  updateMustChangePassword: (value: boolean) => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const { user } = await resolveAuthState();
        setUser(user);
      } catch (error) {
        console.error('Failed to resolve auth state:', error);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, []);

  const navigate = useNavigate();

  const login = async (email: string, password: string): Promise<AuthUser> => {
    setLoading(true);
    try {
      const data = await authApi.login({ email, password });
      if (data?.email) {
        setUser(data);
        // Invalidate shared cache so subsequent route guards re-resolve auth state
        invalidateAuthCache();
      }
      return data;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error("Failed to call logout API:", error);
    } finally {
      setUser(null);
      invalidateAuthCache();
    }
  };

  useEffect(() => {
    registerAuthFailureHandler(async () => {
      await logout();
      navigate({ to: '/login' });
    });

    return () => {
      registerAuthFailureHandler(async () => {
        // No-op if auth provider is unmounted
      });
    };
  }, [logout, navigate]);

  const updateMustChangePassword = (value: boolean) => {
    setUser(prev => prev ? { ...prev, mustChangePassword: value } : null);
  };


  const value: AuthContextType = {
    user,
    token: null, // JWT token is in HttpOnly cookie, managed by browser
    loading,
    login,
    logout,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ROLE_ADMIN',
    mustChangePassword: user?.mustChangePassword ?? false,
    updateMustChangePassword,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
