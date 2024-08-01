fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.fleet.plugin) apply false
    alias(libs.plugins.fleet.plugin.layer) apply false
    alias(libs.plugins.rpc.compiler) apply false
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

tasks.register("pluginsDist") {
    dependsOn(":gradle-daemons-plugin:distZip")
    dependsOn(":fleet-backend-plugin:buildPlugin")
}

tasks.register("uploadPlugins") {
    dependsOn(":gradle-daemons-plugin:uploadPlugin")
    dependsOn(":fleet-backend-plugin:publishPlugin")
}