# Imagem multi-stage da API Principal Ktor (Kotlin)
# Utiliza base Debian (glibc) compatível com os binários do protoc e protoc-gen-grpc
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia arquivos de configuração do Gradle
COPY api/gradle api/gradle
COPY api/gradlew api/gradlew.bat api/settings.gradle.kts api/build.gradle.kts api/gradle.properties ./api/
RUN chmod +x ./api/gradlew

# O build do Ktor necessita dos arquivos .proto em ../protos
COPY protos ./protos
COPY api/src ./api/src

WORKDIR /app/api
RUN ./gradlew installDist -x test --no-daemon

# Imagem final de runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Instala curl para health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/api/build/install/api /app

ENV PORT=8080
ENV HOST=0.0.0.0

EXPOSE 8080

ENTRYPOINT ["/app/bin/api"]
