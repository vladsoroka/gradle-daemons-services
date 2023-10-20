package fleet.gradle.daemons.frontend

import fleet.common.services.services
import fleet.common.topology.ServiceEntity
import fleet.frontend.actions.kernel
import fleet.frontend.icons.IconKeys
import fleet.frontend.layout.ToolEntity
import fleet.frontend.ui.db.durableState
import fleet.gradle.daemons.common.GradleDaemonsService
import fleet.gradle.daemons.protocol.DaemonInfo
import fleet.gradle.daemons.protocol.DaemonState
import fleet.kernel.saga
import fleet.kernel.withEntities
import fleet.rpc.client.durable
import fleet.util.UID
import fleet.util.withoutCausality
import kotlinx.coroutines.CoroutineScope
import noria.NoriaContext
import noria.foundation.background
import noria.foundation.layout.padding
import noria.foundation.shape.RoundedCornerShape
import noria.model.Trigger
import noria.readNonReactive
import noria.state
import noria.ui.Modifier
import noria.ui.components.*
import noria.ui.components.list.ListViewOptions
import noria.ui.components.list.SpeedSearchOptions
import noria.ui.components.list.defaultListCell
import noria.ui.components.list.listModel
import noria.ui.components.modifiers.constrain
import noria.ui.core.launchRestart
import noria.ui.core.theme
import noria.ui.draw.clip
import noria.ui.text.trimmedTextLineWithHighlights
import noria.ui.text.uiText
import noria.ui.theme.TextStyleKeys
import noria.ui.theme.ThemeKeys
import noria.ui.unit.DpSize
import noria.ui.unit.dp
import noria.ui.withModifier
import java.text.DateFormat
import java.util.*

internal interface DaemonsViewEntity : ToolEntity {
    override fun sharedUID(): UID = UID(DaemonsViewEntity::class.toString())
}

enum class Triggers(ident: String) {
    NewDaemonsView("gradle/new-daemons-view"),
    JumpToDaemonsView("gradle/jump-to-daemons-view");

    val trigger: Trigger = Trigger(ident)
}

internal fun NoriaContext.renderDaemonsView(daemonsViewEntity: DaemonsViewEntity) {
    val gradleDaemonsServices = services<GradleDaemonsService>()
    if (gradleDaemonsServices.isEmpty()) {
        vbox {
            hbox(modifier = Modifier.padding(8.dp)) {
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
    val showStoppedState = state { false }
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
    vbox {
        hbox(modifier = Modifier.padding(8.dp), align = Align.Center) {
            gap(width = 8.dp)
            button("Reload", iconKey = IconKeys.Reload, iconSide = IconSide.Left) {
                tick.update { it + 1 }
            }
            gap(width = 16.dp)
            button("Stop All") {
                for (gradleDaemonsService in gradleDaemonsServices) {
                    actionContext.kernel.saga(gradleDaemonsService) {
                        gradleDaemonsService.stopAll()
                        tick.update { it + 1 }
                    }
                }
            }
            gap(width = 8.dp)
            button("Stop All When Idle") {
                for (gradleDaemonsService in gradleDaemonsServices) {
                    actionContext.kernel.saga(gradleDaemonsService) {
                        gradleDaemonsService.stopAll(whenIdle = true)
                        tick.update { it + 1 }
                    }
                }
            }
            gap(width = 16.dp)
            checkbox(showStoppedState, "Show Stopped")
        }
        gap(height = 4.dp)
        val daemons = listState.read()
        if (daemons.isEmpty()) {
            hbox(modifier = Modifier.padding(8.dp)) {
                uiText(editorState.read())
            }
        } else {
            val listModel = listModel(daemons,
                options = ListViewOptions(
                    selectFirstItem = true,
                    speedSearchOptions = SpeedSearchOptions.Default(filterResults = true)
                ),
                textToMatchFn = { it.info.title() }) { item, opts ->
                defaultListCell(
                    listItemOpts = opts, cellColors = ::toolItemCellColors,
                    iconRenderer = {
                        val iconKey =
                            if (item.info.state == DaemonState.Busy) IconKeys.Plugins.Docker.Running else IconKeys.Plugins.Docker.Stopped
                        vbox(Align.Center, modifier = Modifier.constrain(preferredHeight = 12.dp)) {
                            icon(iconKey, size = DpSize(12.dp, 12.dp))
                        }
                    }
                ) {
                    trimmedTextLineWithHighlights(
                        text = item.info.title(),
                        textColor = theme[ThemeKeys.Text],
                        matcher = opts.matcher
                    )
                }
            }
            withModifier(Modifier.clip(RoundedCornerShape(6.dp))) {
                mainDetailListView(
                    listModel = listModel,
                    modifier = Modifier.background(theme[ThemeKeys.Fill]),
                    renderDetailItem = { daemon ->
                        val lastBusyDate = dateFormat.format(Date(daemon.info.lastBusy))
                        vbox(modifier = Modifier.padding(8.dp), align = Align.Stretch) {
                            vbox {
                                val preferredWidth = 120.dp
                                hbox {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("PID", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText("${daemon.info.pid}")
                                }
                                hbox {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Status", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText("${daemon.info.state}")
                                }
                                hbox {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Last busy", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    hbox {
                                        uiText(lastBusyDate)
                                        gap(width = 4.dp)
                                        uiText(
                                            "(last time the daemon was brought out of idle mode)",
                                            textStyleKey = TextStyleKeys.DefaultItalic
                                        )
                                    }
                                }
                                hbox {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Java home", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText(daemon.info.javaHome ?: "")
                                }
                                hbox {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Daemons dir", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText(daemon.info.registryDir ?: "")
                                }
                                hbox {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Idle timeout", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    uiText(daemon.info.idleTimeout?.toString() ?: "")
                                }
                                hbox {
                                    constrain(preferredWidth = preferredWidth) {
                                        uiText("Daemons options", textStyleKey = TextStyleKeys.DefaultSemiBold)
                                    }
                                    vbox {
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