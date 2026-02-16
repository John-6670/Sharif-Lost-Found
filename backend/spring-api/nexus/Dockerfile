# --------------------------
# Stage 1: Build
# --------------------------
FROM eclipse-temurin:17-jdk-alpine AS build

# Set working directory
WORKDIR /app

# Copy only the pom.xml first (for caching dependencies)
COPY pom.xml .

# If you have Maven Wrapper, use it; else install Maven
RUN apk add --no-cache maven bash git

# Copy the full source code
COPY src ./src

# Build the Spring Boot app, skip tests to speed up
RUN mvn clean package -DskipTests

# --------------------------
# Stage 2: Runtime
# --------------------------
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
