# Multi-stage build para aplicación React/Vite usando Bun
FROM oven/bun:1 AS build

WORKDIR /app

# Copiar archivos de dependencias
COPY package.json bun.lock* ./

# Instalar dependencias con Bun (más rápido y maneja mejor los conflictos)
RUN bun install

# Copiar código fuente
COPY . .

# Construir la aplicación para producción
RUN bun run build

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