# MKV Video Platform - Multi-stage Dockerfile

# Stage 1: Build with Maven
FROM tomcat:8.5-jdk8 AS builder

# Install Maven for building
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM tomcat:8.5-jdk8

# Install FFmpeg and curl
RUN apt-get update && apt-get install -y ffmpeg curl && rm -rf /var/lib/apt/lists/*

# Create video storage directory
RUN mkdir -p /data/videos

# Copy JAR - Spring Boot uses java -jar, not Tomcat deployment
COPY --from=builder /app/target/*.jar /app.jar

# Expose ports
EXPOSE 8080

# Volume for video storage
VOLUME ["/data/videos"]

# Copy entrypoint script
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && sed -i 's/\r$//' /docker-entrypoint.sh

# Environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV VIDEO_STORAGE_PATH="/data/videos"

# Use entrypoint script to run Spring Boot application
ENTRYPOINT ["/docker-entrypoint.sh"]
