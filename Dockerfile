# Multi-stage build for Alemandan POS
# Stage 1: Build the application with Maven
FROM eclipse-temurin:17-jdk as builder

WORKDIR /build

# Copy Maven wrapper and pom.xml first for dependency caching
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /build/target/*.jar /app/app.jar

# Expose port 8080 (Render will use PORT env var)
EXPOSE 8080

# Run the application with PORT environment variable support
# Render provides PORT env var, which we pass to Spring Boot
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "/app/app.jar"]
