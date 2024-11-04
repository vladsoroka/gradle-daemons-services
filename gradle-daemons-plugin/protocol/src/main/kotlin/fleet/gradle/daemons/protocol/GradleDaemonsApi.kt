package fleet.gradle.daemons.protocol

import fleet.api.topology.ApiKey
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import kotlinx.serialization.Serializable

@Rpc
interface GradleDaemonsApi : RemoteApi<Unit> {
    companion object : ApiKey<GradleDaemonsApi> by ApiKey(remoteApiDescriptor())
    suspend fun listDaemons(includeStopped: Boolean): List<DaemonInfo>
    suspend fun stopAll(whenIdle: Boolean = false)
}

@Serializable
data class DaemonInfo(
    val pid: Long?,
    val version: String?,
    val state: DaemonState,
    val reason: String?,
    val lastBusy: Long,
    val daemonExpirationStatus: DaemonExpirationStatus,
    val daemonOpts: Collection<String>,
    val javaHome: String?,
    val idleTimeout: Int?,
    val registryDir: String?
)

@Serializable
sealed class DaemonState {
    @Serializable
    data object Idle : DaemonState()
    @Serializable
    data object Busy : DaemonState()
    @Serializable
    data object Canceled : DaemonState()
    @Serializable
    data object StopRequested : DaemonState() {
        override fun toString(): String {
            return "Stop Requested"
        }
    }
    @Serializable
    data object Stopped : DaemonState()
    @Serializable
    data object Broken : DaemonState()
    @Serializable
    data class Unknown(val state: String) : DaemonState() {
        override fun toString(): String {
            return state
        }
    }
}

@Serializable
sealed class DaemonExpirationStatus(val status: String) {
    @Serializable
    data object DoNotExpire : DaemonExpirationStatus("DO_NOT_EXPIRE")
    @Serializable
    data object QuietExpire : DaemonExpirationStatus("QUIET_EXPIRE")
    @Serializable
    data object GracefulExpire : DaemonExpirationStatus("GRACEFUL_EXPIRE")
    @Serializable
    data object ImmediateExpire : DaemonExpirationStatus("IMMEDIATE_EXPIRE")
    @Serializable
    data class Unknown(val unknownStatus: String) : DaemonExpirationStatus(unknownStatus)
}