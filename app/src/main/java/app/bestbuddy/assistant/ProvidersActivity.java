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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static app.bestbuddy.assistant.Ui.*;

/**
 * Settings → Providers: what powers the chats. The built-in agents (OpenCode, Gemini CLI, Codex, Antigravity, Grok)
 * and the API key services (Settings → API keys) are providers here: each one can be turned off, each has its own
 * list of models, and the user picks which of those models appear in the chat's model picker. A global proxy sends
 * every request to a provider through one address, and a chat whose model fails can try another one by itself.
 */
public class ProvidersActivity extends Screen {
    /** One provider in the list: a built-in agent or an API key service. */
    private static final class Row {
        final String id;          // "h:opencode" or "p:openrouter"
        final String name;
        final boolean builtin;
        final String sub;         // "Built-in · 12/12 models · Active"
        final List<String[]> models;

        Row(String id, String name, boolean builtin, String sub, List<String[]> models) {
            this.id = id;
            this.name = name;
            this.builtin = builtin;
            this.sub = sub;
            this.models = models;
        }
    }

    private LinearLayout body;
    private LinearLayout list;
    private LinearLayout bottom;
    private EditText search;
    private boolean showDisabled, searchModels;
    private boolean selecting, building;
    private String query = "";
    private final Set<String> selected = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = column(this);
        root.setBackgroundColor(BG);
        root.addView(topBar(), full());

        // "Global proxy" on the left, "Select providers" (or "Done") on the right
        LinearLayout tools = row(this);
        tools.setPaddingRelative(dp(this, 20), dp(this, 4), dp(this, 20), dp(this, 4));
        TextView proxy = text(this, getString(R.string.providers_global_proxy), 15, ACCENT_TEXT);
        proxy.setTypeface(sans(this, 600));
        proxy.setOnClickListener(v -> proxySheet());
        tools.addView(proxy, weight1());
        TextView select = text(this, getString(R.string.providers_select), 15, ACCENT_TEXT);
        select.setTypeface(sans(this, 600));
        select.setOnClickListener(v -> {
            selecting = !selecting;
            if (!selecting) selected.clear();
            render();
        });
        tools.addView(select);
        root.addView(tools, full());

