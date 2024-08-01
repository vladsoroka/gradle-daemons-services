module fleet.gradle.daemons.frontend {
    requires fleet.frontend;
    requires fleet.kernel;
    requires fleet.noria.ui;
    requires fleet.rhizomedb;
    requires fleet.frontend.ui;
    requires fleet.util.logging.api;
    requires fleet.gradle.daemons.common;
    requires kotlinx.coroutines.core;
    requires fleet.gradle.daemons.protocol;
    requires fleet.noria.awt;
    requires fleet.navigation.common.api;

    exports fleet.gradle.daemons.frontend;

    provides fleet.kernel.plugins.Plugin with fleet.gradle.daemons.frontend.DaemonsPlugin;
}