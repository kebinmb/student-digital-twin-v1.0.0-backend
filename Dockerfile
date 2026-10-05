# Multi-Stage Build for Spring Boot Backend (Java 21)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and dependencies specification
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Resolve dependencies in a cached layer
RUN ./mvnw dependency:go-offline -B

# Copy source code and build production artifact
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Production Runtime Image
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S sdtgroup && adduser -S sdtuser -G sdtgroup
USER sdtuser:sdtgroup

# Copy compiled jar from builder stage
COPY --from=builder --chown=sdtuser:sdtgroup /workspace/target/web-app-0.0.1-SNAPSHOT.jar app.jar

# Expose HTTP API port and Actuator management port
EXPOSE 8080 8081

# Recommended JVM flags for containerized Spring Boot with Virtual Threads
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]
