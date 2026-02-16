# Use official Java 17 JDK
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy your Spring Boot module into container
COPY backend/spring-api/nexus /app

# If you have Maven Wrapper, use it; otherwise install Maven
RUN apk add --no-cache maven

# Build the app
RUN mvn clean package -DskipTests

# Expose default Spring Boot port
EXPOSE 8080

# Start the app
CMD ["java", "-jar", "target/nexus-0.0.1-SNAPSHOT.jar"]
