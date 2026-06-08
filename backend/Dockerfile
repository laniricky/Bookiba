# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM gradle:8.7-jdk17 AS build

WORKDIR /app

# Copy gradle files first for dependency caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./

RUN chmod +x gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build fat JAR
COPY src ./src
RUN ./gradlew buildFatJar --no-daemon

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/build/libs/*-all.jar app.jar

# Create directory for SQLite database
RUN mkdir -p /data

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
