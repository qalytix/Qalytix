# Qalytix — combined single-image build
# Produces one image that runs both the React frontend (Nginx) and
# the Spring Boot backend (Java), managed by supervisord.
#
# Usage:
#   docker build -t sddmhossain/qalytix:latest .
#   docker push sddmhossain/qalytix:latest

# ── Stage 1: build frontend ───────────────────────────────────────────────────
FROM node:24-alpine AS frontend-build
WORKDIR /app
COPY qalytix-ui/package*.json ./
RUN npm ci --silent
COPY qalytix-ui/ .
RUN npm run build

# ── Stage 2: build backend ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /app
COPY qalytix-backend/.mvn/ .mvn/
COPY qalytix-backend/mvnw qalytix-backend/pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY qalytix-backend/src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 3: combined runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Install Nginx + Supervisor
RUN apk add --no-cache nginx supervisor

# Frontend static files
COPY --from=frontend-build /app/dist /usr/share/nginx/html

# Nginx config (proxies /api and /ws to localhost:8080)
COPY nginx-combined.conf /etc/nginx/http.d/default.conf

# Backend JAR
COPY --from=backend-build /app/target/*.jar /app/app.jar

# Supervisor config
COPY supervisord.conf /etc/supervisord.conf

EXPOSE 80

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
