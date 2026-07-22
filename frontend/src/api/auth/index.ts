/**
 * AUTH API – login, logout, me
 * Consumes: POST /api/auth/login, POST /api/auth/logout, GET /api/auth/me
 *
 * Note: le backend Spring Security attend le champ JSON "username",
 * mais la valeur transmise est l'adresse e-mail du superviseur.
 */
import apiClient from '../../lib/axios';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface ChangePasswordCredentials {
  oldPassword: string;
  newPassword: string;
}

export interface AuthUser {
  token?: string;
  email: string;
  role: 'ROLE_ADMIN' | 'ROLE_SUPERVISEUR';
  mustChangePassword: boolean;
}

/** Réponse brute de l'API Java (username = email côté serveur). */
interface AuthUserResponse {
  token?: string | null;
  username: string;
  role: 'ROLE_ADMIN' | 'ROLE_SUPERVISEUR';
  mustChangePassword: boolean;
}

function mapAuthUser(data: AuthUserResponse): AuthUser {
  return {
    token: data.token ?? undefined,
    email: data.username,
    role: data.role,
    mustChangePassword: data.mustChangePassword,
  };
}

export const login = async (credentials: LoginCredentials): Promise<AuthUser> => {
  const response = await apiClient.post<AuthUserResponse>('/api/auth/login', {
    username: credentials.email,
    password: credentials.password,
  });
  return mapAuthUser(response.data);
};

export const logout = async (): Promise<void> => {
  await apiClient.post('/api/auth/logout');
};

export const getCurrentUser = async (): Promise<AuthUser> => {
  const response = await apiClient.get<AuthUserResponse>('/api/auth/me');
  return mapAuthUser(response.data);
};

export const changePassword = async (credentials: ChangePasswordCredentials): Promise<void> => {
  await apiClient.post('/api/auth/change-password', credentials);
};
