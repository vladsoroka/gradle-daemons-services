package fleet.gradle.daemons.frontend

import fleet.frontend.actions.actions
import fleet.frontend.actions.trigger
import fleet.frontend.icons.IconKeys
import fleet.frontend.layout.*
import fleet.frontend.toolEntityRenderer
import fleet.kernel.ChangeScope
import fleet.kernel.change
import fleet.kernel.plugins.*
import fleet.util.logging.KLoggers
import noria.NoriaContext

private val logger by lazy { KLoggers.logger() }

private const val GradleDaemonsViewName = "Gradle Daemons"

class DaemonsPlugin : Plugin<API> {
    companion object : Plugin.Key<API>

    override val key: Plugin.Key<API> = DaemonsPlugin

    override fun ContributionScope.load(pluginScope: PluginScope): API = API().also {
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
        toolEntityRenderer(DaemonsViewEntity::class, ToolPosition.BottomPanel, NoriaContext::renderDaemonsView)
    }
}

private fun ChangeScope.newGradleDaemonsView(): DaemonsViewEntity {
    return new(DaemonsViewEntity::class) {
        displayName = GradleDaemonsViewName
        icon = IconKeys.Gradle
        closingPolicy = closeableClosingPolicy()
    }
}