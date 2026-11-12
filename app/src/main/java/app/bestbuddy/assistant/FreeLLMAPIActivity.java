package app.bestbuddy.assistant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import static app.bestbuddy.assistant.Ui.*;

/** One-input FreeLLMAPI setup and local runtime controls. */
public class FreeLLMAPIActivity extends Screen {
    private TextView status;
    private Button install, connect, dashboard, stop;
    private EditText keyInput;
    private Spinner providerChoice;
    private static final String[] PROVIDER_NAMES = {
        "Auto-detect provider", "Google AI Studio", "Groq", "OpenRouter", "Cerebras", "Mistral", "OpenCode Zen", "LongCat", "Pollinations"
    };
    private static final String[] PROVIDER_IDS = {"", "google", "groq", "openrouter", "cerebras", "mistral", "opencode", "longcat", "pollinations"};

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
        body.addView(title(this, "Connect your AI key", 20));
        body.addView(text(this, "Paste either a FreeLLMAPI unified key or an upstream provider API key. BestBuddy installs and starts the local gateway automatically, then saves the unified key in Android Keystore-backed storage.", 14, MUTED), gap(10));
        status = text(this, "", 14, OK);
        status.setPaddingRelative(dp(this, 14), dp(this, 12), dp(this, 14), dp(this, 12));
        status.setBackground(rounded(this, CARD2, 12));
        body.addView(status, gap(14));

        keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setHint("Paste API key here");
        keyInput.setTextColor(TEXT);
        keyInput.setHintTextColor(MUTED);
        keyInput.setPadding(dp(this, 14), dp(this, 12), dp(this, 14), dp(this, 12));
        keyInput.setBackground(rounded(this, CARD, 12));
        body.addView(keyInput, gap(10));

        providerChoice = new Spinner(this);
        providerChoice.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, PROVIDER_NAMES));
        body.addView(providerChoice, gap(8));
        connect = button(this, "Connect key automatically", true);
        body.addView(connect, gap(10));

        install = button(this, "Install / Start FreeLLMAPI", false);
        body.addView(install, gap(8));
        dashboard = button(this, "Open local dashboard", false);
        body.addView(dashboard, gap(8));
        stop = button(this, "Stop local server", false);
        body.addView(stop, gap(8));
        Button logs = button(this, "View setup log", false);
        body.addView(logs, gap(8));
        body.addView(text(this, "For a provider key, BestBuddy creates the gateway's local admin account on first setup, stores your provider credential in FreeLLMAPI's encrypted database, retrieves its unified key locally, and saves that key to BestBuddy. If the provider prefix is recognized, it is selected automatically; otherwise choose the provider above. A pre-existing dashboard account created outside BestBuddy must be signed in/reset once before automatic provisioning can manage it.", 13, MUTED), gap(14));
        body.addView(text(this, "The built-in provider list is limited to providers with free-tier access options. Free quotas and eligibility are controlled by each provider and can change; this does not guarantee unlimited free usage. Avoid adding paid credentials if you want to prevent paid routing.", 13, MUTED), gap(8));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(protect(root));

        connect.setOnClickListener(v -> connectKey());
        install.setOnClickListener(v -> install());
        dashboard.setOnClickListener(v -> openDashboard());
        stop.setOnClickListener(v -> { FreeLLMAPI.stop(this); refresh(); });
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

    private String selectedPlatform(String key) {
        String detected = FreeLLMAPIOnboarding.detectPlatform(key);
        if (!detected.isEmpty()) return detected;
        int selected = providerChoice.getSelectedItemPosition();
        return selected >= 0 && selected < PROVIDER_IDS.length ? PROVIDER_IDS[selected] : "";
    }

    private void connectKey() {
        String key = keyInput.getText().toString().trim();
        final String platform = selectedPlatform(key);
        if (key.isEmpty()) { keyInput.setError("Paste an API key"); return; }
        if (!FreeLLMAPIOnboarding.isUnified(key) && platform.isEmpty()) {
            Toast.makeText(this, "Choose the provider for this key", Toast.LENGTH_LONG).show();
            return;
        }
        connect.setEnabled(false);
        status.setText("Preparing FreeLLMAPI and configuring your key…");
        Runnable configure = () -> new Thread(() -> {
            try {
                String message;
                if (FreeLLMAPIOnboarding.isUnified(key)) {
                    FreeLLMAPIOnboarding.saveUnifiedKey(this, key);
                    message = "Unified key saved securely. FreeLLMAPI is connected to BestBuddy.";
                } else {
                    message = FreeLLMAPIOnboarding.configureProvider(this, key, platform);
                }
                final String result = message;
                runOnUiThread(() -> {
                    status.setText(result);
                    keyInput.setText("");
                    connect.setEnabled(true);
                    Toast.makeText(this, "FreeLLMAPI connected", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                final String error = e.getMessage() == null ? e.toString() : e.getMessage();
                runOnUiThread(() -> { status.setText(error); connect.setEnabled(true); });
            }
        }, "freellmapi-key-setup").start();

        if (!FreeLLMAPI.running()) {
            FreeLLMAPI.installAndStart(this, (message, finished, ok) -> runOnUiThread(() -> {
                status.setText(message);
                if (finished) {
                    if (ok) configure.run();
                    else connect.setEnabled(true);
                }
            }));
        } else configure.run();
    }

    private void install() {
        install.setEnabled(false);
        status.setText("Installing FreeLLMAPI. This may take several minutes.");
        FreeLLMAPI.installAndStart(this, (message, finished, ok) -> runOnUiThread(() -> {
            status.setText(message);
            if (finished) { install.setEnabled(true); refresh(); }
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
        if (status != null && status.getText().length() == 0) {
            status.setText(running ? "Running · http://127.0.0.1:3001" : installed ? "Installed · server stopped" :
                    Linux.installed(this) ? "Ready to install" : "Install BestBuddy Linux first.");
        }
        if (dashboard != null) dashboard.setEnabled(running);
        if (stop != null) stop.setEnabled(running);
        if (install != null) install.setEnabled(!FreeLLMAPI.installing());
    }
}
