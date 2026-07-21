import React, { createContext, useState, useEffect, useContext } from 'react';
import * as authApi from '../api/auth';

export interface User {
  username: string;
  role: string;
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<any>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const data = await authApi.getCurrentUser();
        if (data && data.username) {
          setUser({ username: data.username, role: data.role });
        } else {
          setUser(null);
        }
      } catch (error) {
        console.error("Failed to load user info:", error);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, []);

  const login = async (username: string, password: string) => {
    setLoading(true);
    try {
      const data = await authApi.login(username, password);
      if (data && data.username) {
        setUser({ username: data.username, role: data.role });
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
    }
  };

  const value: AuthContextType = {
    user,
    token: null, // JWT token is stored secure in HttpOnly cookie and managed by browser
    loading,
    login,
    logout,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN',
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
