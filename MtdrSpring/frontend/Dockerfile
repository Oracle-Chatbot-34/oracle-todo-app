# Multi-stage build para aplicación React/Vite usando npm
FROM node:18-alpine AS build

WORKDIR /app

# Configurar npm para evitar problemas de dependencias
RUN npm config set fund false
RUN npm config set audit-level none

# Copiar archivos de dependencias
COPY package.json package-lock.json* ./

# Limpiar cache de npm e instalar dependencias con flags robustos
RUN npm cache clean --force
RUN npm install --legacy-peer-deps --no-audit --no-fund

# Copiar código fuente
COPY . .

# Construir la aplicación para producción
RUN npm run build

# Verificar que el build fue exitoso
RUN ls -la dist/ || ls -la build/

# Etapa de producción con nginx
FROM nginx:alpine

# Copiar archivos construidos al directorio de nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuración personalizada de nginx si existe
COPY nginx.conf /etc/nginx/nginx.conf

# Exponer puerto 80
EXPOSE 80

# Comando por defecto
CMD ["nginx", "-g", "daemon off;"]