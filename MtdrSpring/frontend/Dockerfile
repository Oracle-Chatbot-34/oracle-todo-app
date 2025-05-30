# Etapa de build usando Oracle Linux con Node.js instalado desde fuente oficial
FROM container-registry.oracle.com/os/oraclelinux:8-slim AS build
WORKDIR /app

# Instalar herramientas básicas necesarias para descargar e instalar Node.js
RUN microdnf update -y && \
    microdnf install -y curl wget tar xz && \
    microdnf clean all

# Instalar Node.js desde NodeSource (más confiable que imágenes de registry)
# Usamos una versión LTS específica para estabilidad
RUN curl -fsSL https://nodejs.org/dist/v18.19.0/node-v18.19.0-linux-x64.tar.xz | tar -xJ -C /opt/ && \
    ln -s /opt/node-v18.19.0-linux-x64/bin/node /usr/local/bin/node && \
    ln -s /opt/node-v18.19.0-linux-x64/bin/npm /usr/local/bin/npm

# Verificar que Node.js y npm se instalaron correctamente
RUN node --version && npm --version

# Configurar npm para optimizar el proceso de instalación
RUN npm config set fund false && npm config set audit-level none

# Copiar archivos de dependencias primero para aprovechar Docker layer caching
COPY package.json package-lock.json* ./

# Instalar dependencias con configuraciones robustas para CI/CD
RUN npm cache clean --force && npm install --legacy-peer-deps --no-audit --no-fund

# Copiar el resto del código fuente después de instalar dependencias
COPY . .

# CONFIGURACIÓN CRÍTICA: Variables de entorno para Vite
# Estas variables deben estar disponibles ANTES de ejecutar el build
# Configuramos tanto NODE_ENV como variables específicas de Vite

# Variable de entorno del sistema que Node.js puede leer
ENV NODE_ENV=production

# Variables que Vite usará durante el build - con valores explícitos
# El string vacío para VITE_API_BASE_URL indica que usaremos rutas relativas
ENV VITE_API_BASE_URL=""
ENV VITE_AUTH_ENDPOINT="/auth"
ENV VITE_API_ENDPOINT="/api"
ENV VITE_TIMEOUT="30000"
ENV VITE_RETRY_ATTEMPTS="3"
ENV VITE_RETRY_DELAY="1000"

# Variable adicional para indicar que estamos en modo build
ENV MODE=production

# PASO CRÍTICO: Verificar que las variables están disponibles antes del build
# Esto nos ayuda a diagnosticar problemas de variables de entorno
RUN echo "=== Verificando variables de entorno ===" && \
    echo "NODE_ENV: $NODE_ENV" && \
    echo "MODE: $MODE" && \
    echo "VITE_API_BASE_URL: $VITE_API_BASE_URL" && \
    echo "VITE_AUTH_ENDPOINT: $VITE_AUTH_ENDPOINT" && \
    echo "VITE_API_ENDPOINT: $VITE_API_ENDPOINT"

# Construir la aplicación React usando Vite
# En este punto, Vite debería poder acceder a todas las variables de entorno
RUN npm run build

# Verificar que el build produjo los archivos esperados
RUN ls -la dist/ && echo "✅ Frontend build completado exitosamente"

# Etapa de producción usando Oracle Linux con nginx
FROM container-registry.oracle.com/os/oraclelinux:8-slim

# Instalar nginx y curl desde repositorios oficiales de Oracle Linux
RUN microdnf update -y && \
    microdnf install -y nginx curl && \
    microdnf clean all

# Verificar que nginx se instaló correctamente
RUN nginx -v

# Configurar directorios necesarios para nginx en contenedor
RUN mkdir -p /var/log/nginx /var/cache/nginx /run/nginx

# Remover configuración default que puede conflictar con la nuestra
RUN rm -f /etc/nginx/conf.d/default.conf

# Copiar archivos de la aplicación construida desde la etapa de build
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar nuestra configuración personalizada de nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Verificar que la configuración de nginx es válida antes de continuar
RUN nginx -t

# Exponer puerto HTTP estándar
EXPOSE 80

# Health check optimizado para aplicaciones React servidas por nginx
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1

# Ejecutar nginx en modo foreground (requerido para contenedores)
CMD ["nginx", "-g", "daemon off;"]