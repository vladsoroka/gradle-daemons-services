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

val useLocalBackendPlugin: String? by project

fleetPlugin {
    id = "gradle.daemon.services"

    metadata {
        readableName = "Gradle Daemons View"
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
            if (useLocalBackendPlugin != null) {
                plugin(project(":fleet-backend-plugin", "pluginDist")) // reference local IJ plugin Gradle project
            } else {
                plugin("com.github.vladsoroka.gradledaemonservices", properties("pluginVersion").get())
            }
        }
    }

    publishing {
        token = properties("intellijPublishToken")
        channel = "stable"
    }
}

val rpcJars by configurations.creating
dependencies {
    "rpcJars"(files(configurations["commonApi-plugins"]).filter { it.name.contains("fleet.rpc-") })
}

// hack to have backend plugin distribution built when running Fleet
if (useLocalBackendPlugin != null) {
    dependencies {
        implementation(project(":fleet-backend-plugin", "pluginDist"))
    }
    tasks {
        runFleet {
            dependsOn(compileJava)
        }
    }
}
