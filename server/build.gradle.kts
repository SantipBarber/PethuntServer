plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization)
    application
}

group = "com.pethunt.server"
version = "1.0.0"
application {
    mainClass.set("com.pethunt.server.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    // Ktor Core
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Ktor Features
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)

    // Exposed (todas las dependencias necesarias)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
    implementation(libs.exposed.crypt)

    // PostgreSQL
    implementation(libs.postgres.jdbc)

    // MongoDB
    implementation(libs.kmongo.core)
    implementation(libs.kmongo.coroutine)

    // Redis
    implementation(libs.jedis)

    // Dependency Injection
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger)

    // Nuevas dependencias
    implementation(libs.kotlinx.datetime)
    implementation(libs.cryptography.core)
    implementation(libs.cryptography.provider.jdk)

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}