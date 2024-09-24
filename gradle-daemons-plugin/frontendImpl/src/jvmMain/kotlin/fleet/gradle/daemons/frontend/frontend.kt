package fleet.gradle.daemons.frontend

import fleet.frontend.actions.reportedActions
import fleet.frontend.icons.IconKeys
import fleet.frontend.layout.SharedLayoutId
import fleet.frontend.layout.ToolPosition
import fleet.frontend.navigation.Navigation
import fleet.frontend.navigation.setupToolClosingPolicy
import fleet.frontend.navigation.viewType
import fleet.kernel.change
import fleet.kernel.plugins.ContributionScope
import fleet.kernel.plugins.Plugin
import fleet.kernel.plugins.PluginScope
import fleet.navigation.common.api.ViewTypeId
import fleet.util.logging.KLoggers
import kotlinx.serialization.builtins.serializer

private val logger by lazy { KLoggers.logger() }

private const val GradleDaemonsViewName = "Gradle Daemons"

//private val GradleDaemonsViewTypeId = ViewTypeId<Unit>("gradle-daemons")
private val GradleDaemonsViewTypeId = ViewTypeId<Unit>("")

class DaemonsPlugin : Plugin<Unit> {
    companion object : Plugin.Key<Unit>

    override val key: Plugin.Key<Unit> = DaemonsPlugin

    override fun ContributionScope.load(pluginScope: PluginScope) {
        logger.trace { "Initialization of Gradle Daemons plugin started" }
        register(DaemonsViewEntity)
        viewType(GradleDaemonsViewTypeId, Unit.serializer()) {
            displayName = GradleDaemonsViewName
            icon = IconKeys.Gradle
            defaultPosition = ToolPosition.BottomPanel
            defaultLocation {
                defineNewAction {
                    trigger(Actions.NewDaemonsView.id)
                }
                defineJumpAction {
                    trigger(Actions.JumpToDaemonsView.id)
                }
                DefaultLocation { }
            }
            Open {
                val entity = change { DaemonsViewEntity.new {} }
                View(entity) {
                    sharedLayoutId = SharedLayoutId(DaemonsViewEntity::class.toString())
                    setupToolClosingPolicy()
                    navigationSupport {
                        Navigation {}
                    }
                    Renderer {
                        renderDaemonsView(entity, pluginScope)
                    }
                }
            }
        }
        reportedActions { Actions.values().map { it.id } }
    }
}