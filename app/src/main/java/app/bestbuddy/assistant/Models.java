package app.bestbuddy.assistant;

/** Models offered per agent. "" means the agent's own default. */
final class Models {
    static final String[][] CLAUDE = {
            {"", "Default"},
            {"claude-opus-5-5", "Opus 5.5"},
            {"claude-sonnet-5-5", "Sonnet 5.5"},
            {"claude-haiku-5-5", "Haiku 5.5"},
            {"claude-haiku-4-5-20251001", "Haiku 4.5"},
            {"claude-fable-5-1", "Fable 5.1"},
    };
    /** Used until the helper reports Codex's own model list. */
    static final String[][] CODEX = {
            {"", "Default"},
            {"gpt-6.1-sol", "GPT-6.1-Sol"},
            {"gpt-6-astra", "GPT-6-Astra"},
            {"gpt-6-luna", "GPT-6-Luna"},
    };
    /** Used until OpenCode reports its own list (`opencode models`, which includes the free ones). */
    static final String[][] OPENCODE = {
            {"", "Default"},
            {"opencode/big-pickle", "opencode/big-pickle"},
    };
    /** Gemini CLI's models (--model); Auto lets it pick the best Gemini 3 model for the task. */
    static final String[][] GEMINI = {
            {"", "Default"},
            {"gemini-3-flash-preview", "Gemini 3 Flash"},
            {"gemini-2.5-flash", "Gemini 2.5 Flash"},
            {"gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite"},
    };
    /** Used until Antigravity reports its own list (`agy models`, which depends on the plan). */
    static final String[][] AGY = {
            {"", "Default"},
            {"gemini-3.8-flash-high", "Gemini 3.8 Flash (High)"},
            {"gemini-3.8-flash-medium", "Gemini 3.8 Flash (Medium)"},
            {"gemini-3.8-flash-low", "Gemini 3.8 Flash (Low)"},
    };
    /** Used until Grok reports its own list (`grok models`). */
    static final String[][] GROK = {
            {"", "Default"},
            {"grok-4.7", "grok-4.7"},
    };
    private static String[][] codexLive, opencodeLive, agyLive, grokLive;

    private Models() {
    }

    private static long refreshedAt;

    /** Re-reads the agents' model lists (new free OpenCode models appear often) when the picker opens, at most every
     *  10 minutes; the helper's answer updates the lists. The API key services' lists too. */
    static void refresh(android.content.Context c) {
        Providers.refresh(c);
        long now = System.currentTimeMillis();
        if (now - refreshedAt < 600_000 || !Linux.installed(c)) return;
        refreshedAt = now;
        try {
            Helper.run(c, "ping");
        } catch (Exception ignored) {
        }
    }

    /** The agent's own models (its plan's, or OpenCode's), "" (its default) first. */
    static String[][] forBackend(String backend) {
        if ("opencode".equals(backend)) return withDefault(opencodeLive != null ? opencodeLive : OPENCODE);
        if ("agy".equals(backend)) return withDefault(agyLive != null ? agyLive : AGY);
        if ("grok".equals(backend)) return withDefault(grokLive != null ? grokLive : GROK);
        if ("gemini".equals(backend)) return withDefault(GEMINI);
        if (!"codex".equals(backend)) return withDefault(CLAUDE);
        return withDefault(codexLive != null ? codexLive : CODEX);
    }

