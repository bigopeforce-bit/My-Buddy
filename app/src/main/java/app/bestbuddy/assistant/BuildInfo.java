package app.bestbuddy.assistant;

/** Single source of the app version; read by build.sh and app/build.gradle.kts. */
final class BuildInfo {
    static final String VERSION = "1.15";
    static final int VERSION_CODE = 16;

    private BuildInfo() {
    }
}
