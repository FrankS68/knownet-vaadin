# ---- Build-Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Dependencies vorab cachen
RUN mvn dependency:go-offline -q
COPY src ./src
COPY frontend ./frontend
# Vaadin Production Build (kein Dev-Server, minifiziertes Frontend)
RUN mvn package -Pproduction -DskipTests -q

# ---- Runtime-Stage ----
# eclipse-temurin:17-jre läuft nativ auf amd64 UND arm64 (Raspberry Pi 4/5)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Neo4j-Verbindung wird per Umgebungsvariable überschrieben (siehe docker-compose.yml)
ENV SPRING_NEO4J_URI=bolt://neo4j:7687
ENV SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j
ENV SPRING_NEO4J_AUTHENTICATION_PASSWORD=knownet123

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