    /**
     * Everything a model picker offers for this agent: Default, its plan's models (when it's signed in to a plan, or
     * OpenCode's own), then each API key service it can use, under the service's name: rows {id, name}, a heading
     * {null, service name}. Service models are "@service/model".
     */
    static java.util.List<String[]> options(String backend, boolean services) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        String[][] own = forBackend(backend);
        boolean plan = !("claude".equals(backend) || "codex".equals(backend)) || SetupState.planSignedIn(BestBuddyApp.context(), backend);
        for (String[] m : own) if (m[0].isEmpty() || plan) out.add(m);
        if (!services) return out;
        for (Providers.P p : Providers.enabled()) {
            if (!p.fits(backend)) continue;
            org.json.JSONArray models = p.offered();
            if (models.length() == 0) continue;
            out.add(new String[]{null, p.name});
            for (int i = 0; i < models.length(); i++) {
                org.json.JSONObject m = models.optJSONObject(i);
                out.add(new String[]{Providers.ref(p, m.optString("id")), m.optString("name")});
            }
        }
        return out;
    }

    /** The models an agent offers (its own list, without the "Default" row). */
    static java.util.List<String[]> agentModels(String agent) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        for (String[] m : forBackend(agent)) if (!m[0].isEmpty()) out.add(m);
        return out;
    }

    /** The models a provider offers (a built-in agent's own, or an API key service's), as {id, name}. */
    static java.util.List<String[]> providerModels(String providerId) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        String agent = Providers.agentOf(providerId);
        if (!agent.isEmpty()) return agentModels(agent);
        Providers.P p = Providers.find(Providers.serviceOf(providerId));
        if (p == null) return out;
        for (int i = 0; i < p.models.length(); i++) {
            org.json.JSONObject m = p.models.optJSONObject(i);
            if (m == null || !Providers.freeModel(p, m)) continue;
            String id = m.optString("id");
            String name = m.optString("name");
            out.add(new String[]{id, name.isEmpty() ? id : name});
        }
        return out;
    }

    /** The models of a provider the user picked (empty: all of them). */
    static java.util.Set<String> shown(android.content.Context c, String providerId) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        String agent = Providers.agentOf(providerId);
        if (!agent.isEmpty()) {
            try {
                org.json.JSONArray a = new org.json.JSONArray(Prefs.agentModels(c, agent));
                for (int i = 0; i < a.length(); i++) out.add(a.optString(i));
            } catch (Exception ignored) {
            }
            return out;
        }
        Providers.P p = Providers.find(Providers.serviceOf(providerId));
        if (p != null) out.addAll(p.shown);
        return out;
    }

    /** Saves the models of a provider the user picked (empty: all of them). */
    static void setShown(android.content.Context c, String providerId, java.util.Set<String> ids) {
        String agent = Providers.agentOf(providerId);
        org.json.JSONArray a = new org.json.JSONArray();
        for (String s : ids) a.put(s);
        if (!agent.isEmpty()) {
            Prefs.setAgentModels(c, agent, a.toString());
            return;
        }
        Providers.P p = Providers.find(Providers.serviceOf(providerId));
        if (p == null) return;
        p.shown.clear();
        p.shown.addAll(ids);
        try {
            Providers.save(c, p, null);
        } catch (Exception ignored) {
        }
    }

    /** The models of a provider the pickers show: the ones picked, or all of them. */
    static java.util.List<String[]> offered(String providerId) {
        java.util.Set<String> shown = shown(BestBuddyApp.context(), providerId);
        java.util.List<String[]> all = providerModels(providerId);
        if (shown.isEmpty()) return all;
        java.util.List<String[]> out = new java.util.ArrayList<>();
        for (String[] m : all) if (shown.contains(m[0])) out.add(m);
        return out.isEmpty() ? all : out;
    }

    /**
     * The model to try when the chat's one failed: the user's backup chain first (the one after the current), then the
     * next in the same provider's list, then the next provider's first model, then the agent's own next model.
     * "" when there is nothing to try (see ChatHub.autoSwitch).
     */
    static String nextFallback(String backend, String current, java.util.Set<String> tried) {
        // the backup models the user set, in their order
        java.util.List<String> chain = fallbacks(backend);
        int at = chain.indexOf(current);
        for (int i = at + 1; i < chain.size(); i++) {
            if (tried == null || !tried.contains(chain.get(i))) return chain.get(i);
        }
        if (at < 0) {
            for (String m : chain) {
                if (!m.equals(current) && (tried == null || !tried.contains(m))) return m;
            }
        }
        Providers.P p = Providers.of(current);
        if (p != null) { // a service's model: the next one in that service, then another service
            java.util.List<String> ids = new java.util.ArrayList<>();
            for (String[] m : providerModels(Providers.apiId(p.id))) ids.add(m[0]);
            int i = ids.indexOf(Providers.model(current));
            for (int j = i + 1; j < ids.size(); j++) {
                String ref = Providers.ref(p, ids.get(j));
                if (tried == null || !tried.contains(ref)) return ref;
            }
            for (Providers.P q : Providers.enabled()) {
                if (q.id.equals(p.id) || !q.fits(backend)) continue;
                java.util.List<String[]> m = providerModels(Providers.apiId(q.id));
                if (!m.isEmpty()) return Providers.ref(q, m.get(0)[0]);
            }
            return "";
        }
        // the agent's own model: the next one in its list ("" is its default, the first entry)
        java.util.List<String> own = new java.util.ArrayList<>();
        for (String[] m : forBackend(backend)) own.add(m[0]);
        int k = own.indexOf(current);
        for (int j = k + 1; j < own.size(); j++) {
            if (!own.get(j).isEmpty() && (tried == null || !tried.contains(own.get(j)))) return own.get(j);
        }
        return "";
    }

    /** What a chat on Default uses with this agent: "" (its plan's default), "@service/model" or one of its own models. */
    static String agentDefault(String backend) {
        String d = Prefs.agentDefault(BestBuddyApp.context(), backend);
        if (!d.startsWith("@")) {
            // A plain ID may be a legacy API-service default; never reinterpret a known paid service model as an agent model.
            String s = serviceModel(backend, d);
            if (s != null) return s;
            for (Providers.P provider : Providers.all()) if (provider.modelInfo(d) != null) return "";
            return d;
        }
        Providers.P p = Providers.of(d);
        if (p == null || !p.fits(backend) || !Providers.freeModel(p, Providers.model(d))) return "";
        String m = Providers.model(d);
        return Providers.ref(p, m.isEmpty() ? p.defaultModel() : m);
    }

    /** The service's reference for a model id it lists (e.g. "kilo-auto/free" → "@kilo/kilo-auto/free"), or null. */
    static String serviceModel(String backend, String id) {
        if (id == null || id.isEmpty() || id.startsWith("@")) return null;
        for (Providers.P p : Providers.enabled()) {
            if (!p.fits(backend)) continue;
            for (String[] m : providerModels(Providers.apiId(p.id))) {
                if (m[0].equals(id)) return Providers.ref(p, id);
            }
        }
        return null;
    }

    /** The model this agent plans with ("" = none: the chat's own model). */
    static String planModel(String backend) {
        if (!Prefs.dualModels(BestBuddyApp.context())) return "";
        return Prefs.planModel(BestBuddyApp.context(), backend);
    }

    /** The model this agent carries a task out with ("" = the chat's own model). */
    static String executeModel(String backend) {
        if (!Prefs.dualModels(BestBuddyApp.context())) return "";
        return Prefs.executeModel(BestBuddyApp.context(), backend);
    }

    /** The backup models to try when the chat's model fails, in the order the user set them. */
    static java.util.List<String> fallbacks(String backend) {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            org.json.JSONArray a = new org.json.JSONArray(Prefs.fallbacks(BestBuddyApp.context(), backend));
            for (int i = 0; i < a.length(); i++) {
                String m = a.optString(i);
                if (m.startsWith("@")) {
                    Providers.P provider = Providers.of(m);
                    if (provider == null || !provider.fits(backend)\n                            || Providers.off(BestBuddyApp.context(), Providers.apiId(provider.id))\n                            || !Providers.freeModel(provider, Providers.model(m))) continue;
                }
                if (!m.isEmpty() && !out.contains(m)) out.add(m);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /** Every model a chat of this agent can pick: the agent's own, then each API key service's. */
    static java.util.List<String[]> pickable(String backend) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        for (String[] m : agentModels(backend)) out.add(m);
        for (Providers.P p : Providers.enabled()) {
            if (!p.fits(backend)) continue;
            for (String[] m : providerModels(Providers.apiId(p.id))) {
                out.add(new String[]{Providers.ref(p, m[0]), m[0] + " · " + p.name});
            }
        }
        return out;
    }

    /** The chat's model as it will run: the one picked, or the agent's default. */
    static String effective(Sessions.Session s) {
        return s.model.isEmpty() ? agentDefault(s.backend) : s.model;
    }

    /** A service model's context size in tokens as its service said, or 0 when not known. */
    static long window(String ref) {
        Providers.P p = Providers.of(ref);
        org.json.JSONObject m = p == null ? null : p.modelInfo(Providers.model(ref));
        return m == null ? 0 : m.optLong("window");
    }

    /** The line under a model in the pickers: its id (or what Default means), with its context size when known. */
    static String detail(String backend, String id, String defaultHint) {
        if (id.isEmpty()) return defaultHint;
        long w = window(id);
        String shown = Providers.of(id) != null ? Providers.model(id) : id;
        return w > 0 ? shown + " · " + BestBuddyApp.str(R.string.models_context, tokens(w)) : shown;
    }

    /** 1048576 → "1M", 200000 → "200K", 8104 → "8.1K". */
    static String tokens(long n) {
        if (n >= 1_000_000) return trim(String.format(java.util.Locale.ROOT, "%.1f", n / 1_000_000.0)) + "M";
        if (n >= 10_000) return Math.round(n / 1000.0) + "K";
        if (n >= 1000) return trim(String.format(java.util.Locale.ROOT, "%.1f", n / 1000.0)) + "K";
        return String.valueOf(n);
    }

    private static String trim(String s) {
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    /** How full the chat's context is (percent), or -1 when its agent hasn't said. */
    static int contextPercent(Sessions.Session s) {
        if (s.ctxWindow <= 0 || s.ctxUsed <= 0) return -1;
        return (int) Math.min(100, Math.round(s.ctxUsed * 100.0 / s.ctxWindow));
    }

    /** A copy of a list with its "" row (the agent's default) labeled in the user's language; the lists keep "Default". */
    private static String[][] withDefault(String[][] list) {
        String[][] m = list.clone();
        for (int i = 0; i < m.length; i++) if (m[i][0].isEmpty()) m[i] = new String[]{"", BestBuddyApp.str(R.string.models_default)};
        return m;
    }

    /** OpenCode's models as JSON [[id, name], …] ("provider/model" for both). */
    static void setOpencode(String json) {
        String[][] parsed = parse(json);
        if (parsed == null) return;
        java.util.List<String[]> free = new java.util.ArrayList<>();
        free.add(parsed[0]);
        for (int i = 1; i < parsed.length; i++) {
            String id = parsed[i][0].toLowerCase(java.util.Locale.ROOT);
            if (id.equals("opencode/big-pickle") || id.endsWith(":free") || id.endsWith("/free")) free.add(parsed[i]);
        }
        if (free.size() > 1) opencodeLive = free.toArray(new String[0][]);
        else opencodeLive = null; // retain the known free OpenCode model if no free markers are reported
    }

    /** Antigravity's models as JSON [[id, name], …]. */
    static void setAgy(String json) {
        String[][] m = parse(json);
        if (m != null) agyLive = m;
    }

    /** Grok's models as JSON [[id, name], …] (its ids are its names). */
    static void setGrok(String json) {
        String[][] m = parse(json);
        if (m != null) grokLive = m;
    }

    /** Codex's model list as reported by the helper: JSON [[slug, name], …]. */
    static void setCodex(String json) {
        String[][] m = parse(json);
        if (m != null) codexLive = m;
    }

    private static String[][] parse(String json) {
        try {
            org.json.JSONArray a = new org.json.JSONArray(json);
            if (a.length() == 0) return null;
            String[][] m = new String[a.length() + 1][];
            m[0] = new String[]{"", "Default"};
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONArray e = a.getJSONArray(i);
                m[i + 1] = new String[]{e.getString(0), e.getString(1)};
            }
            return m;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Effort levels the chat's agent accepts ([value, label]; "" is the agent's default), or null if BestBuddy can't set it.
     * A service model that lists its levels (DeepSeek: low, high, max) keeps only those, besides Auto.
     */
    static String[][] efforts(Sessions.Session s) {
        String[][] all = agentEfforts(s.backend);
        String ref = effective(s);
        Providers.P p = Providers.of(ref);
        org.json.JSONObject m = p == null ? null : p.modelInfo(Providers.model(ref));
        org.json.JSONArray e = m == null ? null : m.optJSONArray("efforts");
        if (all == null || e == null || e.length() == 0) return all;
        java.util.Set<String> has = new java.util.HashSet<>();
        for (int j = 0; j < e.length(); j++) has.add(e.optString(j));
        java.util.List<String[]> out = new java.util.ArrayList<>();
        for (String[] x : all) if (x[0].isEmpty() || has.contains(x[0])) out.add(x);
        return out.toArray(new String[0][]);
    }

    private static String[][] agentEfforts(String backend) {
        if ("claude".equals(backend)) return new String[][]{{"", BestBuddyApp.str(R.string.models_effort_auto)},
                {"low", BestBuddyApp.str(R.string.models_effort_low)}, {"medium", BestBuddyApp.str(R.string.models_effort_medium)},
                {"high", BestBuddyApp.str(R.string.models_effort_high)}, {"xhigh", BestBuddyApp.str(R.string.models_effort_xhigh)},
                {"max", BestBuddyApp.str(R.string.models_effort_max)}};
        if ("codex".equals(backend)) return new String[][]{{"", BestBuddyApp.str(R.string.models_effort_auto)},
                {"low", BestBuddyApp.str(R.string.models_effort_low)}, {"medium", BestBuddyApp.str(R.string.models_effort_medium)},
                {"high", BestBuddyApp.str(R.string.models_effort_high)}};
        // OpenCode: effort ("variants") depends on the model; Antigravity's model ids include it; Grok's depends on the
        // model too (it lists no levels)
        return null;
    }

    /** "agent · model · effort" for a chat's chip. */
    static String summary(Sessions.Session s) {
        int ctx = contextPercent(s); // only once it matters: past half full
        return s.brainName() + " · " + shownModel(s)
                + (s.effort.isEmpty() ? "" : " · " + BestBuddyApp.str(R.string.models_chip_effort, s.effort))
                + (ctx >= 50 ? " · " + BestBuddyApp.str(R.string.models_chip_context, ctx) : "");
    }

    /** The API key service the chat's model comes from (its name), or null for the agent's own. */
    static String via(Sessions.Session s) {
        Providers.P p = Providers.of(effective(s));
        return p == null ? null : p.name;
    }

    /** The chat's model for display: the one picked, or what "Default" really is when the agent told us. */
    static String shownModel(Sessions.Session s) {
        String eff = effective(s);
        Providers.P p = Providers.of(eff);
        if (p != null) return BestBuddyApp.str(R.string.models_via, serviceModelName(p, Providers.model(eff)), p.name);
        if (!s.model.isEmpty()) return label(s.backend, s.model);
        if (!eff.isEmpty()) return label(s.backend, eff); // the default the user picked (one of the agent's own models)
        if (!s.actualModel.isEmpty()) return BestBuddyApp.str(R.string.models_name_default, nameOf(s.backend, s.actualModel));
        return BestBuddyApp.str(R.string.models_default_model);
    }

    private static String serviceModelName(Providers.P p, String model) {
        org.json.JSONObject m = p.modelInfo(model);
        return m == null || m.optString("name").isEmpty() ? model : m.optString("name");
    }

    /** What "Default" means for this chat, for the pickers. */
    static String defaultHint(Sessions.Session s) {
        String d = agentDefault(s.backend);
        Providers.P p = Providers.of(d);
        if (p != null) return BestBuddyApp.str(R.string.models_via, Providers.model(d), p.name);
        if (!d.isEmpty()) return label(s.backend, d); // the default the user picked (one of the agent's own models)
        if (!s.actualModel.isEmpty()) return BestBuddyApp.str(R.string.models_default_now, nameOf(s.backend, s.actualModel));
        return defaultHint(s.backend);
    }

    /** What the agent's own default is, before it has said. */
    static String defaultHint(String backend) {
        if ("claude".equals(backend)) return BestBuddyApp.str(R.string.models_default_claude);
        if ("codex".equals(backend)) return BestBuddyApp.str(R.string.models_default_codex);
        if ("agy".equals(backend)) return BestBuddyApp.str(R.string.models_default_agy);
        if ("grok".equals(backend)) return BestBuddyApp.str(R.string.models_default_grok);
        if ("gemini".equals(backend)) return BestBuddyApp.str(R.string.models_default_gemini);
        return BestBuddyApp.str(R.string.models_default_opencode);
    }

    /** A model id the agent reported, by its name in the model list if it's there. */
    static String nameOf(String backend, String id) {
        String l = label(backend, id);
        return l.equals(id) ? pretty(id) : l;
    }

    /** "claude-opus-5-5-20261001" → "Opus 5.5"; other ids as they are. */
    static String pretty(String id) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("claude-(opus|sonnet|haiku|fable)-(\\d+)-(\\d+)").matcher(id);
        if (m.find()) return Character.toUpperCase(m.group(1).charAt(0)) + m.group(1).substring(1) + " " + m.group(2) + "." + m.group(3);
        return id;
    }

    static String label(String backend, String model) {
        if (model == null || model.isEmpty()) return BestBuddyApp.str(R.string.models_default);
        Providers.P p = Providers.of(model);
        if (p != null) return BestBuddyApp.str(R.string.models_via, serviceModelName(p, Providers.model(model)), p.name);
        for (String[] m : forBackend(backend)) if (m[0].equals(model)) return m[1];
        return model;
    }
}
