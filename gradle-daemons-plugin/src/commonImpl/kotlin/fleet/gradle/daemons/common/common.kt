package fleet.gradle.daemons.common

import com.jetbrains.rhizomedb.Ident
import fleet.common.topology.ServiceEntity
import fleet.gradle.daemons.protocol.GradleDaemonsApi

@Ident("fleet.gradle.daemons.protocol.GradleDaemonsApi")
interface GradleDaemonsService : ServiceEntity<GradleDaemonsApi>, GradleDaemonsApi
