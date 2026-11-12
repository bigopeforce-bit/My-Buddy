package app.bestbuddy.assistant;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

/**
 * Computes BestBuddy's palette from the user's appearance settings (mode, accent, surface style) and
 * writes it into the {@link Ui} colour fields. Every screen calls {@link #apply(Activity)} in onCreate.
 */
final class Theme {
    static final String[][] ACCENTS = {
            {"#3D6FE0", "Ocean"}, {"#7C5CE0", "Violet"}, {"#12B886", "Mint"},
            {"#E8734A", "Coral"}, {"#E5678F", "Rose"}, {"#D9A03A", "Amber"},
    };
    static final String[][] SURFACES = {{"warm", "Warm"}, {"neutral", "Neutral"}, {"black", "Black"}, {"amoled", "AMOLED"}, {"minimal", "Minimal"}, {"sunset", "Sunset"}, {"dynamic", "Dynamic"}};
    static final String[][] MODES = {{"system", "System"}, {"light", "Light"}, {"dark", "Dark"}};

    private static String applied = "";

    private Theme() {
    }

    /** {@link #ACCENTS} with the colour names in the user's language (the labels above are English). */
    static String[][] accents(Context c) {
        return labeled(c, ACCENTS, R.string.prefs_accent_ocean, R.string.prefs_accent_violet, R.string.prefs_accent_mint,
                R.string.prefs_accent_coral, R.string.prefs_accent_rose, R.string.prefs_accent_amber);
    }

    /** {@link #SURFACES} with labels in the user's language. */
    static String[][] surfaces(Context c) {
        String[][] out = new String[SURFACES.length][];
        out[0] = new String[]{SURFACES[0][0], c.getString(R.string.prefs_surface_warm)};
        out[1] = new String[]{SURFACES[1][0], c.getString(R.string.prefs_surface_neutral)};
        out[2] = new String[]{SURFACES[2][0], c.getString(R.string.prefs_surface_black)};
        for (int i = 3; i < SURFACES.length; i++) out[i] = SURFACES[i].clone();
        return out;
    }

    /** {@link #MODES} with labels in the user's language. */
    static String[][] modes(Context c) {
        return labeled(c, MODES, R.string.prefs_theme_system, R.string.prefs_theme_light, R.string.prefs_theme_dark);
    }

    /** {value, label} pairs with each label replaced by the string resource at the same index. */
    private static String[][] labeled(Context c, String[][] options, int... labels) {
        String[][] out = new String[options.length][];
        for (int i = 0; i < options.length; i++) out[i] = new String[]{options[i][0], c.getString(labels[i])};
        return out;
    }

