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

tasks.register("pluginsDist") {
    dependsOn(":gradle-daemons-plugin:distZip")
    dependsOn(":fleet-backend-plugin:buildPlugin")
}

tasks.register("uploadPlugins") {
    dependsOn(":gradle-daemons-plugin:uploadPlugin")
    dependsOn(":fleet-backend-plugin:publishPlugin")
}