/**
 * Shared Axios instance for all API calls.
 * - baseURL from VITE_API_URL env variable (default: http://localhost:8081)
 * - withCredentials: true  → browser automatically sends HttpOnly cookies (JWT + refreshToken)
 * - 401 interceptor        → calls /api/auth/refresh and replays the original request once
 *   If the refresh also fails, the user is redirected to /login.
 */
import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { handleAuthFailure } from './auth-failure';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';

const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  withCredentials: true, // required for HttpOnly cookie transport (CORS + credentials)
  headers: {
    'Content-Type': 'application/json',
  },
});

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  skipAuthRefresh?: boolean;
}

// ── 401 interceptor – transparent token rotation ───────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as CustomAxiosRequestConfig;
    const requestUrl = originalRequest.url ?? '';
    const isAuthRequest =
      requestUrl.includes('/api/auth/login') ||
      requestUrl.includes('/api/auth/logout') ||
      requestUrl.includes('/api/auth/me');

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.skipAuthRefresh &&
      !isAuthRequest
    ) {
      originalRequest._retry = true;
      try {
        // Ask the backend to rotate the refresh token and emit new JWT cookie
        await axios.post(`${API_URL}/api/auth/refresh`, {}, { withCredentials: true });
        // Replay the original request – the new JWT cookie is now set by the browser
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh also failed → use the registered auth failure handler.
        try {
          await handleAuthFailure();
        } catch (handlerError) {
          console.error('Auth failure handler failed:', handlerError);
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
