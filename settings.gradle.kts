rootProject.name = "gradle-daemons-services"

include("gradle-daemons-plugin")
include("gradle-daemons-plugin:protocol")
include("gradle-daemons-plugin:frontendImpl")
include("fleet-backend-plugin")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        maven("https://packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client")
        maven("https://download.jetbrains.com/teamcity-repository")
        maven("https://packages.jetbrains.team/maven/p/fleet/fleet-sdk")
    }
}