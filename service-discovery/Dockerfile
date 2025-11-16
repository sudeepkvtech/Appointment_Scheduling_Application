FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy parent pom and shared library
COPY pom.xml /app/parent/pom.xml
COPY shared-library /app/parent/shared-library

# Copy service-discovery module
COPY service-discovery/pom.xml /app/service-discovery/pom.xml
COPY service-discovery/src /app/service-discovery/src

# Build the application
WORKDIR /app/parent
RUN ./mvnw clean package -DskipTests -pl service-discovery -am

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/parent/service-discovery/target/*.jar app.jar

# Expose port
EXPOSE 8761

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
