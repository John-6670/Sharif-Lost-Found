# Use official Java 17 JDK image
FROM eclipse-temurin:21-jdk-alpine AS build

# Set working directory inside the container
WORKDIR /app

COPY . /app

# Install Maven (for Alpine Linux)
RUN apk add --no-cache maven bash git

# Build the app, skip tests to speed up and avoid test failures
RUN mvn clean package -DskipTests

# --------------------------
# Runtime stage
# --------------------------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Start the app
ENTRYPOINT ["java","-jar","app.jar"]
