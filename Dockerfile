# Build is two steps. Step 1 uses heavier image with everything needed to compile
# the code (full Maven/JDK image). Step 2 copies finished .jar into smaller image
# minus build tools, leaving only necessities
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copying pom.xml and resolving dependencies before copying source lets Docker cache
# this layer, so a source-only change doesn't trigger a full dependency re-download
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
