# Image frontend : bundle Vite servi par nginx non root, /api relayé vers le backend (même origine).
#   docker build -f infra/frontend.Dockerfile -t eduquest-frontend .
FROM node:22-alpine AS build
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY infra/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=3s CMD wget -qO- http://127.0.0.1:8081/ >/dev/null || exit 1
