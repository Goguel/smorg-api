# 1. Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copia arquivos de configuração do Gradle para aproveitar cache de camadas
COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
RUN chmod +x ./gradlew

# Copia o código fonte e gera o pacote de distribuição
COPY src/ src/
RUN ./gradlew installDist --no-daemon

# 2. Runtime stage (imagem enxuta contendo apenas o JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia a aplicação compilada
COPY --from=build /app/build/install/api /app

ENV PORT=8080
ENV HOST=0.0.0.0

EXPOSE 8080

ENTRYPOINT ["/app/bin/api"]
