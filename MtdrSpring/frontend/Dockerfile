FROM oven/bun:1 AS build

WORKDIR /app

# Instalar dependencias
COPY package.json bun.lockb ./
RUN bun install

# Copiar código fuente
COPY . .

# Construir aplicación
RUN bun run build

# Segunda etapa: imagen de producción
FROM nginx:alpine

# Copiar archivos de configuración para nginx
COPY --from=build /app/dist /usr/share/nginx/html
COPY --from=build /app/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]