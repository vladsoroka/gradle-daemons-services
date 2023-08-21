import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()
//    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

val fleetBackend by configurations.creating
configurations {
    compileOnly {
        extendsFrom(fleetBackend)
    }
}
val pluginDist by configurations.creating {
    isCanBeResolved = false
}
artifacts {
    add(pluginDist.name, tasks.getByName("buildPlugin"))
}

dependencies {
    val backendLibPath = properties("backendLibPath").get()
    "fleetBackend"(files("$backendLibPath/product.jar"))
    implementation(project(":gradle-daemons-plugin:protocol"))
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName = "gradle-daemons-services"
    version = properties("intellijVersion")
    type = properties("intellijType")

    plugins = listOf("com.intellij.gradle")
}

tasks {
    withType<PatchPluginXmlTask> {
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
    }

    withType<PublishPluginTask> {
        token.set(properties("intellijPublishToken"))
    }

    runIde { enabled = false }
    buildSearchableOptions { enabled = false }
}
