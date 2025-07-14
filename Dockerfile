# Use a lightweight JDK base image
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy Maven build files
COPY pom.xml .

# Download dependencies (cached)
RUN apk add --no-cache maven && mvn dependency:go-offline

# Copy the entire project
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Expose port used by Vert.x
EXPOSE 8888

# Set environment variables directory for secrets (optional)
ENV DOTENV_PATH=.env

# Start the application
CMD ["java", "-cp", "target/classes:target/dependency/*", "com.itinerary.Main"]
