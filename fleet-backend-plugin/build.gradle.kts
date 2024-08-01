import org.jetbrains.intellij.IntelliJPluginConstants
import org.jetbrains.intellij.tasks.InstrumentCodeTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellij.plugin)
    alias(libs.plugins.rpc.compiler)
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

    // todo although rpc classes are in "product.jar" it cannot be used for Fleet RPC provider code compilation
    compileOnly(project(":gradle-daemons-plugin", "rpcJars"))
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName = "gradle-daemons-services"
    version = libs.versions.intellij.runtime.asProvider().get()
    type = IntelliJPluginConstants.PLATFORM_TYPE_FLEET

    plugins = listOf("com.intellij.gradle")
}

tasks {
    withType<PatchPluginXmlTask> {
        sinceBuild = libs.versions.intellij.runtime.min.version
        untilBuild = libs.versions.intellij.runtime.max.version
    }

    withType<PublishPluginTask> {
        token.set(properties("intellijPublishToken"))
    }

    withType<InstrumentCodeTask> {
        compilerVersion = libs.versions.intellij.instrumenting.compiler
    }

    runIde { enabled = false }
    buildSearchableOptions { enabled = false }
}
