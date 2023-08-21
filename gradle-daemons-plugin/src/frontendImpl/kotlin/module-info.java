module fleet.gradle.daemons.frontend {
    requires fleet.frontend;
    requires fleet.kernel;
    requires fleet.noria.ui;
    requires fleet.rhizomedb;
    requires fleet.frontend.ui;
    requires fleet.util.logging.api;
    requires fleet.plugins.smartMode.frontend;
    requires fleet.gradle.daemons.common;

    exports fleet.gradle.daemons.frontend;
}