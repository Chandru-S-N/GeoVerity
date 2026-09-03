# Multi-stage Dockerfile for GeoVerity Spring Boot Backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S geoverity && adduser -S geoverity -G geoverity
USER geoverity
COPY --from=builder /app/target/geoverity-backend-1.0.0.jar app.jar
EXPOSE 8080
VOLUME /app/keys
ENTRYPOINT ["java", "-jar", "app.jar"]
