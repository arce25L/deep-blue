# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# -DskipTests: los tests con Testcontainers necesitan Docker dentro del build
RUN mvn -B -q -DskipTests package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/deepblue-rescue-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
