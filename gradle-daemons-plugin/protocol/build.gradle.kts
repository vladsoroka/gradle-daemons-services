fun properties(key: String) = providers.gradleProperty(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.rpc.compiler)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.serialization.json.jvm)
    compileOnly(project(":gradle-daemons-plugin", "rpcJars"))
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
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