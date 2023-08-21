package fleet.gradle.daemons.protocol

import fleet.rpc.FleetApi

interface GradleDaemonsApi : FleetApi {
    suspend fun listDaemons() : List<String>
}