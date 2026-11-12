package app.bestbuddy.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static app.bestbuddy.assistant.Ui.*;

/** User-facing directory for BestBuddy's built-in tools and work areas. */
public final class ToolHubActivity extends Screen {
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = column(this);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout header = row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPaddingRelative(dp(this, 18), dp(this, 12), dp(this, 18), dp(this, 12));
        Button back = outlinedButton(this, "‹", MUTED);
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(this, 44), dp(this, 44)));
        LinearLayout heading = column(this);
        heading.setPaddingRelative(dp(this, 12), 0, 0, 0);
        heading.addView(title(this, "Explore tools", 23));
        heading.addView(text(this, "Open BestBuddy's built-in workspaces and phone tools.", 12, MUTED));
        header.addView(heading, weight1());
        root.addView(header, full());

        ScrollView scroll = new ScrollView(this);
        content = column(this);
        content.setPaddingRelative(dp(this, 16), dp(this, 8), dp(this, 16), dp(this, 24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNav(this, NAV_SETTINGS, ChatHub.get(this).runningCount()), full());
        setContentView(root);
        render();
    }

    private void render() {
        content.removeAllViews();
        section("Phone and execution");
        addTool("Terminal", "Run commands in BestBuddy's built-in Linux environment", TerminalActivity.class);
        addTool("Browser", "Open and work with web pages", BrowserActivity.class);
        addTool("App permissions", "Review BestBuddy's Android access and permissions", AppPermissionsActivity.class);

        section("AI providers and models");
        addTool("Providers", "Choose and configure available AI providers", ProvidersActivity.class);
        addTool("Models", "Browse and select configured models", ModelsActivity.class);
        addTool("API keys", "Manage provider credentials", ApiKeysActivity.class);
        addTool("FreeLLMAPI", "Configure the unified free-model API connection", FreeLLMAPIActivity.class);

        section("Skills and integrations");
        addTool("Free skills library", "Browse, preview and install available skills", FreeSkillsActivity.class);
        addTool("Installed skills", "Manage and import your skills", SkillsActivity.class);
        addTool("GitHub", "Connect and work with GitHub repositories", GitHubActivity.class);

        section("Automation and review");
        addTool("Schedules", "Create and manage scheduled tasks", SchedulesActivity.class);
        addTool("Automation rules", "Configure trusted routines and notification rules", RulesActivity.class);
        addTool("Waiting for review", "Review actions and questions awaiting your input", WaitingActivity.class);
    }

    private void section(String label) {
        TextView heading = sectionLabel(this, label);
        content.addView(heading, topGap(16));
    }

    private void addTool(String name, String description, Class<?> destination) {
        LinearLayout card = column(this);
        card.setPaddingRelative(dp(this, 14), dp(this, 12), dp(this, 14), dp(this, 12));
        card.setBackground(outlined(this, CARD, 16, LINE, 1));
        TextView itemTitle = title(this, name, 15);
        card.addView(itemTitle);
        card.addView(text(this, description, 12, MUTED), topGap(3));
        Button open = button(this, "Open " + name, false);
        open.setOnClickListener(v -> startActivity(new Intent(this, destination)));
        card.addView(open, topGap(9));
        content.addView(card, topGap(8));
    }

    private LinearLayout.LayoutParams topGap(int value) {
        LinearLayout.LayoutParams lp = full();
        lp.topMargin = dp(this, value);
        return lp;
    }
}
