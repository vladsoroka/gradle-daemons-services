package com.github.vladsoroka.gradle.daemon.services

import fleet.backend.FleetServiceProvider
import fleet.backend.workspace.BackendServicesRegistration
import fleet.backend.workspace.BackendWorkspace
import fleet.backend.workspace.WorkspaceAwareApi
import fleet.backend.workspace.register
import fleet.gradle.daemons.protocol.DaemonExpirationStatus
import fleet.gradle.daemons.protocol.DaemonInfo
import fleet.gradle.daemons.protocol.DaemonState
import fleet.gradle.daemons.protocol.GradleDaemonsApi
import fleet.util.logging.KLoggers
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.build.BuildEnvironment
import org.jetbrains.plugins.gradle.internal.daemon.getDaemonsStatus
import org.jetbrains.plugins.gradle.internal.daemon.gracefulStopDaemons
import org.jetbrains.plugins.gradle.internal.daemon.stopDaemons
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val logger by lazy { KLoggers.logger(FleetGradleDaemonsApiProvider::class) }

class FleetGradleDaemonsApiProvider : FleetServiceProvider {
    override fun BackendServicesRegistration.contributeServices(workspace: BackendWorkspace) {
        register(GradleDaemonsApi) {
            BackendGradleDaemonsApi(workspace)
        }
    }
}

@Suppress("UnstableApiUsage")
private class BackendGradleDaemonsApi(ws: BackendWorkspace) : WorkspaceAwareApi(ws), GradleDaemonsApi {

    private val initialized = AtomicBoolean()

    override suspend fun listDaemons(includeStopped: Boolean): List<DaemonInfo> {
        logger.trace { "Daemons list request..." }
        ensureInit(ws)
        return getDaemonsStatus()
            .filter { includeStopped || it.token != null }
            .map { it.toDaemonInfo() }
    }

    override suspend fun stopAll(whenIdle: Boolean) {
        if (whenIdle) {
            logger.trace { "Graceful stop all daemons request..." }
            gracefulStopDaemons()
        } else {
            logger.trace { "Stop all daemons request..." }
            stopDaemons()
        }
    }

    private fun ensureInit(ws: BackendWorkspace) {
        if (initialized.get()) return
        val gradleSettings = ws.projects.firstNotNullOfOrNull { (_, backendProject) ->
            GradleSettings.getInstance(backendProject.ijProject).takeIf { it.linkedProjectsSettings.isNotEmpty() }
        } ?: return

        if (initialized.compareAndSet(false, true)) {
            val gradleProjectSettings = gradleSettings.linkedProjectsSettings.first()
            val projectConnection = GradleConnector.newConnector()
                .forProjectDirectory(File(gradleProjectSettings.externalProjectPath))
                .connect()
            runCatching {
                val buildEnvironment = projectConnection
                    .model(BuildEnvironment::class.java)
                    .setStandardError(OutputWrapper { logger.warn { it } })
                    .setStandardOutput(OutputWrapper { logger.debug { it } })
                    .get()
                logger.debug { "Init succeeded for: ${buildEnvironment.buildIdentifier.rootDir}" }
            }.onFailure {
                initialized.set(false)
                logger.warn(it) { "Unable to init tooling connection. Daemons information might be unavailable till usage of the Gradle tapi." }
            }
            projectConnection.close()
        }
    }
}

private fun org.jetbrains.plugins.gradle.internal.daemon.DaemonState.toDaemonInfo(): DaemonInfo {
    return DaemonInfo(
        pid = pid,
        version = version,
        state = toDaemonState(status),
        reason = reason,
        lastBusy = timestamp,
        daemonExpirationStatus = toDaemonExpirationStatus(daemonExpirationStatus),
        daemonOpts = daemonOpts ?: emptyList(),
        javaHome = javaHome?.path,
        idleTimeout = idleTimeout,
        registryDir = registryDir?.path
    )
}

fun toDaemonExpirationStatus(daemonExpirationStatus: String?): DaemonExpirationStatus {
    return when (daemonExpirationStatus) {
        "DO_NOT_EXPIRE" -> DaemonExpirationStatus.DoNotExpire
        "QUIET_EXPIRE" -> DaemonExpirationStatus.QuietExpire
        "GRACEFUL_EXPIRE" -> DaemonExpirationStatus.GracefulExpire
        "IMMEDIATE_EXPIRE" -> DaemonExpirationStatus.ImmediateExpire
        else -> DaemonExpirationStatus.Unknown(daemonExpirationStatus ?: "unknown")
    }
}

fun toDaemonState(status: String?): DaemonState {
    @Suppress("SpellCheckingInspection")
    return when (status?.uppercase()) {
        "IDLE" -> DaemonState.Idle
        "BUSY" -> DaemonState.Busy
        "CANCELED" -> DaemonState.Canceled
        "STOPREQUESTED" -> DaemonState.StopRequested
        "STOPPED" -> DaemonState.Stopped
        "BROKEN" -> DaemonState.Broken
        else -> DaemonState.Unknown(status ?: "unknown")
    }
}
