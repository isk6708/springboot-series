# syntax=docker/dockerfile:1

# ───────────── Stage 1: build Angular (lab-app-auth) ─────────────
FROM node:22-alpine AS frontend
WORKDIR /build/lab-app-auth

# Install dependencies first (cached unless package*.json changes)
COPY lab-app-auth/package.json lab-app-auth/package-lock.json ./
RUN npm ci

# Build the production bundle (ng build does NOT run unit tests)
COPY lab-app-auth/ ./
RUN npx ng build --configuration production
# Output: /build/lab-app-auth/dist/lab-raj-app/browser

# Angular unit tests are skipped. To enable them later, uncomment:
# RUN npx ng test --watch=false


# ───────────── Stage 2: build Spring Boot (lab-raj) with Angular inside ─────────────
FROM eclipse-temurin:17-jdk AS backend
WORKDIR /build/lab-raj

# Maven wrapper + pom first (dependency download is cached)
COPY lab-raj/.mvn/ .mvn/
COPY lab-raj/mvnw lab-raj/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Source code + Angular static files
COPY lab-raj/src/ src/
COPY --from=frontend /build/lab-app-auth/dist/lab-raj-app/browser/ src/main/resources/static/

# Tests are SKIPPED: -Dmaven.test.skip=true = do not compile AND do not run tests
RUN ./mvnw -B -Dmaven.test.skip=true package
# Output: /build/lab-raj/target/lab-raj-0.0.1-SNAPSHOT.jar

# To run the backend tests during the image build later, use this line instead:
# RUN ./mvnw -B package


# ───────────── Stage 3: small runtime image ─────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as a normal user, not root
RUN useradd --system --create-home --uid 1001 appuser \
 && mkdir -p /app/data && chown -R appuser /app

COPY --from=backend /build/lab-raj/target/lab-raj-*.jar /app/app.jar

ENV SERVER_PORT=8117 \
    SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/heitechdata.db \
    JAVA_OPTS=""

VOLUME ["/app/data"]
EXPOSE 8117
USER appuser

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]