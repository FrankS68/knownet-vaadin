# ---- Build-Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

ARG GITHUB_TOKEN

# settings.xml mit GitHub Token fuer witch-auth von GitHub Packages
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>token</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

ENV GITHUB_TOKEN=${GITHUB_TOKEN}

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B -q || true

COPY src ./src
COPY frontend ./frontend
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -Pproduction -DskipTests -B -q

# ---- Runtime-Stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
