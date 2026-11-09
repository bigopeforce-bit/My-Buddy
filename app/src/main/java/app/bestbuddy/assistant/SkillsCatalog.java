package app.bestbuddy.assistant;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The skills BestBuddy can install with one tap (Settings → Skills → Discover). Each is a SKILL.md bundled in
 * assets/skills/&lt;name&gt;/; installing copies the folder into ~/.agents/skills, where the agents look, so the next task
 * can use it without anything else to set up. The list is fixed and ordered by usefulness on a phone.
 */
final class SkillsCatalog {
    static final String[] ORDER = {
            "daily-brief", "inbox-triage", "reply-like-me", "summarize-chat", "meeting-notes",
            "web-research", "price-watch", "travel-plan", "receipt-tracker", "translate-tone",
            "writing-polish", "study-cards", "data-cleanup", "code-review", "app-walkthrough", "android-app",
    };

    static final class Item {
        final String name, title, description;

        Item(String name, String title, String description) {
            this.name = name;
            this.title = title;
            this.description = description;
        }
    }

    private SkillsCatalog() {
    }

    static List<Item> all(Context c) {
        List<Item> out = new ArrayList<>();
        for (String name : ORDER) {
            String md = read(c, name);
            if (md == null) continue;
            String[] fm = Skills.frontMatter(md);
            out.add(new Item(name, title(name), fm[1]));
        }
        return out;
    }

    static Item find(Context c, String name) {
        for (Item i : all(c)) if (i.name.equals(name)) return i;
        return null;
    }

    /** "reply-like-me" → "Reply like me". */
    static String title(String slug) {
        String[] words = slug.split("-");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            if (i == 0) b.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
            else b.append(words[i]);
        }
        return b.toString();
    }

    /** A bundled SKILL.md, or null when this build doesn't have it. */
    static String read(Context c, String name) {
        try (InputStream in = c.getAssets().open("skills/" + name + "/SKILL.md")) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            return o.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }
}
