# Multi-stage build for Alemandan POS
# Stage 1: Build the application with Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /build/target/*.jar /app/app.jar

# Expose port 8080 (Render will use PORT env var)
EXPOSE 8080

# Run the application with PORT environment variable support
# Render provides PORT env var, which we pass to Spring Boot
# Using shell form to allow environment variable substitution
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
