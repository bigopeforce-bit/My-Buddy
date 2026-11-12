package app.bestbuddy.assistant;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.json.JSONObject;

/** One-input onboarding for a unified FreeLLMAPI key or a provider credential. */
final class FreeLLMAPIOnboarding {
    private FreeLLMAPIOnboarding() {}

    static String detectPlatform(String key) {
        String k = key == null ? "" : key.trim();
        if (k.startsWith("AIza")) return "google";
        if (k.startsWith("gsk_")) return "groq";
        if (k.startsWith("sk-or-v1-")) return "openrouter";
        if (k.startsWith("csk-")) return "cerebras";
        if (k.startsWith("sk-") && k.length() > 30) return "mistral";
        return "";
    }

    static boolean isUnified(String key) {
        return key != null && key.trim().startsWith("freellmapi-");
    }

    static void saveUnifiedKey(Context c, String key) throws Exception {
        Providers.P p = Providers.find("freellmapi");
        if (p == null) {
            p = new Providers.P("freellmapi");
            p.name = "FreeLLMAPI";
            p.openai = FreeLLMAPI.BASE_URL;
        }
        Providers.save(c, p, key.trim());
    }

    static String configureProvider(Context c, String rawKey, String platform) throws Exception {
        String key = rawKey == null ? "" : rawKey.trim();
        if (key.isEmpty()) throw new IllegalArgumentException("Paste an API key first.");
        if (platform == null || platform.isEmpty()) throw new IllegalArgumentException("Choose the provider for this key.");

        JSONObject status = request("GET", "/api/auth/status", null, null);
        String email = Secrets.get(c, "freellmapi_admin_email");
        String password = Secrets.get(c, "freellmapi_admin_password");
        String token;

        if (status.optBoolean("needsSetup", false)) {
            if (email == null || password == null) {
                email = "bestbuddy-" + randomHex(8) + "@localhost";
                password = randomHex(24) + "Aa9!";
                Secrets.put(c, "freellmapi_admin_email", email);
                Secrets.put(c, "freellmapi_admin_password", password);
            }
            JSONObject body = new JSONObject().put("email", email).put("password", password);
            JSONObject created = request("POST", "/api/auth/setup", body, null);
            token = created.optString("token");
        } else {
            if (email == null || password == null) {
                throw new IllegalStateException("This FreeLLMAPI installation already has a dashboard account. Sign in to its dashboard once, or reset its setup before using automatic key setup.");
            }
            JSONObject login = request("POST", "/api/auth/login",
                    new JSONObject().put("email", email).put("password", password), null);
            token = login.optString("token");
        }
        if (token.isEmpty()) throw new IllegalStateException("FreeLLMAPI did not return an admin session.");

        JSONObject added = request("POST", "/api/keys",
                new JSONObject().put("platform", platform).put("key", key).put("label", "BestBuddy · " + platform),
                token);
        if (added.has("error")) throw new IllegalStateException(added.optJSONObject("error").optString("message", "Provider key was rejected."));

        String command = "cd /root/.bestbuddy/freellmapi && set -a && . ./.env && set +a && node --input-type=module -e 'import {getUnifiedApiKey} from \"./server/dist/db/index.js\"; console.log(getUnifiedApiKey())'";
        String output = Linux.exec(c, command, 30);
        if (!output.contains("[exit 0]")) throw new IllegalStateException("Provider saved, but BestBuddy could not retrieve the local unified key: " + tail(output));
        String unified = output.substring(0, output.lastIndexOf("[exit 0]")).trim();
        if (!isUnified(unified)) throw new IllegalStateException("FreeLLMAPI returned an unexpected unified-key format.");
        saveUnifiedKey(c, unified);
        return "Provider connected. BestBuddy saved the unified FreeLLMAPI key securely.";
    }

    private static JSONObject request(String method, String path, JSONObject body, String token) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL("http://127.0.0.1:" + FreeLLMAPI.PORT + path).openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(10000);
        con.setRequestMethod(method);
        con.setRequestProperty("Accept", "application/json");
        if (token != null) con.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream out = con.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        }
        int code = con.getResponseCode();
        InputStream stream = code >= 200 && code < 400 ? con.getInputStream() : con.getErrorStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = stream) {
            byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) > 0 && bytes.size() < 65536) bytes.write(b, 0, n);
        } finally { con.disconnect(); }
        JSONObject json = new JSONObject(bytes.toString("UTF-8"));
        if (code < 200 || code >= 300) {
            JSONObject error = json.optJSONObject("error");
            throw new IllegalStateException(error == null ? "FreeLLMAPI request failed (" + code + ")." : error.optString("message", "FreeLLMAPI request failed (" + code + ")."));
        }
        return json;
    }

    private static String randomHex(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        StringBuilder s = new StringBuilder();
        for (byte v : b) s.append(String.format(java.util.Locale.ROOT, "%02x", v & 255));
        return s.toString();
    }
    private static String tail(String s) {
        String[] lines = s.split("\\r?\\n");
        StringBuilder b = new StringBuilder();
        for (int i = Math.max(0, lines.length - 8); i < lines.length; i++) b.append(lines[i]).append(' ');
        return b.toString();
    }
}
