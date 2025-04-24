FROM node:18

WORKDIR /app

COPY package.json ./
COPY bun.lockb ./

RUN npm install -g bun
RUN bun install

COPY . ./

RUN bun run build

# Install a simple HTTP server for serving static content
RUN npm install -g serve

EXPOSE 3000

CMD ["serve", "-s", "dist", "-l", "3000"]