        ScrollView sv = new ScrollView(this);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        body = column(this);
        body.setPaddingRelative(dp(this, 20), dp(this, 6), dp(this, 20), dp(this, 24));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // the selection's actions (Enable, Disable, Delete), only while selecting
        bottom = column(this);
        bottom.setVisibility(View.GONE);
        root.addView(bottom, full());

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        frame.setFitsSystemWindows(true);
        frame.addView(root);
        setContentView(protect(frame)); // agents never see or change the providers, even when they may tap BestBuddy's screens
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        t.addView(title(this, getString(R.string.providers_title), 18));
        t.addView(text(this, getString(R.string.providers_subtitle) + " · Free models only · Automatic context-limit switching", 12, MUTED));
        top.addView(t, weight1());
        return top;
    }

    private void render() {
        building = true;
        body.removeAllViews();
        // "+ Add provider"
        Button add = button(this, getString(R.string.providers_add), true);
        add.setBackground(rounded(this, CARD2, 16));
        add.setTextColor(TEXT);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(this, 52));
        ap.topMargin = dp(this, 6);
        body.addView(add, ap);
        add.setOnClickListener(v -> ApiKeysActivity.addProvider(this));

        // the search box
        search = field(getString(R.string.providers_search_hint), false);
        search.setText(query);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(this, 52));
        sp.topMargin = dp(this, 14);
        body.addView(search, sp);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int n) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int n) {
            }

            public void afterTextChanged(Editable e) {
                query = e.toString();
                renderList();
            }
        });

        // "Show disabled" and "Search all models"
        LinearLayout chips = row(this);
        chips.setPaddingRelative(0, dp(this, 10), 0, 0);
        chips.addView(disabledChip(), wrap());
        TextView allModels = text(this, getString(R.string.providers_search_all_models), 14,
                searchModels ? ACCENT_TEXT : MUTED);
        allModels.setTypeface(sans(this, searchModels ? 600 : 400));
        allModels.setPaddingRelative(dp(this, 16), dp(this, 8), dp(this, 4), dp(this, 8));
        allModels.setOnClickListener(v -> {
            searchModels = !searchModels;
            render();
        });
        chips.addView(allModels, wrap());
        body.addView(chips, full());
        body.addView(text(this, getString(R.string.providers_disabled_hint), 12, MUTED), gap(10));

        // the providers themselves
        building = false;
        list = column(this);
        body.addView(list, gap(6));
        renderList();

        // when a model's gone, the chat tries another one by itself
        body.addView(space(26), full());
        body.addView(switchRow(this, getString(R.string.providers_auto_switch),
                getString(R.string.providers_auto_switch_info), Prefs.autoSwitch(this), on -> {
                    Prefs.setAutoSwitch(this, on);
                    ChatHub.get(this).changed();
                }), full());
        body.addView(text(this, getString(R.string.providers_footer), 12, MUTED), gap(14));
        building = false;
        renderBottom();
    }

    /** The providers themselves. */
    private void renderList() {
        if (list == null || building) return;
        list.removeAllViews();
        boolean any = false;
        for (Row r : rows()) {
            if (!matches(r)) continue;
            any = true;
            list.addView(card(r), full());
            list.addView(space(4), full());
        }
        if (!any) list.addView(text(this, getString(R.string.providers_none), 14, MUTED), gap(12));
    }

    /** The providers to show: the built-in agents and the API key services. */
    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        for (Account a : Account.OFFERED) {
            String id = Providers.builtinId(a.agent);
            List<String[]> models = Models.providerModels(id);
            int shown = Models.shown(this, id).size();
            out.add(new Row(id, a.name(), true, getString(R.string.providers_builtin) + " · "
                    + getString(R.string.providers_models_count, shown == 0 ? models.size() : shown, models.size())
                    + " · " + (Providers.off(this, id) ? getString(R.string.providers_disabled)
                    : getString(R.string.providers_active)), models));
        }
        for (Providers.P p : Providers.all()) {
            String id = Providers.apiId(p.id);
            List<String[]> models = Models.providerModels(id);
            if (models.isEmpty()) continue; // don't offer services with no verified free model
            int shown = Models.shown(this, id).size();
            String sub = getString(R.string.providers_api) + " · " + p.host() + " · "
                    + getString(R.string.providers_models_count, shown == 0 ? models.size() : shown, models.size())
                    + " · " + (Providers.off(this, id) ? getString(R.string.providers_disabled)
                    : getString(R.string.providers_active));
            out.add(new Row(id, p.name, false, sub, models));
        }
        return out;
    }

    /** Does this provider match the search? Disabled ones stay searchable, as the screen says. */
    private boolean matches(Row r) {
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return showDisabled || !Providers.off(this, r.id);
        if (r.name.toLowerCase(Locale.ROOT).contains(q) || r.sub.toLowerCase(Locale.ROOT).contains(q)) return true;
        if (searchModels) {
            for (String[] m : r.models) {
                if (m[0].toLowerCase(Locale.ROOT).contains(q) || m[1].toLowerCase(Locale.ROOT).contains(q)) return true;
            }
        }
        return false;
    }

    /** One provider's card: its name, what it is, and its switch and models link. */
    private LinearLayout card(Row r) {
        boolean off = Providers.off(this, r.id);
        LinearLayout c = column(this);
        c.setPaddingRelative(dp(this, 14), dp(this, 12), dp(this, 10), dp(this, 12));
        c.setBackground(outlined(this, CARD, 16, OUTLINE, 1));
        LinearLayout top = row(this);
        top.setGravity(Gravity.TOP);
        if (r.builtin) top.addView(agentTile(this, Providers.agentOf(r.id)));
        else top.addView(icon(this, R.drawable.ms_key, MUTED, 22));
        LinearLayout t = column(this);
        t.setPaddingRelative(dp(this, 12), 0, dp(this, 8), 0);
        t.addView(title(this, r.name, 15));
        t.addView(single(monoText(this, r.sub, 12, off ? MUTED : ACCENT_TEXT)));
        top.addView(t, weight1());
        if (selecting) {
            CheckBox cb = new CheckBox(this);
            cb.setChecked(selected.contains(r.id));
            cb.setClickable(false);
            top.addView(cb);
        } else {
            ImageButton more = iconButton(this, R.drawable.ms_more_vert, getString(R.string.providers_more));
            more.setOnClickListener(v -> menu(r, more));
            top.addView(more);
        }
        c.addView(top, full());

        // the switch and the models link, under the name
        LinearLayout row = row(this);
        row.setPaddingRelative(dp(this, 4), dp(this, 8), dp(this, 4), 0);
        LinearLayout sw = row(this);
        sw.setGravity(Gravity.CENTER_VERTICAL);
        sw.addView(toggle(this, !off, on -> {
            Providers.setOff(this, r.id, !on);
            render();
        }));
        TextView label = text(this, off ? getString(R.string.providers_disabled) : getString(R.string.providers_enabled),
                14, off ? MUTED : TEXT);
        label.setPaddingRelative(dp(this, 10), 0, 0, 0);
        sw.addView(label);
        row.addView(sw, weight1());
        TextView manage = text(this, getString(R.string.providers_manage_models), 14, ACCENT_TEXT);
        manage.setTypeface(sans(this, 600));
        manage.setPaddingRelative(dp(this, 10), dp(this, 6), dp(this, 6), dp(this, 6));
        manage.setOnClickListener(v -> manageModels(r));
        row.addView(manage, wrap());
        c.addView(row, full());

        c.setOnClickListener(v -> {
            if (!selecting) return;
            if (!selected.remove(r.id)) selected.add(r.id);
            render();
        });
        c.setOnLongClickListener(v -> {
            if (selecting) return false;
            selecting = true;
            selected.add(r.id);
            render();
            return true;
        });
        return c;
    }

    /** The card's menu: manage its models, turn it off or on, edit or remove an API key service. */
    private void menu(Row r, View anchor) {
        android.widget.PopupMenu pm = new android.widget.PopupMenu(
                new android.view.ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_DayNight), anchor);
        pm.getMenu().add(getString(R.string.providers_manage_models));
        pm.getMenu().add(getString(Providers.off(this, r.id) ? R.string.providers_enable : R.string.providers_disable));
        if (!r.builtin) {
            pm.getMenu().add(getString(R.string.providers_edit));
            pm.getMenu().add(getString(R.string.providers_delete));
        }
        pm.setOnMenuItemClickListener(it -> {
            String title = String.valueOf(it.getTitle());
            if (title.equals(getString(R.string.providers_manage_models))) {
                manageModels(r);
            } else if (title.equals(getString(R.string.providers_enable))) {
                Providers.setOff(this, r.id, false);
                render();
            } else if (title.equals(getString(R.string.providers_disable))) {
                Providers.setOff(this, r.id, true);
                render();
            } else if (title.equals(getString(R.string.providers_edit))) {
                ApiKeysActivity.editProvider(this, Providers.serviceOf(r.id));
            } else if (title.equals(getString(R.string.providers_delete))) {
                confirmRemove(r);
            }
            return true;
        });
        pm.show();
    }

    /** Which of the provider's models the chat's picker shows: a search box, a tick per model, and a way back. */
    private void manageModels(Row r) {
        LinearLayout c = column(this);
        final Dialog[] d = new Dialog[1];
        c.addView(title(this, getString(R.string.providers_manage_title), 18));
        c.addView(text(this, getString(R.string.providers_manage_subtitle), 13, MUTED), gap(4));
        // the provider's own chip, as the screen's header
        LinearLayout chip = row(this);
        chip.setPaddingRelative(dp(this, 12), dp(this, 6), dp(this, 12), dp(this, 6));
        chip.setBackground(rounded(this, CARD2, 10));
        chip.addView(single(text(this, r.name, 13, TEXT)));
        c.addView(chip, gap(10));

        EditText find = field(getString(R.string.providers_search_provider), false);
        c.addView(find, gap(10));

        final Set<String> picked = new LinkedHashSet<>(Models.shown(this, r.id));
        if (picked.isEmpty()) for (String[] m : r.models) picked.add(m[0]);
        final boolean[] changed = {false};
        TextView count = text(this, "", 13, MUTED);
        TextView clear = text(this, "", 13, ACCENT_TEXT);
        clear.setTypeface(sans(this, 600));
        LinearLayout head = row(this);
        head.addView(count, weight1());
        head.addView(clear, wrap());
        c.addView(head, gap(8));

        TextView refresh = text(this, getString(R.string.providers_refresh_models), 13, ACCENT_TEXT);
        refresh.setTypeface(sans(this, 600));
        refresh.setPaddingRelative(0, dp(this, 4), 0, dp(this, 8));
        c.addView(refresh, gap(4));
        refresh.setOnClickListener(v -> refreshModels(r, () -> {
            d[0].dismiss();
            manageModels(r);
        }));

        LinearLayout models = column(this);
        c.addView(models, gap(4));
        final Runnable[] fill = new Runnable[1];
        fill[0] = () -> {
            models.removeAllViews();
            String q = find.getText().toString().trim().toLowerCase(Locale.ROOT);
            int shown = 0, total = 0;
            for (String[] m : r.models) {
                if (!q.isEmpty() && !m[0].toLowerCase(Locale.ROOT).contains(q)
                        && !m[1].toLowerCase(Locale.ROOT).contains(q)) continue;
                total++;
                if (picked.contains(m[0])) shown++;
                LinearLayout row = row(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPaddingRelative(dp(this, 4), dp(this, 8), dp(this, 4), dp(this, 8));
                row.setBackgroundResource(ripple(this, false));
                CheckBox cb = new CheckBox(this);
                cb.setChecked(picked.contains(m[0]));
                cb.setClickable(false);
                row.addView(cb);
                LinearLayout t = column(this);
                t.setPaddingRelative(dp(this, 8), 0, 0, 0);
                t.addView(single(title(this, m[1], 15)));
                t.addView(single(monoText(this, m[0], 11, MUTED)));
                row.addView(t, weight1());
                row.setOnClickListener(v -> {
                    if (!picked.remove(m[0])) picked.add(m[0]);
                    changed[0] = true;
                    cb.setChecked(picked.contains(m[0]));
                    fill[0].run();
                });
                models.addView(row, full());
            }
            count.setText(getString(R.string.providers_selected_count, shown, total));
            clear.setText(getString(R.string.providers_clear_results, total));
            clear.setVisibility(total == 0 ? View.GONE : View.VISIBLE);
        };
        fill[0].run();
        clear.setOnClickListener(v -> {
            picked.clear();
            changed[0] = true;
            fill[0].run();
        });
        find.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int n) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int n) {
            }

            public void afterTextChanged(Editable e) {
                fill[0].run();
            }
        });

        c.addView(text(this, getString(R.string.providers_manage_footer), 12, MUTED), gap(10));
        Button save = button(this, getString(R.string.providers_save_selection), true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(this, 52));
        slp.topMargin = dp(this, 14);
        c.addView(save, slp);
        save.setOnClickListener(v -> {
            if (changed[0]) Models.setShown(this, r.id, picked);
            d[0].dismiss();
            render();
        });
        d[0] = bottomSheet(this, protect(c));
        d[0].show();
    }

    /** Asks the service (or the agent) for its models again. */
    private void refreshModels(Row r, Runnable then) {
        Toast.makeText(this, R.string.providers_refreshing, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                if (r.builtin) {
                    String agent = Providers.agentOf(r.id);
                    Account.of(agent).refresh(this);
                    try {
                        Thread.sleep(3000); // the agent's list comes a moment later
                    } catch (InterruptedException ignored) {
                    }
                    Account.of(agent).refresh(this);
                } else {
                    Providers.P p = Providers.find(Providers.serviceOf(r.id));
                    if (p != null) Providers.fetch(this, p);
                }
            } catch (Exception ignored) {
            }
            runOnUiThread(then);
        }, "provider-models").start();
    }

    /** Removes an API key service: its key goes too, and chats that used it go back to Default. */
    private void confirmRemove(Row r) {
        confirmSheet(this, getString(R.string.providers_remove_title, r.name), getString(R.string.providers_remove_text),
                getString(R.string.providers_delete), true, () -> {
                    String id = Providers.serviceOf(r.id);
                    Providers.remove(this, id);
                    forgetChats(id);
                    render();
                });
    }

    /** Chats that picked a removed service's model go back to Default. */
    private void forgetChats(String id) {
        ChatHub hub = ChatHub.get(this);
        for (Sessions.Session s : new ArrayList<>(hub.sessions.all)) {
            if (id.equals(Providers.idOf(s.model))) hub.setModel(s, "");
        }
    }

    /** The selection's actions: turn the picked providers on or off, or delete the API key services among them. */
    private void renderBottom() {
        bottom.removeAllViews();
        bottom.setVisibility(selecting ? View.VISIBLE : View.GONE);
        if (!selecting) return;
        View line = new View(this);
        line.setBackgroundColor(LINE);
        bottom.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(this, 1)));
        LinearLayout row = row(this);
        row.setPaddingRelative(dp(this, 20), dp(this, 12), dp(this, 20), dp(this, 12));
        TextView n = text(this, getString(R.string.providers_selected, selected.size()), 14, MUTED);
        row.addView(n, weight1());
        Button on = button(this, getString(R.string.providers_enable), false);
        on.setOnClickListener(v -> setSelected(false));
        row.addView(on, wrap());
        Button off = button(this, getString(R.string.providers_disable), false);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(this, 44));
        olp.setMarginStart(dp(this, 8));
        row.addView(off, olp);
        Button del = button(this, getString(R.string.providers_delete), false);
        del.setTextColor(ERR);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(this, 44));
        dlp.setMarginStart(dp(this, 8));
        row.addView(del, dlp);
        // only API key services can be deleted
        boolean anyApi = false;
        for (String id : selected) if (Providers.agentOf(id).isEmpty()) anyApi = true;
        del.setEnabled(anyApi);
        del.setAlpha(anyApi ? 1 : 0.4f);
        del.setOnClickListener(v -> {
            for (String id : new ArrayList<>(selected)) {
                if (!Providers.agentOf(id).isEmpty()) continue;
                Providers.remove(this, Providers.serviceOf(id));
                forgetChats(Providers.serviceOf(id));
            }
            selected.clear();
            selecting = false;
            render();
        });
        bottom.addView(row, full());
    }

    private void setSelected(boolean off) {
        for (String id : selected) Providers.setOff(this, id, off);
        selected.clear();
        selecting = false;
        render();
    }

    /** The proxy every request to a provider goes through. */
    private void proxySheet() {
        LinearLayout c = column(this);
        final Dialog[] d = new Dialog[1];
        c.addView(title(this, getString(R.string.providers_proxy_title), 18));
        c.addView(text(this, getString(R.string.providers_proxy_info), 13, MUTED), gap(8));
        EditText host = field(getString(R.string.providers_proxy_hint), false);
        host.setText(Prefs.proxy(this));
        c.addView(host, gap(12));
        LinearLayout btns = row(this);
        Button clear = button(this, getString(R.string.providers_proxy_clear), false);
        btns.addView(clear, weight1());
        Button save = button(this, getString(R.string.providers_proxy_save), true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(this, 48), 1);
        sp.setMarginStart(dp(this, 10));
        btns.addView(save, sp);
        c.addView(btns, gap(10));
        d[0] = bottomSheet(this, protect(c));
        clear.setOnClickListener(v -> {
            Prefs.setProxy(this, "");
            saveProxy("");
            d[0].dismiss();
            render();
        });
        save.setOnClickListener(v -> {
            String addr = host.getText().toString().trim();
            if (!addr.isEmpty() && !addr.matches("[A-Za-z0-9._-]+(:[0-9]{1,5})?")) {
                Toast.makeText(this, R.string.providers_proxy_bad, Toast.LENGTH_SHORT).show();
                return;
            }
            Prefs.setProxy(this, addr);
            saveProxy(addr);
            d[0].dismiss();
            render();
        });
        d[0].show();
    }

    /** The proxy in BestBuddy's Linux too, so the agents' own requests go through it (see buddy-run). */
    private void saveProxy(String value) {
        try {
            Linux.write(new java.io.File(Linux.root(this), "root/.buddy/proxy"), value);
        } catch (Exception ignored) {
        }
    }

    /** An outlined chip that reads as pressed or not (the "Show disabled" one). */
    private LinearLayout disabledChip() {
        LinearLayout l = row(this);
        l.setPaddingRelative(dp(this, 12), dp(this, 5), dp(this, 12), dp(this, 5));
        l.setBackground(showDisabled ? rounded(this, ACCENT_SOFT, 16) : outlined(this, 0x00000000, 16, OUTLINE, 1));
        TextView t = text(this, getString(R.string.providers_show_disabled), 13, showDisabled ? ACCENT_TEXT : MUTED);
        t.setTypeface(sans(this, showDisabled ? 600 : 400));
        l.addView(t);
        l.setOnClickListener(v -> {
            showDisabled = !showDisabled;
            render();
        });
        return l;
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

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(this, dp)));
        return v;
    }

    private LinearLayout.LayoutParams gap(int top) {
        LinearLayout.LayoutParams p = full();
        p.topMargin = dp(this, top);
        return p;
    }
}
