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

# Copy compiled app from build stage
COPY --from=build /app/target /app/target

# Set PORT for Render
ENV PORT=8888


# Expose port
EXPOSE 8888

# Launch Vert.x app
CMD ["java", "-cp", "target/classes:target/dependency/*", "com.itinerary.Main"]
