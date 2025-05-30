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
# Estas configuraciones reducen warnings y mejoran velocidad
RUN npm config set fund false && npm config set audit-level none

# Copiar archivos de dependencias primero para aprovechar Docker layer caching
# Si package.json no cambia, esta layer se reutiliza en builds subsecuentes
COPY package.json package-lock.json* ./

# Instalar dependencias con configuraciones robustas para CI/CD
RUN npm cache clean --force && npm install --legacy-peer-deps --no-audit --no-fund

# Copiar el resto del código fuente después de instalar dependencias
# Esta separación optimiza el tiempo de build cuando solo cambia código
COPY . .

# Variables de entorno para build de producción con Vite
# Estas variables configuran cómo Vite optimiza la aplicación
ENV NODE_ENV=production
ENV VITE_API_BASE_URL=""
ENV VITE_AUTH_ENDPOINT="/auth"
ENV VITE_API_ENDPOINT="/api"

# Construir la aplicación React usando Vite
# Esto genera archivos estáticos optimizados para producción
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
# Estos directorios son requeridos para que nginx funcione correctamente
RUN mkdir -p /var/log/nginx /var/cache/nginx /run/nginx

# Remover configuración default que puede conflictar con la nuestra
RUN rm -f /etc/nginx/conf.d/default.conf

# Copiar archivos de la aplicación construida desde la etapa de build
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar nuestra configuración personalizada de nginx que maneja proxy al backend
COPY nginx.conf /etc/nginx/nginx.conf

# Verificar que la configuración de nginx es válida antes de continuar
# Esto previene errores en runtime debido a configuración incorrecta
RUN nginx -t

# Exponer puerto HTTP estándar
EXPOSE 80

# Health check optimizado para aplicaciones React servidas por nginx
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1

# Ejecutar nginx en modo foreground (requerido para contenedores)
CMD ["nginx", "-g", "daemon off;"]