package app.bestbuddy.assistant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import static app.bestbuddy.assistant.Ui.*;

/** Setup and controls for the local FreeLLMAPI router running in BestBuddy's Alpine Linux. */
public class FreeLLMAPIActivity extends Screen {
    private TextView status;
    private Button install, dashboard, addProvider, stop;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = column(this);
        root.setBackgroundColor(BG);
        LinearLayout head = row(this);
        head.setPaddingRelative(dp(this, 16), dp(this, 14), dp(this, 16), dp(this, 12));
        Button back = button(this, "Back", false);
        back.setOnClickListener(v -> finish());
        head.addView(back);
        head.addView(title(this, "FreeLLMAPI", 22), weight1());
        root.addView(head, full());

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = column(this);
        body.setPaddingRelative(dp(this, 20), dp(this, 8), dp(this, 20), dp(this, 28));
        body.addView(title(this, "One API key. Free-model routing.", 20));
        body.addView(text(this, "BestBuddy runs FreeLLMAPI inside its private Alpine Linux environment. The local dashboard manages upstream provider credentials, model availability and fallback order. BestBuddy then connects through one unified FreeLLMAPI key.", 14, MUTED), gap(10));
        status = text(this, "", 14, OK);
        status.setPaddingRelative(dp(this, 14), dp(this, 12), dp(this, 14), dp(this, 12));
        status.setBackground(rounded(this, CARD2, 12));
        body.addView(status, gap(14));

        install = button(this, "Install / Start FreeLLMAPI", true);
        body.addView(install, gap(10));
        dashboard = button(this, "Open local dashboard", false);
        body.addView(dashboard, gap(8));
        addProvider = button(this, "Add FreeLLMAPI to BestBuddy providers", false);
        body.addView(addProvider, gap(8));
        stop = button(this, "Stop local server", false);
        body.addView(stop, gap(8));
        Button logs = button(this, "View setup log", false);
        body.addView(logs, gap(8));
        body.addView(title(this, "First-time setup", 17), gap(22));
        body.addView(text(this, "1. Install FreeLLMAPI. BestBuddy installs Node.js, npm and the required build tools inside Alpine, then builds and starts the server.\n\n2. Open the dashboard at http://127.0.0.1:3001 and complete its first-run account setup.\n\n3. Add only free-tier upstream provider credentials on the FreeLLMAPI Keys page and arrange the fallback chain. Provider credentials stay in FreeLLMAPI's encrypted database.\n\n4. Copy the unified freellmapi-… API key from its dashboard. In BestBuddy, add the FreeLLMAPI preset in API keys and paste that one key. Use model auto or a listed model.\n\nThe BestBuddy API endpoint is http://127.0.0.1:3001/v1. Existing BestBuddy providers remain available.", 13, MUTED), gap(8));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(protect(root));

        install.setOnClickListener(v -> install());
        dashboard.setOnClickListener(v -> openDashboard());
        addProvider.setOnClickListener(v -> ApiKeysActivity.addProvider(this));
        stop.setOnClickListener(v -> {
            FreeLLMAPI.stop(this);
            refresh();
            Toast.makeText(this, "FreeLLMAPI stopped", Toast.LENGTH_SHORT).show();
        });
        logs.setOnClickListener(v -> {
            String value = FreeLLMAPI.logs(this);
            android.widget.ScrollView sv = new android.widget.ScrollView(this);
            TextView t = monoText(this, value, 11, TEXT);
            t.setTextIsSelectable(true);
            sv.addView(t);
            LinearLayout c = column(this);
            c.addView(title(this, "FreeLLMAPI setup log", 17));
            c.addView(sv, new LinearLayout.LayoutParams(-1, dp(this, 420)));
            bottomSheet(this, protect(c)).show();
        });
        refresh();
    }

    private void install() {
        install.setEnabled(false);
        status.setText("Installing FreeLLMAPI. This may take several minutes and requires an internet connection.");
        FreeLLMAPI.installAndStart(this, (message, finished, ok) -> runOnUiThread(() -> {
            status.setText(message);
            if (finished) {
                install.setEnabled(true);
                refresh();
                Toast.makeText(this, ok ? "FreeLLMAPI ready" : "FreeLLMAPI setup failed", Toast.LENGTH_LONG).show();
            }
        }));
    }

    private void openDashboard() {
        if (!FreeLLMAPI.running()) {
            Toast.makeText(this, "Start FreeLLMAPI first", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:3001")));
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        boolean installed = FreeLLMAPI.installed(this);
        boolean running = FreeLLMAPI.running();
        status.setText(running ? "Running · http://127.0.0.1:3001" : installed ? "Installed · server stopped" :
                Linux.installed(this) ? "Not installed · ready to set up" : "BestBuddy Linux is not installed yet.");
        dashboard.setEnabled(running);
        addProvider.setEnabled(running);
        stop.setEnabled(running);
        install.setEnabled(!installing());
    }

    private boolean installing() { return false; }
}
