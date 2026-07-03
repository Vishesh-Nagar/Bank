# ── Stage 1: Build React/Vite SPA ────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Build the production bundle (Vite outputs to /app/dist)
RUN npm run build

# ── Stage 2: Serve with Nginx ─────────────────────────────────────────────────
FROM nginx:1.27-alpine
# Remove the default Nginx config
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

ENV API_GATEWAY_URL="http://bank-api-gateway:8080"

EXPOSE 80
CMD envsubst '${API_GATEWAY_URL}' < /etc/nginx/conf.d/default.conf > /etc/nginx/conf.d/default.conf.tmp && \
    mv /etc/nginx/conf.d/default.conf.tmp /etc/nginx/conf.d/default.conf && \
    nginx -g 'daemon off;'
