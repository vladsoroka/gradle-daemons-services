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
    intellijPlatform {
        defaultRepositories()
        nightly()
    }
}

val pluginDist by configurations.creating {
    isCanBeResolved = false
}
artifacts {
    add(pluginDist.name, tasks.buildPlugin)
}

dependencies {
    intellijPlatform {
        fleetBackend(libs.versions.intellij.runtime.asProvider().get(), useInstaller = false)
        bundledPlugins("com.intellij.gradle")
        javaCompiler(libs.versions.intellij.instrumenting.compiler)
    }

    implementation(project(":gradle-daemons-plugin:protocol"))

    // todo although rpc classes are in "product.jar" it cannot be used for Fleet RPC provider code compilation
    compileOnly(project(":gradle-daemons-plugin", "rpcJars"))
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = libs.versions.intellij.runtime.min.version
            untilBuild = libs.versions.intellij.runtime.max.version
        }
    }

    publishing {
        token = properties("intellijPublishToken")
    }
}

tasks {
    runIde { enabled = false }
    buildSearchableOptions { enabled = false }
}
