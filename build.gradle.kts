plugins {
    kotlin("jvm") version "2.0.21"
}

group = "app.xmmdev"
version = "0.2-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.lettuce:lettuce-core:6.5.1.RELEASE")
    implementation("io.ratpack:ratpack-core:1.9.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.18.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.114.Final:osx-aarch_64")

    testImplementation(kotlin("test"))
    testImplementation("io.ratpack:ratpack-test:1.9.0")
    testImplementation( "org.slf4j:slf4j-simple:1.7.36")
    testImplementation("com.redis:testcontainers-redis:2.2.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}