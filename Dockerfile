# syntax=docker/dockerfile:1.7

# ============================================================================
# Stage 1 — Build (Maven, JDK 21, Spring Boot layered JAR)
# ============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Maven wrapper + pom first to leverage Docker layer cache for dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
 && ./mvnw -B -q dependency:go-offline -DskipTests

# Now copy source and build
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests \
 && mv target/*.jar target/app.jar \
 && java -Djarmode=tools -jar target/app.jar extract --layers --destination target/extracted

# ============================================================================
# Stage 2 — Runtime (JRE only, non-root, layered for fast cold starts)
# ============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Curl + wget for healthchecks; tini for proper PID 1 signal handling
RUN apk add --no-cache wget tini

# Non-root user
RUN addgroup -S linkup && adduser -S -G linkup linkup

WORKDIR /app

# Copy each Spring Boot layer separately for optimal Docker cache reuse
COPY --from=builder --chown=linkup:linkup /workspace/target/extracted/dependencies/ ./
COPY --from=builder --chown=linkup:linkup /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=linkup:linkup /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=linkup:linkup /workspace/target/extracted/application/ ./

USER linkup:linkup

EXPOSE 8080

# Container-aware memory + fail-fast on OOM. Override JAVA_OPTS at runtime to tune.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE="prod"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher \"$@\"", "--"]
