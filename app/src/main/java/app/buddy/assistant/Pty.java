package app.buddy.assistant;

/**
 * The terminal's pseudo-terminal (native/pty.c, libbuddypty.so).
 *
 * <p>The native library exports its symbols as {@code Java_app_buddy_assistant_Pty_*}: the package BestBuddy had
 * before it was renamed to {@code app.bestbuddy.assistant}. JNI looks a native method up by the name of the class that
 * declares it, so the declaration lives here, under the name the library still uses. Renaming the symbols in pty.c
 * would mean rebuilding the library (native/build.sh needs an Android NDK); keeping the declaration here means both
 * the prebuilt library and one built from source work.
 */
public final class Pty {
    static {
        System.loadLibrary("buddypty");
    }

    private Pty() {
    }

    /** Starts {@code path} on a new pty; returns {masterFd, pid}. */
    public static native int[] start(String path, String[] args, String[] env, String cwd, int rows, int cols)
            throws java.io.IOException;

    public static native void resize(int fd, int rows, int cols);

    /** Blocks until the program exits; returns its exit code. */
    public static native int waitFor(int pid);

    public static native void hangup(int pid);

    public static native void close(int fd);
}
