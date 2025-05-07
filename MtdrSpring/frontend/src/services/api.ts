import axios, { AxiosRequestConfig, AxiosResponse, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { config } from '../lib/config';

// Create axios instance with base URL
const api = axios.create({
  baseURL: config.apiBaseUrl,
  withCredentials: true,
  timeout: 30000, // 30 second timeout
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
});

// Create a request cache for GET requests
const requestCache = new Map<string, { data: unknown, timestamp: number }>();
const CACHE_DURATION = 60000; // 1 minute cache

// Request interceptor to add auth token and handle caching
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`);
    }
    
    // Check cache for GET requests
    if (config.method?.toLowerCase() === 'get' && config.url) {
      const cacheKey = `${config.url}${JSON.stringify(config.params || {})}`;
      const cachedResponse = requestCache.get(cacheKey);
      
      if (cachedResponse && (Date.now() - cachedResponse.timestamp < CACHE_DURATION)) {
        // Convert to a rejected promise that axios will catch
        // @ts-expect-error - we're using a trick to return cached data through the error mechanism
        return Promise.reject({
          __cached: true,
          config,
          data: cachedResponse.data
        });
      }
    }
    
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors and caching
api.interceptors.response.use(
  (response: AxiosResponse) => {
    // Cache successful GET responses
    if (response.config.method?.toLowerCase() === 'get' && response.config.url) {
      const cacheKey = `${response.config.url}${JSON.stringify(response.config.params || {})}`;
      requestCache.set(cacheKey, {
        data: response.data,
        timestamp: Date.now()
      });
    }
    
    return response;
  },
  (error: AxiosError | { __cached: boolean; config: AxiosRequestConfig; data: unknown }) => {
    // Return cached response if available
    if ('__cached' in error && error.__cached) {
      return Promise.resolve({ ...error, data: error.data });
    }
    
    // Handle authentication errors
    if ('__cached' in error) {
      // Skip auth checking for cached responses
    } else if ((error as AxiosError).response?.status === 401) {
      // Clear auth data and redirect to login
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      localStorage.removeItem('fullName');
      
      // Only redirect if we're not already on the login page
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }
    
    // Log other errors
    console.error('API Error:', axios.isAxiosError(error) ? error.response?.data : ('message' in error ? error.message : 'Unknown error'));
    
    return Promise.reject(error);
  }
);

// Method to clear cache
export const clearApiCache = () => {
  requestCache.clear();
};

// Method to clear a specific cache entry
export const clearApiCacheFor = (url: string, params?: Record<string, unknown>) => {
  const cacheKey = `${url}${JSON.stringify(params || {})}`;
  requestCache.delete(cacheKey);
};

export default api;