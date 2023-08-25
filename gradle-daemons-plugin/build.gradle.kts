fun properties(key: String) = providers.gradleProperty(key)

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleFleetPlugin)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

fleetPlugin {
    id = "gradle-daemons-services"

    metadata {
        vendor = "Vladislav Soroka"
        readableName = "Gradle Daemons Services"
        description = "Provides actions to view and stop Gradle daemons"
    }
    fleetRuntime {
        version = properties("fleetRuntimeVersion").get()
        minVersion = properties("fleetRuntimeMinVersion").get()
        maxVersion = properties("fleetRuntimeMaxVersion").get()
    }

    layers {
        commonImpl {
            dependencies {
                api(project(":gradle-daemons-plugin:protocol"))
            }
        }
    }

    pluginDependencies {
        plugin("fleet.gradle")
    }
    backendRequirements {
        intellij {
//            plugin(project(":fleet-backend-plugin", "pluginDist")) // reference local IJ plugin Gradle project
            plugin("com.github.vladsoroka.gradledaemonservices", properties("pluginVersion").get())
        }
    }
}

val rpcJars by configurations.creating
dependencies {
    "rpcJars"(files(configurations["commonApi-plugins"]).filter { it.name.contains("fleet.rpc-") })
}
