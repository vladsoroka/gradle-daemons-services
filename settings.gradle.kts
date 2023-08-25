rootProject.name = "gradle-daemons-services"

include("gradle-daemons-plugin")
include("gradle-daemons-plugin:protocol")
include("fleet-backend-plugin")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        maven("https://packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client")
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/fleet/fleet-sdk")
            credentials {
                username = settings.providers.gradleProperty("spaceUsername").orNull
                password = settings.providers.gradleProperty("spacePassword").orNull
            }
        }
    }
}