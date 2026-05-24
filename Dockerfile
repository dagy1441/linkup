# syntax=docker/dockerfile:1.7

# ============================================================================
# Stage 1 — Build (Maven, JDK 21, Spring Boot layered JAR)
# ============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Maven wrapper + pom first to leverage Docker layer cache for dependencies.
# BuildKit cache mount on ~/.m2 persists downloaded artifacts across builds — survives
# Maven Central flakiness and avoids re-downloading 200+ MB on every rebuild.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Pre-fetch dependencies (survives transient network errors via cache mount).
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -q dependency:go-offline -DskipTests

# Now copy source and build the fat jar
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -q clean package -DskipTests \
 && mv target/*.jar target/app.jar

# ============================================================================
# Stage 2 — Runtime (JRE only, non-root)
# ============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# wget for healthcheck; tini for proper PID 1 signal handling
RUN apk add --no-cache wget tini

# Non-root user
RUN addgroup -S linkup && adduser -S -G linkup linkup

WORKDIR /app

# Spring Boot fat jar. The jar's MANIFEST already declares Main-Class so `java -jar`
# resolves the launcher regardless of Spring Boot version (works for 3.x and 4.x).
COPY --from=builder --chown=linkup:linkup /workspace/target/app.jar /app/app.jar

USER linkup:linkup

EXPOSE 8080

# Container-aware memory + fail-fast on OOM. Override JAVA_OPTS at runtime to tune.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE="prod"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
