rootProject.version = libs.versions.jvmcraft.get()

plugins {
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.vannitktech.maven.publish).apply(false)
}

allprojects {
    group = "io.github.xmm"
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}
