# Use a lightweight OpenJDK 17 base image
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container (replace with your actual jar name if needed)
COPY target/*.jar eureka-discovery-server.jar

# Expose the Eureka port
EXPOSE 8761

# Run the application
ENTRYPOINT ["java", "-jar", "eureka-discovery-server.jar"]
