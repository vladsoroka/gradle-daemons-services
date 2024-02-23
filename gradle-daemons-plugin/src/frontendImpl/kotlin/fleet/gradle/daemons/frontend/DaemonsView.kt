package fleet.gradle.daemons.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import fleet.common.services.services
import fleet.common.topology.ServiceEntity
import fleet.compose.theme.components.Icon
import fleet.frontend.actions.performSagaAction
import fleet.frontend.icons.IconKeys
import fleet.frontend.layout.SharedLayoutId
import fleet.frontend.layout.ToolEntity
import fleet.frontend.ui.db.durableState
import fleet.gradle.daemons.common.GradleDaemonsService
import fleet.gradle.daemons.protocol.DaemonInfo
import fleet.gradle.daemons.protocol.DaemonState
import fleet.kernel.withEntities
import fleet.rpc.client.durable
import fleet.util.withoutCausality
import kotlinx.coroutines.CoroutineScope
import noria.NoriaContext
import noria.model.Trigger
import noria.readNonReactive
import noria.state
import noria.ui.components.*
import noria.ui.components.list.ListViewOptions
import noria.ui.components.list.SpeedSearchOptions
import noria.ui.components.list.defaultListCell
import noria.ui.components.list.listModel
import noria.ui.components.modifiers.constrain
import noria.ui.core.launchRestart
import noria.ui.core.theme
import noria.ui.text.uiText
import noria.ui.theme.keys.TextStyleKeys
import noria.ui.theme.keys.ThemeKeys
import noria.ui.withModifier
import java.text.DateFormat
import java.util.*

internal interface DaemonsViewEntity : ToolEntity {
    override fun sharedUID(): SharedLayoutId = SharedLayoutId(DaemonsViewEntity::class.toString())
}

enum class Triggers(ident: String) {
    NewDaemonsView("gradle/new-daemons-view"),
    JumpToDaemonsView("gradle/jump-to-daemons-view");

    val trigger: Trigger = Trigger(ident)
}

@Composable
internal fun NoriaContext.renderDaemonsView(daemonsViewEntity: DaemonsViewEntity) {
    val gradleDaemonsServices = services<GradleDaemonsService>()
    if (gradleDaemonsServices.isEmpty()) {
        Column {
            Row(modifier = Modifier.padding(8.dp)) {
                uiText("Gradle daemons information is not available")
            }
        }
        return
    }

    val editorState = durableState(daemonsViewEntity, "gradleDaemonsView#${daemonsViewEntity.eid}") {
        "Loading data..."
    }
    val listState = durableState(daemonsViewEntity, "daemonsList#${daemonsViewEntity.eid}") {
        emptyList<DaemonItem>()
    }
    val tick = state { 0 }
    val showStoppedState = durableState(daemonsViewEntity, "daemonsList#show_stopped") { false }
    launchRestart(tick.read(), showStoppedState.read()) {
        val daemonInfos = mutableListOf<DaemonInfo>()
        gradleDaemonsServices.forEach { daemonsService ->
            withEntities(daemonsService, daemonsViewEntity) {
                try {
                    editorState.update { "Loading daemons info ..." }
                    listState.update { emptyList() }
                    daemonInfos.addAll(rpcCall(daemonsService) { it.listDaemons(showStoppedState.readNonReactive()) })
                    listState.update { daemonInfos.map { DaemonItem(it, daemonsService) } }
                    val statusText = if (daemonInfos.isEmpty()) "Gradle daemons not found" else ""
                    editorState.update { statusText }
                } catch (t: Throwable) {
                    editorState.update { t.stackTraceToString() }
                    listState.update { emptyList() }
                }
            }
        }
    }
    Column {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.Center) {
            Spacer(Modifier.width(8.dp))
            button("Reload", iconKey = IconKeys.Reload, iconSide = IconSide.Left) {
                tick.update { it + 1 }
            }
            Spacer(Modifier.width(16.dp))
            button("Stop All") {
                for (gradleDaemonsService in gradleDaemonsServices) {
                    performSagaAction(actionContext) {
                        withEntities(gradleDaemonsService) {
                            gradleDaemonsService.stopAll()
                            tick.update { it + 1 }
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            button("Stop All When Idle") {
                for (gradleDaemonsService in gradleDaemonsServices) {
                    performSagaAction(actionContext) {
                        withEntities(gradleDaemonsService) {
                            gradleDaemonsService.stopAll(whenIdle = true)
                            tick.update { it + 1 }
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            checkbox(showStoppedState, "Show Stopped")
        }
        Spacer(Modifier.height(4.dp))
        val daemons = listState.read()
        if (daemons.isEmpty()) {
            Row(modifier = Modifier.padding(8.dp)) {
                uiText(editorState.read())
            }
        } else {
            val listModel = listModel(daemons,
                options = ListViewOptions(
                    selectFirstItem = true,
                    speedSearchOptions = SpeedSearchOptions.Default(filterResults = true)
                ),
                textToMatchFn = { it.info.title() },
                renderFn = { item, opts ->
                    defaultListCell(
                        item.info.title(),
                        listItemOpts = opts,
                        cellColors = { toolItemCellColors(it) },
                        iconRenderer = {
                            val iconKey =
                                if (item.info.state == DaemonState.Busy) IconKeys.Plugins.Docker.Running else IconKeys.Plugins.Docker.Stopped
                            Column(modifier = Modifier.constrain(preferredHeight = 12.dp), verticalArrangement = Arrangement.Center) {
                                Icon(iconKey, size = DpSize(12.dp, 12.dp))
                            }
                        }
                    )
                })
            withModifier(Modifier.clip(RoundedCornerShape(6.dp))) {
                mainDetailListView(
                    listModel = listModel,
                    modifier = Modifier.background(theme[ThemeKeys.Fill]),
                    renderDetailItem = { daemon ->
                        val lastBusyDate = dateFormat.format(Date(daemon.info.lastBusy))
                        Column (modifier = Modifier.padding(8.dp)) {
                            Column {
                                val preferredWidth = 120.dp
                                Row {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("PID", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText("${daemon.info.pid}")
                                }
                                Row {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Status", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText("${daemon.info.state}")
                                }
                                Row {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Last busy", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    Row {
                                        uiText(lastBusyDate)
                                        Spacer(Modifier.width(4.dp))
                                        uiText(
                                            "(last time the daemon was brought out of idle mode)",
                                            textStyleKey = TextStyleKeys.DefaultItalic
                                        )
                                    }
                                }
                                Row {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Java home", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText(daemon.info.javaHome ?: "")
                                }
                                Row {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Daemons dir", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText(daemon.info.registryDir ?: "")
                                }
                                Row {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Idle timeout", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText(daemon.info.idleTimeout?.toString() ?: "")
                                }
                                Row {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Daemons options", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    Column {
                                        for (opt in daemon.info.daemonOpts) {
                                            uiText(opt)
                                        }
                                    }
                                }
                            }
                        }
                    })
            }
        }
    }
}

class DaemonItem(val info: DaemonInfo, service: GradleDaemonsService)

private suspend fun <T : ServiceEntity<*>, K> rpcCall(service: T, call: suspend CoroutineScope.(T) -> K): K {
    return withEntities(service) {
        durable {
            withoutCausality {
                call(service)
            }
        }
    }
}

private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
private fun DaemonInfo.title(): String {
    val versionOrEmpty = version ?: ""
    return "$pid $state ${versionOrEmpty}, ${timeFormat.format(Date(lastBusy))}"
}