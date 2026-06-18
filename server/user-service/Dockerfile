# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Maven wrapper and parent POM first (layer caching)
COPY pom.xml ./pom.xml
COPY mvnw ./mvnw
COPY mvnw.cmd ./mvnw.cmd
COPY .mvn ./.mvn

# Copy common-lib (shared dependency of all services)
COPY common-lib ./common-lib

# Copy this service's source
COPY user-service ./user-service

RUN chmod +x mvnw
# Install common-lib into local .m2, then build only user-service
RUN ./mvnw -B -pl common-lib install -DskipTests
RUN ./mvnw -B -pl user-service package -DskipTests

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/user-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
