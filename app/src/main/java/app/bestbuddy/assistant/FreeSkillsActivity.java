package app.bestbuddy.assistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.util.List;
import static app.bestbuddy.assistant.Ui.*;

/** Browse free, curated public skill stores; install into the shared agent skills folder or save SKILL.md. */
public final class FreeSkillsActivity extends Screen {
    private static final int CREATE_MD = 71;
    private LinearLayout body;
    private String store = FreeSkillsCatalog.ALL;
    private FreeSkillsCatalog.Item pendingDownload;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = column(this);
        root.setBackgroundColor(BG);
        LinearLayout top = row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPaddingRelative(dp(this, 4), dp(this, 4), dp(this, 16), dp(this, 4));
        ImageButton back = iconButton(this, R.drawable.ms_arrow_back, "Back");
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 44), dp(this, 44)));
        back.setOnClickListener(v -> finish());
        top.addView(back);
        top.addView(title(this, "Free Skill Stores", 18), weight1());
        root.addView(top, full());

        ScrollView scroll = new ScrollView(this);
        body = column(this);
        body.setPaddingRelative(dp(this, 16), dp(this, 8), dp(this, 16), dp(this, 28));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        FrameLayout frame = new FrameLayout(this);
        frame.addView(root);
        setContentView(protect(frame));
        render();
    }

    private void render() {
        body.removeAllViews();
        body.addView(text(this, "Free, open-source skills in the standard SKILL.md format. Download a Markdown file or install it directly into BestBuddy's shared agent skills folder.", 13, MUTED), full());
        body.addView(text(this, "Review a skill before installing it. Installed skills are instructions for your AI agents.", 12, MUTED), gap(6));

        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = row(this);
        tabs.setPaddingRelative(0, dp(this, 12), 0, dp(this, 4));
        for (String s : FreeSkillsCatalog.stores()) {
            Button tab = button(this, s, s.equals(store));
            tab.setTextSize(11);
            tab.setMinHeight(dp(this, 38));
            tab.setMinimumHeight(dp(this, 38));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, dp(this, 38));
            tp.setMarginEnd(dp(this, 6));
            tabs.addView(tab, tp);
            tab.setOnClickListener(v -> { store = s; render(); });
        }
        tabsScroll.addView(tabs);
        body.addView(tabsScroll, full());

        List<FreeSkillsCatalog.Item> items = FreeSkillsCatalog.inStore(store);
        for (int i = 0; i < items.size(); i++) body.addView(skillCard(items.get(i)), gap(i == 0 ? 10 : 8));
        body.addView(text(this, "Sources: GitHub repositories • MIT and Apache-2.0 licensed collections • Free to browse", 11, MUTED), gap(18));
    }

    private LinearLayout skillCard(FreeSkillsCatalog.Item item) {
        LinearLayout card = column(this);
        card.setPaddingRelative(dp(this, 14), dp(this, 13), dp(this, 14), dp(this, 13));
        card.setBackground(outlined(this, CARD, 16, OUTLINE, 1));
        card.addView(title(this, item.title, 15));
        TextView desc = text(this, item.description, 12, MUTED);
        desc.setPaddingRelative(0, dp(this, 3), 0, 0);
        card.addView(desc, full());
        card.addView(text(this, item.store + "  •  " + item.license + "  •  " + item.source, 10, MUTED), gap(7));

        LinearLayout actions = row(this);
        Button download = button(this, "Download .MD", false);
        download.setTextSize(12);
        download.setMinHeight(dp(this, 40));
        download.setMinimumHeight(dp(this, 40));
        Button install = button(this, Skills.find(this, item.name) == null ? "Install" : "Update", true);
        install.setTextSize(12);
        install.setMinHeight(dp(this, 40));
        install.setMinimumHeight(dp(this, 40));
        actions.addView(download, new LinearLayout.LayoutParams(0, dp(this, 42), 1));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(this, 42), 1);
        ip.setMarginStart(dp(this, 8));
        actions.addView(install, ip);
        download.setOnClickListener(v -> saveMarkdown(item));
        install.setOnClickListener(v -> installSkill(item));
        card.addView(actions, gap(10));
        return card;
    }

    private void saveMarkdown(FreeSkillsCatalog.Item item) {
        pendingDownload = item;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/markdown");
        i.putExtra(Intent.EXTRA_TITLE, item.name + "-SKILL.md");
        try { startActivityForResult(i, CREATE_MD); }
        catch (Exception e) { Toast.makeText(this, "No file picker is available.", Toast.LENGTH_LONG).show(); }
    }

    private void installSkill(FreeSkillsCatalog.Item item) {
        if (!Linux.installed(this)) {
            Toast.makeText(this, "Set up BestBuddy Linux before installing skills. You can still download the .MD file.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Downloading " + item.title + "…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String message;
            try {
                String md = FreeSkillsCatalog.fetch(item);
                String name = Skills.installMarkdown(this, item.name, md);
                message = "Installed /" + name + ". Your agents can use it now.";
            } catch (Exception e) {
                message = e.getMessage() == null ? "Skill installation failed." : e.getMessage();
            }
            final String result = message;
            runOnUiThread(() -> { Toast.makeText(this, result, Toast.LENGTH_LONG).show(); render(); });
        }, "free-skill-install").start();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != CREATE_MD || result != Activity.RESULT_OK || data == null || data.getData() == null || pendingDownload == null) return;
        Uri uri = data.getData();
        FreeSkillsCatalog.Item item = pendingDownload;
        pendingDownload = null;
        Toast.makeText(this, "Downloading " + item.title + "…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String message;
            try {
                String md = FreeSkillsCatalog.fetch(item);
                try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                    if (out == null) throw new IllegalStateException("Could not open the selected file.");
                    out.write(md.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.flush();
                }
                message = "Saved " + item.name + "-SKILL.md";
            } catch (Exception e) {
                message = e.getMessage() == null ? "Markdown download failed." : e.getMessage();
            }
            final String resultText = message;
            runOnUiThread(() -> Toast.makeText(this, resultText, Toast.LENGTH_LONG).show());
        }, "free-skill-download").start();
    }

    private LinearLayout.LayoutParams gap(int top) {
        LinearLayout.LayoutParams p = full();
        p.topMargin = dp(this, top);
        return p;
    }
}
