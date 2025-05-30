# Build simple y rápido
FROM node:18-alpine AS build
WORKDIR /app

# Instalar dependencias mínimas
COPY package.json package-lock.json* ./
RUN npm install --legacy-peer-deps

# Build de producción
COPY . .
ENV NODE_ENV=production
RUN npm run build

# Runtime con nginx - configuración que se aplica en runtime, no en build
FROM nginx:alpine

# Instalar curl para health checks
RUN apk add --no-cache curl

# Remover configuración default
RUN rm /etc/nginx/conf.d/default.conf

# Copiar archivos y configuración
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

# Crear script que valida configuración solo en runtime
RUN echo '#!/bin/sh' > /docker-entrypoint.sh && \
    echo 'echo "Starting DashMaster Frontend"' >> /docker-entrypoint.sh && \
    echo 'nginx -g "daemon off;"' >> /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/docker-entrypoint.sh"]