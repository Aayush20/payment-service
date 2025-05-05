# Use official OpenJDK image as base
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Maven build output (JAR) into container
COPY target/payment-service-*.jar app.jar

# Expose the service port (matches `server.port` in application.properties)
EXPOSE 8083

# Entry point
ENTRYPOINT ["java", "-jar", "app.jar"]
