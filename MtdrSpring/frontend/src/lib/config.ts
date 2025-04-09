export const config = {
    apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '',
    authEndpoint: import.meta.env.VITE_AUTH_ENDPOINT || '/auth',
    apiEndpoint: import.meta.env.VITE_API_ENDPOINT || '/api',
  };