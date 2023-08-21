package com.github.vladsoroka.gradle.daemon.services

import fleet.backend.FleetServiceDescriptor
import fleet.backend.FleetServiceProvider
import fleet.backend.workspace.BackendWorkspace
import fleet.backend.workspace.WorkspaceAwareApi
import fleet.gradle.daemons.protocol.GradleDaemonsApi
import org.jetbrains.plugins.gradle.internal.daemon.GradleDaemonServices

class FleetGradleDaemonsApiProvider : FleetServiceProvider {
    override fun getServices(workspace: BackendWorkspace): List<FleetServiceDescriptor> {
        return listOf(
            GradleDaemonsApi::class to {
                BackendGradleDaemonsApi(workspace)
            }
        )
    }
}

private class BackendGradleDaemonsApi(ws: BackendWorkspace) : WorkspaceAwareApi(ws), GradleDaemonsApi {
    override suspend fun listDaemons(): List<String> {
        @Suppress("UnstableApiUsage")
        return GradleDaemonServices.getDaemonsStatus().map { it.description }
    }
}
