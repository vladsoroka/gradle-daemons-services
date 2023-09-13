package com.github.vladsoroka.gradle.daemon.services

import fleet.backend.FleetServiceDescriptor
import fleet.backend.FleetServiceProvider
import fleet.backend.workspace.BackendWorkspace
import fleet.backend.workspace.WorkspaceAwareApi
import fleet.gradle.daemons.protocol.DaemonExpirationStatus
import fleet.gradle.daemons.protocol.DaemonInfo
import fleet.gradle.daemons.protocol.DaemonState
import fleet.gradle.daemons.protocol.GradleDaemonsApi
import fleet.util.logging.KLoggers
import org.jetbrains.plugins.gradle.internal.daemon.GradleDaemonServices

private val logger by lazy { KLoggers.logger(FleetGradleDaemonsApiProvider::class) }

class FleetGradleDaemonsApiProvider : FleetServiceProvider {
    override fun getServices(workspace: BackendWorkspace): List<FleetServiceDescriptor> {
        return listOf(
            GradleDaemonsApi::class to {
                BackendGradleDaemonsApi(workspace)
            }
        )
    }
}

@Suppress("UnstableApiUsage")
private class BackendGradleDaemonsApi(ws: BackendWorkspace) : WorkspaceAwareApi(ws), GradleDaemonsApi {
    override suspend fun listDaemons(includeStopped: Boolean): List<DaemonInfo> {
        logger.trace { "Daemons list request..." }
        return GradleDaemonServices.getDaemonsStatus()
            .filter { includeStopped || it.token != null }
            .map { it.toDaemonInfo() }
    }

    override suspend fun stopAll(whenIdle: Boolean) {
        if (whenIdle) {
            logger.trace { "Graceful stop all daemons request..." }
            GradleDaemonServices.gracefulStopDaemons()
        }
        else {
            logger.trace { "Stop all daemons request..." }
            GradleDaemonServices.stopDaemons()
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
