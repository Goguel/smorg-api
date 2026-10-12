# 1. Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copia arquivos do Gradle do módulo api
COPY api/gradle api/gradle
COPY api/gradlew api/gradlew.bat api/settings.gradle.kts api/build.gradle.kts api/gradle.properties ./api/
RUN chmod +x ./api/gradlew

# Copia código-fonte e gera o pacote de distribuição
COPY api/src ./api/src
WORKDIR /app/api
RUN ./gradlew installDist --no-daemon

# 2. Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/api/build/install/api /app

ENV PORT=8080
ENV HOST=0.0.0.0

EXPOSE 8080

ENTRYPOINT ["/app/bin/api"]
