plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
    // Plugin Protobuf para compilar o contrato .proto e gerar stubs Java e Kotlin
    id("com.google.protobuf") version "0.10.0"
}

group = "com.smorg"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.netty)
    implementation("io.ktor:ktor-server-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    implementation("io.ktor:ktor-server-status-pages:3.5.2")
    implementation("io.ktor:ktor-server-routing-openapi:3.5.2")
    implementation("io.ktor:ktor-server-swagger:3.5.2")
    implementation(libs.logback.classic)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // Cliente gRPC para integração com microsserviço Go (stubs com Kotlin Coroutines)
    implementation(platform("io.grpc:grpc-bom:1.85.0"))
    implementation("io.grpc:grpc-netty-shaded")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-kotlin-stub:1.5.0")
    implementation("com.google.protobuf:protobuf-kotlin:4.36.2")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation("io.ktor:ktor-client-content-negotiation:3.5.2")

    // Testcontainers: PostgreSQL real em container descartável para testes de integração
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")

    // ArchUnit: Teste de arquitetura para validar integridade da Clean Architecture
    testImplementation("com.tngtech.archunit:archunit:1.5.0")

    // Servidor gRPC em memória para testes do cliente sem necessidade de subir o Go
    testImplementation("io.grpc:grpc-inprocess")
}

// O contrato Protocol Buffers é compartilhado na pasta ../protos
sourceSets {
    main {
        proto {
            srcDir("../protos")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.36.2"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.85.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.5.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    maxHeapSize = "512m"
}
