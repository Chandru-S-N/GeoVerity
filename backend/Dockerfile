# ==============================================================================
# GeoVerity Spring Boot Backend - Production Multi-stage Dockerfile
# Optimized for free-tier cloud containers (512MB RAM, Java 21 JRE)
# ==============================================================================

# Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B || true

COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S geoverity && adduser -S geoverity -G geoverity && \
    mkdir -p /app/data /app/keys && \
    chown -R geoverity:geoverity /app

USER geoverity

COPY --from=builder --chown=geoverity:geoverity /build/target/geoverity-backend-1.0.0.jar app.jar

ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-XX:+UseSerialGC -Xss512k -XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

VOLUME ["/app/data", "/app/keys"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
