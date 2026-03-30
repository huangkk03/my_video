# MKV Video Platform - Multi-stage Dockerfile

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-8 AS builder

WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src

RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Stage 2: Runtime
FROM eclipse-temurin:8-jre

# Install FFmpeg and curl
RUN apt-get update && apt-get install -y ffmpeg curl && rm -rf /var/lib/apt/lists/*

# Create video storage directory
RUN mkdir -p /data/videos

# Copy JAR
COPY --from=builder /app/target/*.jar app.jar

# Expose ports
EXPOSE 8080

# Volume for video storage
VOLUME ["/data/videos"]

# Environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV VIDEO_STORAGE_PATH="/data/videos"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
