package app.bestbuddy.assistant;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * FreeLLMAPI runtime inside BestBuddy's existing Alpine PRoot. Provider credentials and the
 * FreeLLMAPI database remain in the private Linux root; BestBuddy only stores its client key
 * through the existing encrypted Providers/Secrets path.
 */
final class FreeLLMAPI {
    static final int PORT = 3001;
    static final String BASE_URL = "http://127.0.0.1:" + PORT + "/v1";
    private static final String DIR = "/root/.bestbuddy/freellmapi";
    private static final String LOG = "/root/.bestbuddy/freellmapi-runtime.log";
    private static volatile Process process;
    private static volatile boolean installing;

    interface Listener { void onProgress(String message, boolean finished, boolean ok); }

    private FreeLLMAPI() {}

    static boolean installed(Context c) {
        if (!Linux.installed(c)) return false;
        try {
            String out = Linux.exec(c, "test -f " + DIR + "/server/dist/index.js && test -f " + DIR + "/.env", 10);
            return out.contains("[exit 0]");
        } catch (Exception e) { return false; }
    }

    static boolean running() {
        if (process != null && process.isAlive()) return true;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", PORT), 350);
            return true;
        } catch (Exception e) { return false; }
    }

    static synchronized void ensureStarted(Context c) {
        if (!installed(c) || running()) return;
        start(c);
    }

    static void installAndStart(Context context, Listener listener) {
        final Context app = context.getApplicationContext();
        synchronized (FreeLLMAPI.class) {
            if (installing) {
                if (listener != null) listener.onProgress("FreeLLMAPI setup is already running.", false, true);
                return;
            }
            installing = true;
        }
        Prefs.setFreeLLMAPIEnabled(app, true);
        new Thread(() -> {
            boolean ok = false;
            String message = "";
            try {
                if (!Linux.installed(app)) throw new IllegalStateException("Install BestBuddy Linux first from Settings.");
                if (!installed(app)) {
                    report(listener, "Installing Node.js, npm and build tools in Alpine…", false, false);
                    String setup = "set -e; apk add --no-cache nodejs npm git python3 make g++; " +
                            "mkdir -p /root/.bestbuddy; " +
                            "if [ ! -d " + DIR + "/.git ]; then git clone --depth 1 https://github.com/tashfeenahmed/freellmapi.git " + DIR + "; " +
                            "else git -C " + DIR + " pull --ff-only; fi; " +
                            "cd " + DIR + "; npm install --include=optional --no-audit --no-fund; " +
                            "npm run build; mkdir -p /root/.config/FreeLLMAPI; " +
                            "if [ ! -f .env ]; then " +
                            "KEY=$(node -e \"process.stdout.write(require('crypto').randomBytes(32).toString('hex'))\"); " +
                            "umask 077; printf 'ENCRYPTION_KEY=%s\\nPORT=3001\\nHOST=127.0.0.1\\nNODE_ENV=production\\nFREEAPI_DB_PATH=/root/.config/FreeLLMAPI/freeapi.db\\nREQUEST_ANALYTICS_RETENTION_DAYS=14\\nREQUEST_ANALYTICS_MAX_ROWS=5000\\n' \"$KEY\" > .env; fi; " +
                            "chmod 600 .env; ";
                    String output = Linux.exec(app, setup, 1800);
                    if (!output.contains("[exit 0]")) throw new IllegalStateException(lastLines(output, 16));
                }
                report(listener, "Starting the local FreeLLMAPI server…", false, false);
                start(app);
                long until = System.currentTimeMillis() + 25_000;
                while (System.currentTimeMillis() < until && !running()) Thread.sleep(500);
                if (!running()) throw new IllegalStateException("The server did not open port 3001. Open the setup log for details.");
                ok = true;
                message = "FreeLLMAPI is running at http://127.0.0.1:3001";
            } catch (Exception e) {
                message = e.getMessage() == null ? e.toString() : e.getMessage();
            } finally {
                synchronized (FreeLLMAPI.class) { installing = false; }
            }
            report(listener, message, true, ok);
        }, "freellmapi-install").start();
    }

    private static synchronized void start(Context c) {
        if (running()) return;
        try {
            String command = "cd " + DIR + " && set -a && . ./.env && set +a && exec node server/dist/index.js";
            Process p = Linux.builder(c, "/bin/sh", "-c", command).start();
            process = p;
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
                     FileOutputStream log = new FileOutputStream(new File(Linux.root(c), "root/.bestbuddy/freellmapi-runtime.log"), true)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                        log.flush();
                    }
                } catch (Exception ignored) {}
            }, "freellmapi-log").start();
        } catch (Exception e) {
            throw new IllegalStateException("Could not start FreeLLMAPI: " + e.getMessage(), e);
        }
    }

    static synchronized void stop(Context c) {
        Prefs.setFreeLLMAPIEnabled(c, false);
        Process p = process;
        process = null;
        if (p != null) p.destroy();
    }

    static String logs(Context c) {
        try { return Linux.exec(c, "tail -n 100 " + LOG + " 2>/dev/null || true", 10); }
        catch (Exception e) { return e.toString(); }
    }

    private static void report(Listener l, String message, boolean done, boolean ok) {
        if (l != null) l.onProgress(message, done, ok);
    }

    private static String lastLines(String s, int count) {
        String[] lines = s.split("\\r?\\n");
        StringBuilder b = new StringBuilder();
        for (int i = Math.max(0, lines.length - count); i < lines.length; i++) b.append(lines[i]).append('\n');
        return b.toString();
    }
}
