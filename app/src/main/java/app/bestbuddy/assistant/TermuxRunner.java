package app.bestbuddy.assistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

/** Explicit, user-initiated bridge to Termux RUN_COMMAND. */
final class TermuxRunner {
    private static final String PKG = "com.termux";
    private static final String PERM = "com.termux.permission.RUN_COMMAND";
    private static final String SERVICE = "com.termux.app.RunCommandService";
    private static final String ACTION = "com.termux.RUN_COMMAND";
    private static final String EXTRA = "com.termux.RUN_COMMAND_";

    private TermuxRunner() {}

    static void run(Context c, String command) {
        try {
            c.getPackageManager().getPackageInfo(PKG, 0);
        } catch (Exception e) {
            show(c, "Termux not installed", "Install Termux, then return to BestBuddy → Settings → Remote Control → SSH from Termux.");
            return;
        }
        if (c.checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(c)
                    .setTitle("Allow BestBuddy to run commands in Termux")
                    .setMessage("Grant Run commands in Termux environment under Android Settings → Apps → BestBuddy → Permissions → Additional permissions. Termux must also have allow-external-apps=true in ~/.termux/termux.properties.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Open BestBuddy settings", (d, w) ->
                            c.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + c.getPackageName()))))
                    .show();
            return;
        }
        Intent i = new Intent(ACTION).setClassName(PKG, SERVICE);
        i.putExtra(EXTRA + "PATH", "/data/data/com.termux/files/usr/bin/bash");
        i.putExtra(EXTRA + "ARGUMENTS", new String[]{"-lc", command});
        i.putExtra(EXTRA + "WORKDIR", "/data/data/com.termux/files/home");
        i.putExtra(EXTRA + "BACKGROUND", false);
        i.putExtra(EXTRA + "COMMAND_LABEL", "BestBuddy SSH setup");
        i.putExtra(EXTRA + "COMMAND_DESCRIPTION", "Creates or reuses a Termux SSH key and configures ssh buddy.");
        try {
            c.startService(i);
            Toast.makeText(c, "Opening Termux to run SSH setup…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            show(c, "Could not start Termux command",
                    "Check allow-external-apps=true in ~/.termux/termux.properties and grant BestBuddy the Termux run-command permission. Copy command is available as a manual fallback. " + e.getMessage());
        }
    }

    private static void show(Context c, String title, String message) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }
}
