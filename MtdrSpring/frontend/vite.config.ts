import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  
  // Configuración del servidor de desarrollo
  server: {
    host: '0.0.0.0', // Permitir conexiones externas para Docker
    port: 5173,
    proxy: {
      // Proxy solo activo en desarrollo - estos no afectan el build de producción
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => {
          console.log(`🔄 Proxying auth request: ${path} -> http://localhost:8080${path}`);
          return path;
        }
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => {
          console.log(`🔄 Proxying API request: ${path} -> http://localhost:8080${path}`);
          return path;
        }
      }
    },
  },
  
  // Configuración de build optimizada para producción
  build: {
    outDir: 'dist',
    sourcemap: false, // Deshabilitar sourcemaps en producción para reducir tamaño
    rollupOptions: {
      output: {
        // Separar chunks para mejor caching del navegador
        manualChunks: {
          vendor: ['react', 'react-dom'],
          router: ['react-router-dom'],
          ui: ['@radix-ui/react-select', '@radix-ui/react-popover', '@radix-ui/react-checkbox'],
          charts: ['recharts', 'react-apexcharts'],
        }
      }
    },
    chunkSizeWarningLimit: 1000, // Aumentar límite de advertencia
  },
  
  // Definir variables que estarán disponibles en el código de la aplicación
  // Usar process.env durante la configuración, import.meta.env en runtime
  define: {
    __APP_VERSION__: JSON.stringify(process.env.npm_package_version || '1.0.0'),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
  },
  
  // Variables de entorno - configuración segura para builds
  envPrefix: 'VITE_', // Solo variables que empiecen con VITE_ estarán disponibles
  
  // Configuración para preview (simulación de producción local)
  preview: {
    host: '0.0.0.0',
    port: 4173,
  }
});