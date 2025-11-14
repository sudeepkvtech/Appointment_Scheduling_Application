FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent pom
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./

# Copy shared library
COPY shared-library ./shared-library

# Copy scheduling service
COPY scheduling-service/pom.xml ./scheduling-service/
COPY scheduling-service/src ./scheduling-service/src

# Build the application
RUN mvn clean package -pl scheduling-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/scheduling-service/target/*.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
