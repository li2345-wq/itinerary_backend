# ===== Build Stage =====
FROM maven:3.9.6-eclipse-temurin-21 as build

WORKDIR /app

# Preload dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY . .
RUN mvn clean package -DskipTests

# ===== Runtime Stage =====
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy shaded (fat) JAR from build stage
COPY --from=build /app/target/itinerary-planner-1.0.0.jar /app/app.jar

# Set PORT for Render compatibility
ENV PORT=8888

# Expose the port
EXPOSE 8888

# Start the app using fat JAR
CMD ["java", "-jar", "/app/app.jar"]
