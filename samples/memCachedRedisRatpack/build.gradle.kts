plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.jvmcraft.lib)

    implementation(libs.lettuce.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.datatype.jsr310)

    implementation("io.ratpack:ratpack-core:1.9.0")
    implementation("me.drmaas:ratpack-kotlin-dsl:1.10.3")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.114.Final:osx-aarch_64")
    implementation(libs.testcontainers.redis)
    implementation(libs.slf4j.simple)
}

application {
    mainClass.set("memCachedRedisRatpack.Main")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("memCachedRedisRatpack")
}
