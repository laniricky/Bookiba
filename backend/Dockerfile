# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM gradle:8.7-jdk17 AS build

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src

RUN ./gradlew buildFatJar --no-daemon -x test

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN mkdir -p /data

COPY --from=build /app/build/libs/*-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
