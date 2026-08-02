# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar el JAR ----------
# Usamos una imagen con Maven + JDK 21 (la versión que fija el pom.xml). El build
# ocurre DENTRO del contenedor, así no depende del Java instalado en tu máquina.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos primero solo los ficheros de dependencias para aprovechar la caché de
# capas de Docker: si no cambian, no se vuelven a descargar en cada build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

# Ahora el código y el empaquetado. Saltamos los tests en la imagen (se ejecutan
# en tu máquina / CI); aquí solo queremos el artefacto lo más rápido posible.
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---------- Etapa 2: imagen de ejecución ----------
# Solo el runtime de Java (sin Maven ni código fuente): imagen final más ligera.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Un usuario sin privilegios en vez de root (buena práctica de seguridad).
RUN useradd --system --uid 1001 torii
USER torii

# Copiamos el JAR ya compilado desde la etapa anterior.
COPY --from=build /app/target/torii-*.jar app.jar

# Railway inyecta la variable PORT; application.properties la lee (${PORT:8080}).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
