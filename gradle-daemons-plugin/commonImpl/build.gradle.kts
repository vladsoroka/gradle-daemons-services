fun properties(key: String) = providers.gradleProperty(key)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fleet.plugin.layer)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

kotlin {
    jvmToolchain(21)
    sourceSets {
        jvmMain {
            dependencies {
                api(project(":gradle-daemons-plugin:protocol"))
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "21"
    }
}