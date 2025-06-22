# Use a minimal JDK 17 base image
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the JAR from the build context
# You can also use build-arg to make this dynamic if needed
ARG JAR_FILE=target/eureka-discovery-server-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Expose Eureka port
EXPOSE 8761

# Run the Eureka Discovery Server
ENTRYPOINT ["java", "-jar", "app.jar"]
