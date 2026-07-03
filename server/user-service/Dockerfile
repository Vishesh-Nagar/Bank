# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Maven wrapper and parent POM first (layer caching)
COPY pom.xml ./pom.xml
COPY mvnw ./mvnw
COPY .mvn ./.mvn

# Copy ALL module POMs for Maven reactor resolution (tiny files)
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY account-service/pom.xml ./account-service/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml

# Copy source only for common-lib (shared dependency) + this service
COPY common-lib/src ./common-lib/src
COPY user-service/src ./user-service/src

RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -pl user-service -am package -DskipTests

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/user-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
