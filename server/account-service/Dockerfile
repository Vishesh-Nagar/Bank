# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY mvnw ./mvnw
COPY mvnw.cmd ./mvnw.cmd
COPY .mvn ./.mvn

COPY common-lib ./common-lib
COPY account-service ./account-service

RUN chmod +x mvnw
RUN ./mvnw -B -pl common-lib install -DskipTests
RUN ./mvnw -B -pl account-service package -DskipTests

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/account-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
