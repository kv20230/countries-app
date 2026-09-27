# Build stage: compile the Spring Boot jar with the Gradle wrapper.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Wrapper and build files first, so dependencies are cached until they change.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN sh ./gradlew dependencies --no-daemon > /dev/null

COPY src src
RUN sh ./gradlew bootJar --no-daemon -x test

# Run stage: only the JRE and the jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
