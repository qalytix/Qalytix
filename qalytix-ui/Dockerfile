# ── Stage 1: build ───────────────────────────────────────────────────────────
FROM node:24-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci --silent

COPY . .
RUN npm run build

# ── Stage 2: serve with Nginx ─────────────────────────────────────────────────
FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
