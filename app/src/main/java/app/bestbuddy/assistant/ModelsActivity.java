package app.bestbuddy.assistant;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static app.bestbuddy.assistant.Ui.*;

/**
 * Settings → Models & providers: what the chats run on. The agent (the default brain) and its model, a plan model and
 * an execute model for the two-model mode, and the backup models a chat tries when its model fails.
 */
public class ModelsActivity extends Screen {
    private LinearLayout body;
    private String agent;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        agent = Prefs.backend(this);
        LinearLayout root = column(this);
        root.setBackgroundColor(BG);
        root.addView(topBar(), full());
        ScrollView sv = new ScrollView(this);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        body = column(this);
        body.setPaddingRelative(dp(this, 20), dp(this, 6), dp(this, 20), dp(this, 28));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        frame.setFitsSystemWindows(true);
        frame.addView(root);
        setContentView(protect(frame)); // agents never change the models themselves
    }

    @Override
    protected void onResume() {
        super.onResume();
        agent = Prefs.backend(this);
        render();
    }

    /** Back arrow, the title and what the screen is for. */
    private LinearLayout topBar() {
        LinearLayout top = row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPaddingRelative(dp(this, 4), dp(this, 8), dp(this, 16), dp(this, 6));
        ImageButton back = iconButton(this, R.drawable.ms_arrow_back, getString(R.string.perms_back));
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 44), dp(this, 44)));
        back.setOnClickListener(v -> finish());
        top.addView(back);
        LinearLayout t = column(this);
        t.setPaddingRelative(dp(this, 6), 0, 0, 0);
        t.addView(title(this, getString(R.string.models_title), 18));
        t.addView(text(this, getString(R.string.models_subtitle), 12, MUTED));
        top.addView(t, weight1());
        return top;
    }

    private void render() {
        body.removeAllViews();

        // the tile and what the screen is about
        LinearLayout head = row(this);
        head.setGravity(Gravity.TOP);
        FrameLayout ic = new FrameLayout(this);
        ic.setBackground(rounded(this, ACCENT, 16));
        ic.addView(icon(this, R.drawable.ms_key, ON_ACCENT, 26), new LinearLayout.LayoutParams(dp(this, 26), dp(this, 26), Gravity.CENTER));
        head.addView(ic, new LinearLayout.LayoutParams(dp(this, 56), dp(this, 56)));
        TextView about = title(this, getString(R.string.models_about), 16);
        about.setPaddingRelative(dp(this, 16), dp(this, 6), 0, 0);
        head.addView(about, weight1());
        body.addView(head, gap(14));

        // Manage providers
        LinearLayout manage = row(this);
        manage.setPaddingRelative(dp(this, 16), dp(this, 12), dp(this, 12), dp(this, 12));
        manage.setBackground(outlined(this, CARD, 16, OUTLINE, 1));
        manage.addView(icon(this, R.drawable.ms_key, MUTED, 22));
        LinearLayout mt = column(this);
        mt.setPaddingRelative(dp(this, 14), 0, dp(this, 8), 0);
        mt.addView(title(this, getString(R.string.models_manage_providers), 15));
        mt.addView(single(monoText(this, getString(R.string.models_manage_providers_info), 12, MUTED)));
        manage.addView(mt, weight1());
        manage.addView(icon(this, R.drawable.ms_chevron_right, MUTED, 22));
        manage.setOnClickListener(v -> startActivity(new android.content.Intent(this, ProvidersActivity.class)));
        body.addView(manage, gap(8));

        // Separate plan and execute models
        LinearLayout dual = column(this);
        dual.setPaddingRelative(dp(this, 16), dp(this, 14), dp(this, 16), dp(this, 14));
        dual.setBackground(outlined(this, CARD, 16, OUTLINE, 1));
        dual.addView(title(this, getString(R.string.models_dual_title), 15));
        dual.addView(text(this, getString(R.string.models_dual_info), 13, MUTED), gap(6));
        dual.addView(modelRow(getString(R.string.models_plan_model), Prefs.planModel(this, agent),
                id -> {
                    Prefs.setPlanModel(this, agent, id);
                    render();
                }), gap(10));
        dual.addView(modelRow(getString(R.string.models_execute_model), Prefs.executeModel(this, agent),
                id -> {
                    Prefs.setExecuteModel(this, agent, id);
                    render();
                }), gap(4));
        dual.addView(switchRow(this, getString(R.string.models_dual_switch),
                getString(R.string.models_dual_switch_info), Prefs.dualModels(this), on -> {
                    Prefs.setDualModels(this, on);
                    render();
                }), gap(6));
        body.addView(dual, gap(10));

        // Current configuration
        LinearLayout now = column(this);
        now.setPaddingRelative(dp(this, 16), dp(this, 14), dp(this, 16), dp(this, 14));
        now.setBackground(outlined(this, CARD, 16, OUTLINE, 1));
        now.addView(title(this, getString(R.string.models_now_title), 15));
        String def = Models.agentDefault(agent);
        now.addView(configRow(getString(R.string.models_now_model), modelName(def), true), gap(4));
        now.addView(configRow(getString(R.string.models_now_thinking), getString(R.string.models_thinking_off)), gap(4));
        now.addView(configRow(getString(R.string.models_now_window), window(def)), gap(4));
        now.addView(configRow(getString(R.string.models_now_permission),
                getString(Prefs.devMode(this) ? R.string.models_permission_full : R.string.models_permission_ask)), gap(10));
        now.addView(text(this, getString(R.string.models_now_hint), 12, MUTED));
        body.addView(now, gap(10));

        // Fallbacks
        LinearLayout fb = column(this);
        fb.setPaddingRelative(dp(this, 16), dp(this, 14), dp(this, 16), dp(this, 14));
        fb.setBackground(outlined(this, CARD, 16, OUTLINE, 1));
        fb.addView(title(this, getString(R.string.models_fallbacks_title), 15));
        List<String> chain = Models.fallbacks(agent);
        fb.addView(text(this, chain.isEmpty() ? getString(R.string.models_fallbacks_none)
                : getString(R.string.models_fallbacks_count, chain.size()), 13, MUTED), gap(6));
        Button set = button(this, getString(R.string.models_fallbacks_set), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(this, 46));
        sp.topMargin = dp(this, 6);
        fb.addView(set, sp);
        set.setOnClickListener(v -> fallbackSheet());
        body.addView(fb, gap(10));
    }

    /** A row with a name on the left and the model on the right, with a chevron. */
    private LinearLayout modelRow(String title, String model, Ui.Pick pick) {
        LinearLayout r = row(this);
        r.setPaddingRelative(0, dp(this, 8), 0, dp(this, 8));
        r.setBackgroundResource(ripple(this, false));
        r.addView(text(this, title, 15, TEXT), weight1());
        TextView v = single(monoText(this, modelName(model), 13, MUTED));
        v.setPaddingRelative(dp(this, 10), 0, dp(this, 10), 0);
        r.addView(v, wrap());
        r.addView(icon(this, R.drawable.ms_chevron_right, MUTED, 22));
        r.setOnClickListener(v2 -> picker(title, model, pick));
        return r;
    }

    /** A row of the current configuration: a name on the left, the value on the right. */
    private LinearLayout configRow(String title, String value) {
        return configRow(title, value, false);
    }

    /** A row of the current configuration: a name on the left, the value on the right. */
    private LinearLayout configRow(String title, String value, boolean clickable) {
        LinearLayout r = row(this);
        r.setPaddingRelative(0, dp(this, 6), 0, dp(this, 6));
        TextView t = text(this, title, 14, TEXT);
        r.addView(t, weight1());
        TextView v = single(monoText(this, value, 13, MUTED));
        v.setPaddingRelative(dp(this, 10), 0, 0, 0);
        r.addView(v, wrap());
        if (clickable) {
            r.setBackgroundResource(ripple(this, false));
            r.setOnClickListener(v2 -> picker(getString(R.string.models_now_model), Models.agentDefault(agent), id -> {
                Prefs.setAgentDefault(this, agent, id);
                ChatHub.get(this).changed();
                render();
            }));
        }
        return r;
    }

    /** The model's name as it is shown: its own name, or "model · service". */
    private String modelName(String model) {
        if (model == null || model.isEmpty()) return getString(R.string.models_own_default);
        Providers.P p = Providers.of(model);
        if (p != null) return Providers.model(model) + " · " + p.name;
        for (String[] m : Models.agentModels(agent)) if (m[0].equals(model)) return m[1];
        return model;
    }

    /** The model's context size, when it is known. */
    private String window(String model) {
        long w = model == null || model.isEmpty() ? 0 : Models.window(model);
        if (w <= 0) {
            // the agent's own models: the widest one it reports
            for (String[] m : Models.agentModels(agent)) {
                long x = Models.window(m[0]);
                if (x > w) w = x;
            }
        }
        return w > 0 ? Models.tokens(w) : "—";
    }

    /**
     * The models to pick from: the agent's own, then each API key service's. The first row is the agent's own default.
     */
    private void picker(String title, String current, Ui.Pick pick) {
        LinearLayout c = column(this);
        final Dialog[] d = new Dialog[1];
        c.addView(title(this, title, 18));
        c.addView(text(this, getString(R.string.models_picker_info), 13, MUTED), gap(8));
        EditText find = field(getString(R.string.providers_search_provider), false);
        c.addView(find, gap(10));
        LinearLayout list = column(this);
        c.addView(list, gap(4));
        Runnable fill = () -> {
            list.removeAllViews();
            String q = find.getText().toString().trim().toLowerCase(Locale.ROOT);
            // the agent's own default
            LinearLayout none = row(this);
            none.setPaddingRelative(dp(this, 4), dp(this, 10), dp(this, 4), dp(this, 10));
            none.setBackgroundResource(ripple(this, false));
            none.addView(radio(this, current == null || current.isEmpty()));
            LinearLayout nt = column(this);
            nt.setPaddingRelative(dp(this, 14), 0, 0, 0);
            nt.addView(title(this, getString(R.string.models_own_default), 15));
            nt.addView(single(monoText(this, getString(R.string.models_own_default_info), 12, MUTED)));
            none.addView(nt, weight1());
            none.setOnClickListener(v -> {
                d[0].dismiss();
                pick.onPick("");
            });
            list.addView(none, full());
            for (String[] m : Models.pickable(agent)) {
                String id = m[0], name = m[1];
                if (!q.isEmpty() && !id.toLowerCase(Locale.ROOT).contains(q)
                        && !name.toLowerCase(Locale.ROOT).contains(q)) continue;
                LinearLayout r = row(this);
                r.setPaddingRelative(dp(this, 4), dp(this, 10), dp(this, 4), dp(this, 10));
                r.setBackgroundResource(ripple(this, false));
                r.addView(radio(this, id.equals(current)));
                LinearLayout t = column(this);
                t.setPaddingRelative(dp(this, 14), 0, 0, 0);
                t.addView(single(title(this, name, 15)));
                t.addView(single(monoText(this, id, 11, MUTED)));
                r.addView(t, weight1());
                r.setOnClickListener(v -> {
                    d[0].dismiss();
                    pick.onPick(id);
                });
                list.addView(r, full());
            }
        };
        fill.run();
        find.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int n) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int n) {
            }

            public void afterTextChanged(Editable e) {
                fill.run();
            }
        });
        // a model id typed in: one the agent knows but does not list yet (a new free model, another provider's)
        LinearLayout other = row(this);
        other.setPaddingRelative(0, dp(this, 10), 0, 0);
        final EditText et = field(getString(R.string.models_type_id_hint), false);
        other.addView(et, weight1());
        Button use = button(this, getString(R.string.models_type_id_use), true);
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(this, 44));
        up.setMarginStart(dp(this, 8));
        other.addView(use, up);
        c.addView(other, full());
        use.setOnClickListener(v -> {
            String id = et.getText().toString().trim();
            if (id.isEmpty()) return;
            d[0].dismiss();
            pick.onPick(id);
        });
        d[0] = bottomSheet(this, protect(c));
        d[0].show();
    }

    /** The backup models: a chain the chat goes through when its own model fails, in the order they are set. */
    private void fallbackSheet() {
        LinearLayout c = column(this);
        final Dialog[] d = new Dialog[1];
        c.addView(title(this, getString(R.string.models_fallbacks_title), 18));
        c.addView(text(this, getString(R.string.models_fallbacks_info), 13, MUTED), gap(8));
        EditText find = field(getString(R.string.providers_search_provider), false);
        c.addView(find, gap(10));
        LinearLayout list = column(this);
        c.addView(list, gap(4));
        final Set<String> chain = new LinkedHashSet<>(Models.fallbacks(agent));
        final Runnable[] fill = new Runnable[1];
        fill[0] = () -> {
            list.removeAllViews();
            String q = find.getText().toString().trim().toLowerCase(Locale.ROOT);
            int n = 0;
            for (String[] m : Models.pickable(agent)) {
                String id = m[0], name = m[1];
                if (!q.isEmpty() && !id.toLowerCase(Locale.ROOT).contains(q)
                        && !name.toLowerCase(Locale.ROOT).contains(q)) continue;
                n++;
                LinearLayout r = row(this);
                r.setGravity(Gravity.CENTER_VERTICAL);
                r.setPaddingRelative(dp(this, 4), dp(this, 8), dp(this, 4), dp(this, 8));
                r.setBackgroundResource(ripple(this, false));
                CheckBox cb = new CheckBox(this);
                cb.setChecked(chain.contains(id));
                cb.setClickable(false);
                r.addView(cb);
                LinearLayout t = column(this);
                t.setPaddingRelative(dp(this, 8), 0, 0, 0);
                t.addView(single(title(this, name, 15)));
                t.addView(single(monoText(this, id, 11, MUTED)));
                r.addView(t, weight1());
                r.setOnClickListener(v -> {
                    if (!chain.remove(id)) chain.add(id); // the order they are picked in is the order they are tried
                    fill[0].run();
                });
                list.addView(r, full());
            }
            if (n == 0) list.addView(text(this, getString(R.string.models_fallbacks_empty), 13, MUTED), gap(8));
        };
        fill[0].run();
        find.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int n) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int n) {
            }

            public void afterTextChanged(Editable e) {
                fill[0].run();
            }
        });
        Button save = button(this, getString(R.string.providers_save_selection), true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(this, 52));
        sp.topMargin = dp(this, 14);
        c.addView(save, sp);
        save.setOnClickListener(v -> {
            org.json.JSONArray a = new org.json.JSONArray();
            for (String id : chain) a.put(id);
            Prefs.setFallbacks(this, agent, a.toString());
            d[0].dismiss();
            render();
        });
        d[0] = bottomSheet(this, protect(c));
        d[0].show();
    }

    private EditText field(String hint, boolean secret) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(MUTED);
        et.setTextColor(TEXT);
        et.setTypeface(sans(this, 400));
        et.setTextSize(15);
        et.setSingleLine(true);
        if (secret) et.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        et.setPaddingRelative(dp(this, 14), dp(this, 12), dp(this, 14), dp(this, 12));
        et.setBackground(outlined(this, BG, 12, OUTLINE, 1));
        return et;
    }

    private LinearLayout.LayoutParams gap(int top) {
        LinearLayout.LayoutParams p = full();
        p.topMargin = dp(this, top);
        return p;
    }
}
