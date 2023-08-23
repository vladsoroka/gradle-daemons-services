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

version = "0.1.0"

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
            plugin("com.github.vladsoroka.gradledaemonservices", "latest")
        }
    }
}

val rpcJars by configurations.creating
dependencies {
    "rpcJars"(files(configurations["commonApi-plugins"]).filter { it.name.contains("fleet.rpc-") })
}
