fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.kotlin) apply false
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
//    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
}

tasks.register("uploadPlugins") {
    dependsOn(":fleet-backend-plugin:publishPlugin")
    dependsOn(":gradle-daemons-plugin:uploadPlugin")
}