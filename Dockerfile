# syntax=docker/dockerfile:1.6

# ── Build stage ───────────────────────────────────────────────────────────────
# Resolve dependencies first against the pom alone, then copy sources so a
# source-only change re-uses the cached dependency layer.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp -e dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -e -DskipTests package && \
    cp target/ecommerce-*.jar /workspace/app.jar

# ── Runtime stage ─────────────────────────────────────────────────────────────
# eclipse-temurin:17-jre-jammy is multi-arch (amd64 + arm64); the Alpine variant
# of the same JRE is not published for arm64.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
