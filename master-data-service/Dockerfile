FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent pom
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./

# Copy shared library
COPY shared-library ./shared-library

# Copy master-data service
COPY master-data-service/pom.xml ./master-data-service/
COPY master-data-service/src ./master-data-service/src

# Build the application
RUN mvn clean package -pl master-data-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/master-data-service/target/*.jar app.jar

# Expose port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8085/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
