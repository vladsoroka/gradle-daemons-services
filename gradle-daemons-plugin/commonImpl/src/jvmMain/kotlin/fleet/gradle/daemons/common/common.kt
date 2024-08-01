package fleet.gradle.daemons.common

import com.jetbrains.rhizomedb.Ident
import fleet.common.topology.ServiceEntity
import fleet.gradle.daemons.protocol.GradleDaemonsApi
import fleet.rpc.Rpc

@Ident("fleet.gradle.daemons.protocol.GradleDaemonsApi")
@Rpc
interface GradleDaemonsService : ServiceEntity<GradleDaemonsApi>, GradleDaemonsApi
