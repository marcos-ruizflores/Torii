# syntax=docker/dockerfile:1

# ---------- Stage 1: build the JAR ----------
# Maven + JDK 21 image (the version pinned in pom.xml). The build runs INSIDE the
# container so it doesn't depend on whatever Java is installed locally.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the dependency files first to take advantage of Docker layer caching: if
# they don't change, dependencies aren't downloaded again on every build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

# Now the source and packaging. Tests are skipped here (they run locally / in CI),
# we just want the artifact as fast as possible.
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---------- Stage 2: runtime image ----------
# Just the Java runtime, no Maven or sources, so the final image is smaller.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user instead of root.
RUN useradd --system --uid 1001 torii
USER torii

# Grab the built JAR from the previous stage.
COPY --from=build /app/target/torii-*.jar app.jar

# Railway injects PORT, application.properties reads it (${PORT:8080}).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