    static boolean isDark(Context c) {
        String m = Prefs.themeMode(c);
        if ("dark".equals(m)) return true;
        if ("light".equals(m)) return false;
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Fill Ui's palette for the current settings. Cheap; returns whether anything changed. */
    static boolean load(Context c) {
        boolean dark = isDark(c);
        String surface = Prefs.surface(c);
        int accent = "dynamic".equals(surface) && Build.VERSION.SDK_INT >= 31
                ? systemColor(c, "system_accent1_600", Prefs.accent(c)) : Prefs.accent(c);
        String key = dark + "|" + accent + "|" + surface;
        if (key.equals(applied)) return false;
        applied = key;

        if (dark) {
            switch (surface) {
                case "warm":
                    set(0xFF171513, 0xFF24211E, 0xFF3A3632, 0xFF1E1B19, 0xFF0F0E0D);
                    break;
                case "black":
                    set(0xFF000000, 0xFF0D1119, 0xFF1A2230, 0xFF000000, 0xFF000000);
                    break;
                case "amoled":
                    set(0xFF000000, 0xFF080808, 0xFF151515, 0xFF000000, 0xFF000000);
                    break;
                case "minimal":
                    set(0xFF101010, 0xFF1B1B1B, 0xFF262626, 0xFF101010, 0xFF050505);
                    break;
                case "sunset":
                    set(0xFF1B100B, 0xFF29180F, 0xFF3A2114, 0xFF1B100B, 0xFF100904);
                    break;
                case "dynamic":
                    set(systemColor(c, "system_neutral1_900", 0xFF101218),
                            systemColor(c, "system_neutral1_800", 0xFF1B2028),
                            systemColor(c, "system_neutral2_800", 0xFF252B34),
                            systemColor(c, "system_neutral1_900", 0xFF101218), 0xFF050505);
                    break;
                default: // neutral: BestBuddy's own deep-navy palette
                    set(0xFF080E18, 0xFF111A28, 0xFF1B2740, 0xFF0C1420, 0xFF05090F);
            }
            Ui.TEXT = "sunset".equals(surface) ? 0xFFFFF1E6
                    : "minimal".equals(surface) ? 0xFFF1F1F1
                    : "dynamic".equals(surface) ? systemColor(c, "system_neutral1_50", 0xFFE9EFF9) : 0xFFE9EFF9;
            Ui.MUTED = "sunset".equals(surface) ? 0xFFC9A894
                    : "minimal".equals(surface) ? 0xFFA6A6A6
                    : "dynamic".equals(surface) ? systemColor(c, "system_neutral2_200", 0xFF93A2B8) : 0xFF93A2B8;
            Ui.OK = 0xFF35C98A;
            Ui.ERR = 0xFFF0736F;
            Ui.USER = blend(accent, Ui.BG, 0.24f);
        } else {
            if ("warm".equals(surface)) set(0xFFF7F3EE, 0xFFFFFFFF, 0xFFECE5DD, 0xFFF1EBE4, 0xFF24121E);
            else if ("minimal".equals(surface)) set(0xFFFAFAFA, 0xFFFFFFFF, 0xFFEEEEEE, 0xFFF7F7F7, 0xFF101010);
            else if ("sunset".equals(surface)) set(0xFFFFF7F0, 0xFFFFFFFF, 0xFFF5E4D6, 0xFFFFF1E6, 0xFF24120B);
            else if ("dynamic".equals(surface) && Build.VERSION.SDK_INT >= 31) {
                set(systemColor(c, "system_neutral1_10", 0xFFF7F7FA),
                        systemColor(c, "system_neutral1_50", 0xFFFFFFFF),
                        systemColor(c, "system_neutral2_100", 0xFFE8E8EC),
                        systemColor(c, "system_neutral1_10", 0xFFF7F7FA), 0xFF0A1222);
            } else set(0xFFF4F7FC, 0xFFFFFFFF, 0xFFE8EEF9, 0xFFFFFFFF, 0xFF0A1222);
            Ui.TEXT = "sunset".equals(surface) ? 0xFF2B170D : "minimal".equals(surface) ? 0xFF171717
                    : "dynamic".equals(surface) && Build.VERSION.SDK_INT >= 31
                    ? systemColor(c, "system_neutral1_900", 0xFF0E1A2B) : 0xFF0E1A2B;
            Ui.MUTED = "sunset".equals(surface) ? 0xFF8B6650 : "minimal".equals(surface) ? 0xFF666666
                    : "dynamic".equals(surface) && Build.VERSION.SDK_INT >= 31
                    ? systemColor(c, "system_neutral2_700", 0xFF5F6F86) : 0xFF5F6F86;
            Ui.OK = 0xFF12A66A;
            Ui.ERR = 0xFFD93A3F;
            Ui.USER = blend(accent, Ui.CARD, 0.16f);
        }
        Ui.ACCENT = accent;
        Ui.ON_ACCENT = luminance(accent) > 0.45 ? 0xFF1A1714 : 0xFFFFFFFF;
        Ui.ACCENT_TEXT = dark ? accent : darken(accent, luminance(accent) > 0.45 ? 0.62f : 0.85f);
        Ui.LINE = withAlpha(Ui.TEXT, 0x14);
        Ui.OUTLINE = withAlpha(Ui.TEXT, 0x22);
        Ui.ACCENT_SOFT = withAlpha(accent, 0x24);
        Ui.ERR_SOFT = withAlpha(Ui.ERR, 0x1F);
        Ui.CODE_TEXT = 0xFFF4EFE6; // the code block stays dark in both modes
        Ui.DARK = dark;
        return true;
    }

    /** Read Android 12+ Material You system palette colours without linking a Google dependency. */
    private static int systemColor(Context c, String name, int fallback) {
        if (Build.VERSION.SDK_INT < 31) return fallback;
        int id = c.getResources().getIdentifier(name, "color", "android");
        if (id == 0) return fallback;
        try { return c.getColor(id); } catch (Exception ignored) { return fallback; }
    }

    private static void set(int bg, int card, int card2, int nav, int code) {
        Ui.BG = bg;
        Ui.CARD = card;
        Ui.CARD2 = card2;
        Ui.NAV = nav;
        Ui.CODE = code;
    }

    /** Load the palette and style the window (bar colours and icon contrast) for an activity. */
    static void apply(Activity a) {
        load(a);
        Window w = a.getWindow();
        w.setStatusBarColor(Ui.BG);
        w.setNavigationBarColor(Ui.NAV);
        int flags = w.getDecorView().getSystemUiVisibility();
        if (Ui.DARK) {
            flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }

    /** Settings changed: recalc, and tell the bubble to restyle itself. */
    static void changed(Context c) {
        applied = "";
        load(c);
        BestBuddyService s = BestBuddyService.get();
        if (s != null) s.applyTheme();
    }

    static String key() {
        return applied;
    }

    static int parse(String hex, int fallback) {
        try {
            return Color.parseColor(hex.startsWith("#") ? hex : "#" + hex) | 0xFF000000;
        } catch (Exception e) {
            return fallback;
        }
    }

    static String hex(int c) {
        return String.format("#%06X", c & 0xFFFFFF);
    }

    static int withAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | (a << 24);
    }

    static int blend(int a, int b, float t) {
        return Color.rgb(
                Math.round(Color.red(b) + (Color.red(a) - Color.red(b)) * t),
                Math.round(Color.green(b) + (Color.green(a) - Color.green(b)) * t),
                Math.round(Color.blue(b) + (Color.blue(a) - Color.blue(b)) * t));
    }

    static int darken(int c, float f) {
        return Color.rgb(Math.round(Color.red(c) * f), Math.round(Color.green(c) * f), Math.round(Color.blue(c) * f));
    }

    static double luminance(int c) {
        return (0.2126 * Color.red(c) + 0.7152 * Color.green(c) + 0.0722 * Color.blue(c)) / 255.0;
    }
}
