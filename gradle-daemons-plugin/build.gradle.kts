fun properties(key: String) = providers.gradleProperty(key)

listOf(
    repositories,
    *subprojects.map { it.repositories }.toTypedArray()
).forEach {
    it.mavenCentral()
    it.maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

plugins {
    base
    alias(libs.plugins.fleet.plugin)
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
        version = libs.versions.fleet.runtime.asProvider().get()
        maxVersion = libs.versions.fleet.runtime.max.version
    }

    pluginDependencies {
//        plugin("fleet.gradle")
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
//        channel = "nightly"
        channel = "stable"
    }
}

tasks.runFleet {
//    systemProperty("fleet.backend.debug", "true")
//    systemProperty("fleet.backend.debug.suspend", "y")
}

val rpcJars by configurations.creating
dependencies {
    "rpcJars"(
        files(configurations["commonApiCompileClasspathForDescriptorGeneration"])
            .filter { it.name.contains("fleet.rpc-") || it.name.contains("fleet.protocol-") }
    )
}

//// hack to have backend plugin distribution built when running Fleet
if (useLocalBackendPlugin != null) {
    val backendPluginDistDep by configurations.creating
    dependencies {
        backendPluginDistDep(project(":fleet-backend-plugin", "pluginDist"))
    }
    tasks {
        runFleet {
            dependsOn(backendPluginDistDep)
        }
    }
}
