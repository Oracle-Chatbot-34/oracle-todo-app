# Usar imagen menos popular pero oficial para evitar rate limits
FROM node:18.19-alpine3.18 AS build

WORKDIR /app

# Configurar npm
RUN npm config set fund false
RUN npm config set audit-level none

# Copiar y instalar dependencias
COPY package.json package-lock.json* ./
RUN npm cache clean --force
RUN npm install --legacy-peer-deps --no-audit --no-fund

# Copiar código y construir
COPY . .

ENV NODE_ENV=production
ENV VITE_API_BASE_URL=""
ENV VITE_AUTH_ENDPOINT="/auth"
ENV VITE_API_ENDPOINT="/api"

RUN npm run build
RUN ls -la dist/ && echo "✅ Build completed successfully"

# Imagen de producción - usar versión específica para evitar rate limits
FROM nginx:1.25-alpine

# Instalar curl
RUN apk add --no-cache curl

# Configurar nginx
RUN rm /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

RUN mkdir -p /var/log/nginx

# Script de entrada
RUN echo '#!/bin/sh' > /docker-entrypoint-custom.sh && \
    echo 'echo "🚀 Starting DashMaster Frontend"' >> /docker-entrypoint-custom.sh && \
    echo 'echo "📁 Content directory:"' >> /docker-entrypoint-custom.sh && \
    echo 'ls -la /usr/share/nginx/html' >> /docker-entrypoint-custom.sh && \
    echo 'echo "🔧 Testing nginx configuration:"' >> /docker-entrypoint-custom.sh && \
    echo 'nginx -t' >> /docker-entrypoint-custom.sh && \
    echo 'echo "✅ Nginx configuration is valid"' >> /docker-entrypoint-custom.sh && \
    echo 'exec "$@"' >> /docker-entrypoint-custom.sh && \
    chmod +x /docker-entrypoint-custom.sh

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1

ENTRYPOINT ["/docker-entrypoint-custom.sh"]
CMD ["nginx", "-g", "daemon off;"]