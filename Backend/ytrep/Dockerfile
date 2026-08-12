# --- Build Stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Run Stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for Docker healthcheck support
RUN apk add --no-cache curl

# Copy the built jar from build stage
COPY --from=build /app/target/ytrep-*.jar app.jar

# Expose backend port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
