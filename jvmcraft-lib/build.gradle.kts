import com.vanniktech.maven.publish.SonatypeHost

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vannitktech.maven.publish)
}

dependencies {
    compileOnly(libs.lettuce.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(libs.jackson.datatype.jdk8)
    compileOnly(libs.jackson.datatype.jsr310)
    compileOnly(libs.reactive.streams)
    compileOnly(libs.reactor.core)
    compileOnly(libs.slf4j.api)

    testImplementation(libs.lettuce.core)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.jackson.datatype.jdk8)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.testcontainers.redis)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.testng.engine)
    testImplementation(libs.reactive.streams.tck)
    testImplementation(libs.reactor.test)
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    test {
        java.srcDirs("src/test/java")
    }
}

kotlin {
    jvmToolchain(17)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name.set("Extension for JVM Libraries")
        description.set("An extension enhancing functionality for JVM-based libraries and simplifying development.")
    }
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
