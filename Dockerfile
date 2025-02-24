FROM  maven:3.8.4-openjdk-17-slim  AS stage1
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM --platform=linux/amd64 openjdk:17-alpine AS stage2
WORKDIR /app
# Copy the built JAR file from the previous stage to the container
COPY --from=stage1 /app/target/flightservices-0.0.1-SNAPSHOT.jar .
CMD ["java", "-jar", "flightservices-0.0.1-SNAPSHOT.jar"]