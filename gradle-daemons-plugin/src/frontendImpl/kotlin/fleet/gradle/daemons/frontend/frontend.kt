package fleet.gradle.daemons.frontend

import fleet.frontend.actions.actions
import fleet.frontend.actions.trigger
import fleet.frontend.icons.IconKeys
import fleet.frontend.layout.*
import fleet.frontend.toolEntityRenderer
import com.jetbrains.rhizomedb.ChangeScope
import fleet.frontend.actions.reportedActions
import fleet.kernel.change
import fleet.kernel.plugins.*
import fleet.util.logging.KLoggers
import noria.NoriaContext

private val logger by lazy { KLoggers.logger() }

private const val GradleDaemonsViewName = "Gradle Daemons"

class DaemonsPlugin : Plugin<Unit> {
    companion object : Plugin.Key<Unit>

    override val key: Plugin.Key<Unit> = DaemonsPlugin

    override fun ContributionScope.load(pluginScope: PluginScope) {
        logger.warn("Initialization of Gradle Daemons plugin started")
        trigger(Triggers.NewDaemonsView.trigger, true)
        trigger(Triggers.JumpToDaemonsView.trigger, true)
        actions(
                *toolActions(
                        toolName = GradleDaemonsViewName,
                        entityType = DaemonsViewEntity::class,
                        icon = IconKeys.Gradle,
                        newIdentifier = "tools/gradle/daemons/new",
                        jumpToIdentifier = "tools/gradle/daeons/jump-to",
                        newTriggers = setOf(Triggers.NewDaemonsView.trigger),
                        jumpToTriggers = setOf(Triggers.JumpToDaemonsView.trigger)
                ) { actionContext ->
                    change {
                        openTool(newGradleDaemonsView(), actionContext)
                    }
                }.asArray()
        )
        toolEntityRenderer(DaemonsViewEntity::class, ToolPosition.BottomPanel) { renderDaemonsView(it) }

        reportedActions { Triggers.values().map { it.trigger.ident } }
    }
}

private fun ChangeScope.newGradleDaemonsView(): DaemonsViewEntity {
    return new(DaemonsViewEntity::class) {
        displayName = GradleDaemonsViewName
        icon = IconKeys.Gradle
        closingPolicy = closeableClosingPolicy()
    }
}