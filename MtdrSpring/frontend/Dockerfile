# Multi-stage build usando Oracle Container Registry para evitar rate limits
# Oracle Container Registry Mirror tiene las mismas imágenes sin límites de rate
FROM container-registry.oracle.com/os/node:18-alpine AS build

WORKDIR /app

# Configurar npm para mayor eficiencia y evitar warnings innecesarios
RUN npm config set fund false
RUN npm config set audit-level none

# Copiar archivos de dependencias primero - esto aprovecha el cache de Docker layers
# Si package.json no cambia, esta layer se reutiliza en builds subsecuentes
COPY package.json package-lock.json* ./

# Limpiar cache de npm e instalar dependencias
# --legacy-peer-deps resuelve conflictos de dependencias
# --no-audit y --no-fund aceleran la instalación
RUN npm cache clean --force
RUN npm install --legacy-peer-deps --no-audit --no-fund

# Copiar el resto del código fuente después de instalar dependencias
# Esto significa que cambios en el código no invalidan la cache de node_modules
COPY . .

# Variables de entorno específicas para el build de producción
# Estas variables le dicen a Vite cómo construir la aplicación
ENV NODE_ENV=production
ENV VITE_API_BASE_URL=""
ENV VITE_AUTH_ENDPOINT="/auth"
ENV VITE_API_ENDPOINT="/api"

# Construir la aplicación usando Vite
RUN npm run build

# Verificar que el build fue exitoso antes de continuar
RUN ls -la dist/ && echo "✅ Build completed successfully"

# Segunda etapa: imagen de producción optimizada
# Usamos Oracle Container Registry también para nginx
FROM container-registry.oracle.com/os/nginx:alpine

# Instalar curl para health checks - necesario para Kubernetes probes
RUN apk add --no-cache curl

# Remover configuración default de nginx que puede conflictar con la nuestra
RUN rm /etc/nginx/conf.d/default.conf

# Copiar archivos construidos desde la etapa de build
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar nuestra configuración personalizada de nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Crear directorio para logs si no existe
RUN mkdir -p /var/log/nginx

# Crear script de entrada personalizado para validación y debugging
# Esto nos ayuda a diagnosticar problemas cuando el contenedor inicia
RUN echo '#!/bin/sh' > /docker-entrypoint-custom.sh && \
    echo 'echo "🚀 Starting DashMaster Frontend"' >> /docker-entrypoint-custom.sh && \
    echo 'echo "📁 Content directory:"' >> /docker-entrypoint-custom.sh && \
    echo 'ls -la /usr/share/nginx/html' >> /docker-entrypoint-custom.sh && \
    echo 'echo "🔧 Testing nginx configuration:"' >> /docker-entrypoint-custom.sh && \
    echo 'nginx -t' >> /docker-entrypoint-custom.sh && \
    echo 'echo "✅ Nginx configuration is valid"' >> /docker-entrypoint-custom.sh && \
    echo 'exec "$@"' >> /docker-entrypoint-custom.sh && \
    chmod +x /docker-entrypoint-custom.sh

# Exponer puerto 80 para HTTP
EXPOSE 80

# Health check mejorado para Kubernetes
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1

# Usar nuestro script personalizado como entrada
ENTRYPOINT ["/docker-entrypoint-custom.sh"]
CMD ["nginx", "-g", "daemon off;"]