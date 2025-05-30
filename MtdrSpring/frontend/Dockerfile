# Multi-stage build para optimizar tamaño final
FROM node:18-alpine AS build

WORKDIR /app

# Configurar npm para mayor eficiencia
RUN npm config set fund false
RUN npm config set audit-level none

# Copiar archivos de dependencias primero (mejor caching)
COPY package.json package-lock.json* ./

# Limpiar cache e instalar dependencias
RUN npm cache clean --force
RUN npm install --legacy-peer-deps --no-audit --no-fund

# Copiar código fuente
COPY . .

# Variables de entorno para build de producción
ENV NODE_ENV=production
ENV VITE_API_BASE_URL=""
ENV VITE_AUTH_ENDPOINT="/auth"
ENV VITE_API_ENDPOINT="/api"

# Construir aplicación
RUN npm run build

# Verificar que el build fue exitoso
RUN ls -la dist/ && echo "✅ Build completed successfully"

# Etapa de producción con nginx optimizado
FROM nginx:alpine

# Instalar curl para health checks
RUN apk add --no-cache curl

# Remover configuración default de nginx que puede conflictuar
RUN rm /etc/nginx/conf.d/default.conf

# Copiar archivos construidos
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar nuestra configuración personalizada de nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Crear directorio para logs
RUN mkdir -p /var/log/nginx

# Script de entrada personalizado para validación
RUN echo '#!/bin/sh' > /docker-entrypoint-custom.sh && \
    echo 'echo "🚀 Starting DashMaster Frontend"' >> /docker-entrypoint-custom.sh && \
    echo 'echo "📁 Content directory:"' >> /docker-entrypoint-custom.sh && \
    echo 'ls -la /usr/share/nginx/html' >> /docker-entrypoint-custom.sh && \
    echo 'echo "🔧 Testing nginx configuration:"' >> /docker-entrypoint-custom.sh && \
    echo 'nginx -t' >> /docker-entrypoint-custom.sh && \
    echo 'echo "✅ Nginx configuration is valid"' >> /docker-entrypoint-custom.sh && \
    echo 'exec "$@"' >> /docker-entrypoint-custom.sh && \
    chmod +x /docker-entrypoint-custom.sh

# Exponer puerto
EXPOSE 80

# Health check mejorado
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1

# Usar script personalizado
ENTRYPOINT ["/docker-entrypoint-custom.sh"]
CMD ["nginx", "-g", "daemon off;"]