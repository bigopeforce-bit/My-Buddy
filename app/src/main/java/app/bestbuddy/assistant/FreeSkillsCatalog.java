package app.bestbuddy.assistant;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Curated free, openly licensed public skill sources. Only raw.githubusercontent.com URLs are fetched. */
final class FreeSkillsCatalog {
    static final String ALL = "All stores";
    static final class Item {
        final String store, name, title, description, license, source, url;
        Item(String store, String name, String title, String description, String license, String source, String url) {
            this.store = store; this.name = name; this.title = title; this.description = description;
            this.license = license; this.source = source; this.url = url;
        }
    }

    private static final List<Item> ITEMS = new ArrayList<>();
    static {
        add("Superpowers", "brainstorming", "Brainstorming", "Explore options and shape an idea before implementation.", "MIT", "obra/superpowers", "skills/brainstorming/SKILL.md");
        add("Superpowers", "systematic-debugging", "Systematic debugging", "Investigate root causes with a repeatable debugging process.", "MIT", "obra/superpowers", "skills/systematic-debugging/SKILL.md");
        add("Superpowers", "test-driven-development", "Test-driven development", "Build changes through failing tests, implementation, and verification.", "MIT", "obra/superpowers", "skills/test-driven-development/SKILL.md");
        add("Superpowers", "writing-plans", "Writing plans", "Turn a larger task into a clear, executable implementation plan.", "MIT", "obra/superpowers", "skills/writing-plans/SKILL.md");
        add("Superpowers", "verification-before-completion", "Verification before completion", "Require concrete checks before reporting work as complete.", "MIT", "obra/superpowers", "skills/verification-before-completion/SKILL.md");

        add("Hugging Face", "hf-cli", "Hugging Face CLI", "Work with Hugging Face Hub resources from the command line.", "Apache-2.0", "huggingface/skills", "skills/hf-cli/SKILL.md");
        add("Hugging Face", "huggingface-datasets", "Hugging Face datasets", "Find, inspect, load, and work with datasets on the Hub.", "Apache-2.0", "huggingface/skills", "skills/huggingface-datasets/SKILL.md");
        add("Hugging Face", "huggingface-gradio", "Hugging Face Gradio", "Build and work with Gradio demos and Spaces.", "Apache-2.0", "huggingface/skills", "skills/huggingface-gradio/SKILL.md");
        add("Hugging Face", "huggingface-best", "Hugging Face best practices", "Use practical patterns for Hugging Face model workflows.", "Apache-2.0", "huggingface/skills", "skills/huggingface-best/SKILL.md");

        add("GitHub Awesome Copilot", "code-review", "Code review", "Review changes for correctness, maintainability, and risk.", "MIT", "github/awesome-copilot", ".github/skills/code-review/SKILL.md");
        add("GitHub Awesome Copilot", "acquire-codebase-knowledge", "Codebase knowledge", "Understand an unfamiliar repository and its architecture.", "MIT", "github/awesome-copilot", "skills/acquire-codebase-knowledge/SKILL.md");
        add("GitHub Awesome Copilot", "agent-architecture", "Agent architecture", "Design structured AI agents and their components.", "MIT", "github/awesome-copilot", "skills/agent-architecture/SKILL.md");
        add("GitHub Awesome Copilot", "add-educational-comments", "Educational code comments", "Add useful explanations to code without cluttering it.", "MIT", "github/awesome-copilot", "skills/add-educational-comments/SKILL.md");

        add("wshobson Agents", "api-design-principles", "API design principles", "Design consistent, maintainable APIs and service contracts.", "MIT", "wshobson/agents", "plugins/backend-development/skills/api-design-principles/SKILL.md");
        add("wshobson Agents", "wcag-audit-patterns", "Accessibility audit", "Check interfaces against practical accessibility patterns.", "MIT", "wshobson/agents", "plugins/accessibility-compliance/skills/wcag-audit-patterns/SKILL.md");
        add("wshobson Agents", "parallel-debugging", "Parallel debugging", "Coordinate independent debugging investigations.", "MIT", "wshobson/agents", "plugins/agent-teams/skills/parallel-debugging/SKILL.md");
        add("wshobson Agents", "avoid-ai-writing", "Natural writing", "Improve writing that sounds generic or machine-generated.", "MIT", "wshobson/agents", "plugins/avoid-ai-writing/skills/avoid-ai-writing/SKILL.md");
    }

    private FreeSkillsCatalog() {}
    private static void add(String store, String name, String title, String desc, String license, String repo, String path) {
        ITEMS.add(new Item(store, name, title, desc, license, repo,
                "https://raw.githubusercontent.com/" + repo + "/main/" + path));
    }
    static List<String> stores() {
        List<String> out = new ArrayList<>(); out.add(ALL);
        for (Item i : ITEMS) if (!out.contains(i.store)) out.add(i.store);
        return out;
    }
    static List<Item> all() { return new ArrayList<>(ITEMS); }
    static List<Item> inStore(String store) {
        List<Item> out = new ArrayList<>();
        for (Item i : ITEMS) if (ALL.equals(store) || i.store.equals(store)) out.add(i);
        return out;
    }

    /** Download a curated Markdown skill over HTTPS; cap size and reject any non-GitHub raw host. */
    static String fetch(Item item) throws Exception {
        URL u = new URL(item.url);
        if (!"https".equals(u.getProtocol()) || !"raw.githubusercontent.com".equals(u.getHost())) {
            throw new SecurityException("Untrusted skill source.");
        }
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "BestBuddy-Android-Skills");
        try {
            int code = c.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IllegalStateException("Skill download failed (HTTP " + code + ").");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] b = new byte[8192]; int n;
                while ((n = in.read(b)) != -1) {
                    out.write(b, 0, n);
                    if (out.size() > 2 * 1024 * 1024) throw new IllegalArgumentException("Skill file is too large.");
                }
                return out.toString("UTF-8");
            }
        } finally { c.disconnect(); }
    }
}
