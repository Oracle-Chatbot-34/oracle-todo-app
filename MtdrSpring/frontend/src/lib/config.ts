// Configuración centralizada y robusta para la aplicación
// Esta configuración funciona tanto en desarrollo como en producción

// Función helper para obtener variables de entorno de manera segura
const getEnvVar = (key: string, defaultValue: string = ''): string => {
  // En tiempo de build, las variables pueden estar en diferentes lugares
  // Primero intentamos import.meta.env, luego fallback a valores por defecto
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    return import.meta.env[key] || defaultValue;
  }
  return defaultValue;
};

// Detectar el entorno de ejecución
const isDevelopment = getEnvVar('MODE') === 'development' || 
                     getEnvVar('NODE_ENV') === 'development' ||
                     (typeof window !== 'undefined' && window.location.hostname === 'localhost');

const isProduction = getEnvVar('NODE_ENV') === 'production' || 
                    getEnvVar('MODE') === 'production';

export const config = {
  // Configuración de URLs base inteligente
  apiBaseUrl: getEnvVar('VITE_API_BASE_URL') || (
    isDevelopment ? 'http://localhost:8080' : ''
  ),
  
  // Endpoints de la API
  authEndpoint: getEnvVar('VITE_AUTH_ENDPOINT') || '/auth',
  apiEndpoint: getEnvVar('VITE_API_ENDPOINT') || '/api',
  
  // Configuración de red y timeouts
  timeout: parseInt(getEnvVar('VITE_TIMEOUT', '30000')), // 30 segundos por defecto
  retryAttempts: parseInt(getEnvVar('VITE_RETRY_ATTEMPTS', '3')),
  retryDelay: parseInt(getEnvVar('VITE_RETRY_DELAY', '1000')), // 1 segundo
  
  // Configuración de entorno
  environment: isProduction ? 'production' : (isDevelopment ? 'development' : 'unknown'),
  isDevelopment,
  isProduction,
};

// Función helper para construir URLs completas - robusta y confiable
export const buildApiUrl = (endpoint: string): string => {
  const baseUrl = config.apiBaseUrl;
  
  // En desarrollo, usar localhost si no hay baseUrl configurado
  if (config.isDevelopment && !baseUrl) {
    return `http://localhost:8080${endpoint}`;
  }
  
  // En producción, las rutas son relativas (nginx maneja el proxy)
  if (config.isProduction && !baseUrl) {
    return endpoint;
  }
  
  // Si hay baseUrl configurado explícitamente, usarlo
  return `${baseUrl}${endpoint}`;
};

// Función para logging de configuración - solo activa en desarrollo
export const logConfig = (): void => {
  if (config.isDevelopment) {
    console.log('🔧 App Configuration:', {
      environment: config.environment,
      isDevelopment: config.isDevelopment,
      isProduction: config.isProduction,
      apiBaseUrl: config.apiBaseUrl,
      authEndpoint: config.authEndpoint,
      apiEndpoint: config.apiEndpoint,
      fullAuthUrl: buildApiUrl(config.authEndpoint),
      fullApiUrl: buildApiUrl(config.apiEndpoint),
      timeout: config.timeout,
    });
  }
};

// Función para validar configuración - útil para debugging
export const validateConfig = (): boolean => {
  const requiredConfigs = [
    config.authEndpoint,
    config.apiEndpoint,
  ];
  
  const isValid = requiredConfigs.every(configItem => configItem && configItem.length > 0);
  
  if (!isValid && config.isDevelopment) {
    console.warn('⚠️ Configuración incompleta detectada');
  }
  
  return isValid;
};