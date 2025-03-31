# Build stage
FROM maven:3.8-openjdk-8 AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:8-jre-slim

WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Copy built artifact from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create directory for logs
RUN mkdir -p /app/logs && \
    chmod 777 /app/logs

# Set environment variables
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Expose the application port
EXPOSE 9000

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
