fun properties(key: String) = providers.gradleProperty(key)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fleet.plugin.layer)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(project(":gradle-daemons-plugin:protocol"))
            }
        }
    }
}