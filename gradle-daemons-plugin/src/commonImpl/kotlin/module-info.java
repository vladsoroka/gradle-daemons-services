module fleet.gradle.daemons.common {
    requires fleet.rhizomedb;
    requires kotlin.stdlib;
    requires fleet.kernel;
    requires fleet.run.common;
    requires fleet.common;
    requires fleet.gradle.daemons.protocol;

    exports fleet.gradle.daemons.common;
}