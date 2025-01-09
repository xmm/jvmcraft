
plugins {
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.vannitktech.maven.publish).apply(false)
}

allprojects {
    group = "io.github.xmm"
    version = "2.0.0"

    repositories {
        mavenCentral()
    }
}
