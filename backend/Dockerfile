# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime environment
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the compiled JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Make port 8080 available to Render
EXPOSE 8080

# Environment variables should be supplied via Render Dashboard
ENTRYPOINT ["java", "-jar", "app.jar"]
