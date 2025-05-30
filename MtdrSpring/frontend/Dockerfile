# Usar una versión específica y estable de Node.js que está ampliamente disponible
# La versión 18.19 es LTS y tiene excelente soporte en todos los registries
FROM node:18.19-alpine AS build

WORKDIR /app

# Configuración de npm para optimizar el proceso de build
# Estas configuraciones reducen el ruido en los logs y mejoran el rendimiento
RUN npm config set fund false
RUN npm config set audit-level none

# Estrategia de caching: copiar package.json primero para aprovechar Docker layer caching
# Si las dependencias no cambian, Docker reutilizará esta layer en builds subsecuentes
COPY package.json package-lock.json* ./

# Limpiar cache y instalar dependencias con configuraciones robustas
# --legacy-peer-deps resuelve conflictos de versiones en dependencias React
# --no-audit y --no-fund aceleran la instalación omitiendo checks no esenciales
RUN npm cache clean --force
RUN npm install --legacy-peer-deps --no-audit --no-fund

# Copiar el resto del código fuente después de instalar dependencias
# Esta separación significa que cambios en el código no invalidarán la cache de node_modules
COPY . .

# Variables de entorno para el build de producción
# Estas variables configuran cómo Vite construye la aplicación para producción
ENV NODE_ENV=production
ENV VITE_API_BASE_URL=""
ENV VITE_AUTH_ENDPOINT="/auth"
ENV VITE_API_ENDPOINT="/api"

# Construir la aplicación usando Vite - esto genera archivos estáticos optimizados
RUN npm run build

# Verificar que el build produjo los archivos esperados
RUN ls -la dist/ && echo "✅ Build completed successfully"

# Etapa de producción: usar nginx Alpine que es lightweight y seguro
# nginx:1.25-alpine es una versión estable y ampliamente disponible
FROM nginx:1.25-alpine

# Instalar curl para health checks - necesario para probes de Kubernetes
RUN apk add --no-cache curl

# Remover configuración default de nginx que puede conflictar
RUN rm /etc/nginx/conf.d/default.conf

# Copiar archivos construidos desde la etapa de build
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar nuestra configuración personalizada de nginx que maneja el proxy al backend
COPY nginx.conf /etc/nginx/nginx.conf

# Crear directorio para logs si no existe
RUN mkdir -p /var/log/nginx

# Crear script de entrada personalizado para validación y debugging
# Este script nos ayuda a diagnosticar problemas cuando el contenedor inicia
RUN echo '#!/bin/sh' > /docker-entrypoint-custom.sh && \
    echo 'echo "🚀 Starting DashMaster Frontend"' >> /docker-entrypoint-custom.sh && \
    echo 'echo "📁 Content directory:"' >> /docker-entrypoint-custom.sh && \
    echo 'ls -la /usr/share/nginx/html' >> /docker-entrypoint-custom.sh && \
    echo 'echo "🔧 Testing nginx configuration:"' >> /docker-entrypoint-custom.sh && \
    echo 'nginx -t' >> /docker-entrypoint-custom.sh && \
    echo 'echo "✅ Nginx configuration is valid"' >> /docker-entrypoint-custom.sh && \
    echo 'exec "$@"' >> /docker-entrypoint-custom.sh && \
    chmod +x /docker-entrypoint-custom.sh

# Exponer puerto HTTP estándar
EXPOSE 80

# Health check optimizado para la aplicación React con nginx
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1

# Usar nuestro script personalizado como punto de entrada
ENTRYPOINT ["/docker-entrypoint-custom.sh"]
CMD ["nginx", "-g", "daemon off;"]