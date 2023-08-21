package fleet.gradle.daemons.frontend

import com.jetbrains.rhizomedb.Entrypoint
import fleet.common.services.services
import fleet.frontend.actions.actions
import fleet.frontend.actions.sagaAction
import fleet.gradle.daemons.common.GradleDaemonsService
import fleet.kernel.ChangeScope
import fleet.kernel.plugins.register
import fleet.plugins.smartMode.frontend.smartModeAction
import fleet.util.logging.KLoggers
import noria.model.ActionPresentation
import noria.model.Trigger

private val logger by lazy { KLoggers.logger() }

@Entrypoint
fun ChangeScope.gradleDaemons() {
    logger.warn("Initialization of Gradle Daemons plugin started")
    register {
        actions(
            smartModeAction(
                defaultPresentation = ActionPresentation("Show Daemons"),
                perform = sagaAction {
                    services<GradleDaemonsService>().forEach {
                        logger.warn { it.listDaemons() }
                    }
                },
                identifier = "Show Daemons Fleet action",
                triggers = setOf(SHOW_DAEMONS_TRIGGER)
            )
        )
    }
}

private val SHOW_DAEMONS_TRIGGER = Trigger("show-gradle-daemons-view")