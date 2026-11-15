# syntax=docker/dockerfile:1
#
# Build:  docker build --secret id=github_token,src=.github_token -t knownet-vaadin:latest .
#   oder per Compose (build.secrets), siehe docker-compose.yml.
# Das GitHub-Token (classic, Scope read:packages) wird fuer witch-parent, witch-ui und
# witch-auth aus GitHub Packages gebraucht. Es wird als BuildKit-Secret eingebunden und
# landet weder im Image noch im Build-Cache.

# ---- Build-Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=secret,id=github_token,required=true \
    mkdir -p /root/.m2 \
    && printf '<settings><servers><server><id>github</id><username>token</username><password>%s</password></server></servers></settings>' \
         "$(tr -d '\r\n' < /run/secrets/github_token)" > /tmp/settings.xml \
    && mvn -s /tmp/settings.xml package -DskipTests -B -q \
    && rm /tmp/settings.xml

# ---- Runtime-Stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
