import org.jetbrains.intellij.IntelliJPluginConstants
import org.jetbrains.intellij.tasks.InstrumentCodeTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)
}
val useLocalBackendPlugin: String? by project
version = properties("pluginVersion").get() + (useLocalBackendPlugin?.run { "." + System.currentTimeMillis() } ?: "")
group = properties("pluginGroup").get()

repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

val pluginDist by configurations.creating {
    isCanBeResolved = false
}
artifacts {
    add(pluginDist.name, tasks.buildPlugin)
}

dependencies {
    implementation(project(":gradle-daemons-plugin:protocol"))
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName = "gradle-daemons-services"
    version = properties("intellijVersion")
    type = IntelliJPluginConstants.PLATFORM_TYPE_FLEET

    plugins = listOf("com.intellij.gradle")
}

tasks {
    withType<PatchPluginXmlTask> {
        sinceBuild.set(properties("backendPluginSinceBuild"))
        untilBuild.set(properties("backendPluginUntilBuild"))
    }

    withType<PublishPluginTask> {
        token.set(properties("intellijPublishToken"))
    }

    withType<InstrumentCodeTask> {
        compilerVersion = properties("intellijInstrumentingCompilerVersion")
    }

    runIde { enabled = false }
    buildSearchableOptions { enabled = false }
}
