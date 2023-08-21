plugins {
    id("java")
    alias(libs.plugins.kotlin)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(project(":gradle-daemons-plugin", "rpcJars"))
}

kotlin {
    jvmToolchain(17)
}

val moduleName = "fleet.gradle.daemons.protocol"
tasks {
    compileJava {
        inputs.property("moduleName", moduleName)
        options.compilerArgs = listOf(
            "--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}"
        )
    }
}