import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.xmm"
version = "1.0.0"

repositories {
    mavenCentral()
}

val jacksonVersion = "2.18.0"

dependencies {
    implementation("io.lettuce:lettuce-core:6.5.1.RELEASE")
    implementation("io.ratpack:ratpack-core:1.9.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.114.Final:osx-aarch_64")

    testImplementation(kotlin("test"))
    testImplementation("io.ratpack:ratpack-test:1.9.0")
    testImplementation("org.slf4j:slf4j-simple:1.7.36")
    testImplementation("com.redis:testcontainers-redis:2.2.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name.set("JvmCraft")
        description.set("An extension enhancing functionality for JVM-based libraries and simplifying development.")
        inceptionYear.set("2025")
        url.set("https://github.com/xmm/jvmcraft")

        issueManagement {
            url.set("https://github.com/xmm/jvmcraft/issues")
        }

        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/xmm/jvmcraft/blob/main/LICENSE.txt")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/xmm/jvmcraft.git")
            developerConnection.set("scm:git:ssh://github.com:xmm/jvmcraft.git")
            url.set("https://github.com/xmm/jvmcraft")
        }

        developers {
            developer {
                id.set("xmm")
                name.set("Marat Khairullin")
                email.set("xmm.dev@gmail.com")
                url.set("https://xmm.github.io")
            }
        }
    }
}
