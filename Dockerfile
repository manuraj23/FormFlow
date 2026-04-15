# Use lightweight Java image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy jar file
COPY target/*.jar app.jar

# Expose port (same as SERVER_PORT)
EXPOSE 8082

# Run application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]