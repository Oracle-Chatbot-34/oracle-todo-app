// Configuración centralizada de la aplicación
export const config = {
  // En producción usar rutas relativas (nginx hará el proxy)
  // En desarrollo usar URL completa del backend
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || (
    import.meta.env.DEV ? 'http://localhost:8080' : ''
  ),
  authEndpoint: import.meta.env.VITE_AUTH_ENDPOINT || '/auth',
  apiEndpoint: import.meta.env.VITE_API_ENDPOINT || '/api',
  
  // Configuración adicional para robustez
  timeout: 30000, // 30 segundos - importante para conexiones lentas
  retryAttempts: 3,
  retryDelay: 1000, // 1 segundo entre reintentos
};

// Función helper para construir URLs completas - esto hace el código más mantenible
export const buildApiUrl = (endpoint: string): string => {
  const baseUrl = config.apiBaseUrl;
  
  // Si estamos en desarrollo y no hay baseUrl configurado, usar localhost
  if (import.meta.env.DEV && !baseUrl) {
    return `http://localhost:8080${endpoint}`;
  }
  
  // En producción o con baseUrl configurado explícitamente
  return `${baseUrl}${endpoint}`;
};

// Función para debug de configuración - solo activa en desarrollo
export const logConfig = (): void => {
  if (import.meta.env.DEV) {
    console.log('🔧 App Configuration:', {
      mode: import.meta.env.MODE,
      dev: import.meta.env.DEV,
      prod: import.meta.env.PROD,
      apiBaseUrl: config.apiBaseUrl,
      authEndpoint: config.authEndpoint,
      apiEndpoint: config.apiEndpoint,
      fullAuthUrl: buildApiUrl(config.authEndpoint),
      fullApiUrl: buildApiUrl(config.apiEndpoint),
    });
  }
};