# ---- Build-Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

ARG GITHUB_TOKEN
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

# settings.xml mit GitHub Token fuer witch-auth von GitHub Packages
RUN mkdir -p /root/.m2 && printf '<?xml version="1.0"?><settings><servers><server><id>github</id><username>FrankS68</username><password>%s</password></server></servers></settings>' "$GITHUB_TOKEN" > /root/.m2/settings.xml

COPY pom.xml .
RUN mvn dependency:go-offline -q || true
COPY src ./src
COPY frontend ./frontend
RUN mvn package -Pproduction -DskipTests -q

# ---- Runtime-Stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
