FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent pom
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./

# Copy shared library
COPY shared-library ./shared-library

# Copy encounter service
COPY encounter-service/pom.xml ./encounter-service/
COPY encounter-service/src ./encounter-service/src

# Build the application
RUN mvn clean package -pl encounter-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/encounter-service/target/*.jar app.jar

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
