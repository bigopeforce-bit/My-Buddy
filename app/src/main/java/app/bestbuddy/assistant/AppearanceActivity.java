package app.bestbuddy.assistant;

import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import static app.bestbuddy.assistant.Ui.*;

/** Dedicated appearance editor for themes, accents and typography. */
public final class AppearanceActivity extends Screen {
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    private void build() {
        LinearLayout root = column(this);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout header = row(this);
        header.setPaddingRelative(dp(this, 18), dp(this, 12), dp(this, 18), dp(this, 10));
        FrameLayout back = new FrameLayout(this);
        back.addView(icon(this, R.drawable.ms_arrow_back, TEXT, 22),
                new FrameLayout.LayoutParams(dp(this, 44), dp(this, 44), Gravity.CENTER));
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(this, 44), dp(this, 44)));
        LinearLayout heading = column(this);
        heading.addView(title(this, "Appearance", 24));
        heading.addView(text(this, "Personalise BestBuddy's colours and type", 13, MUTED));
        LinearLayout.LayoutParams hp = weight1();
        hp.setMarginStart(dp(this, 10));
        header.addView(heading, hp);
        root.addView(header, full());

        ScrollView scroll = new ScrollView(this);
        content = column(this);
        content.setPaddingRelative(dp(this, 16), dp(this, 8), dp(this, 16), dp(this, 28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNav(this, NAV_SETTINGS, ChatHub.get(this).runningCount()), full());
        setContentView(root);
        render();
    }

    private void render() {
        content.removeAllViews();

        content.addView(sectionLabel(this, "Theme mode"));
        LinearLayout mode = panel();
        mode.addView(segmented(this, Theme.modes(this), Prefs.themeMode(this), false, value -> {
            Prefs.setThemeMode(this, value);
            changed();
        }));
        content.addView(mode, topGap(8));

        content.addView(sectionLabel(this, "Accent colour"));
        LinearLayout accent = panel();
        accent.addView(text(this, "Choose a preset or enter your own HEX colour.", 13, MUTED));
        LinearLayout swatches = row(this);
        swatches.setGravity(Gravity.CENTER_VERTICAL);
        for (String[] option : Theme.accents(this)) {
            final int colour = Theme.parse(option[0], 0xFF3D6FE0);
            boolean selected = colour == Prefs.accent(this);
            FrameLayout cell = new FrameLayout(this);
            cell.setContentDescription(option[1]);
            FrameLayout ring = new FrameLayout(this);
            if (selected) ring.setBackground(outlined(this, 0x00000000, 22, TEXT, 2));
            View dot = new View(this);
            dot.setBackground(oval(colour));
            ring.addView(dot, new FrameLayout.LayoutParams(dp(this, 30), dp(this, 30), Gravity.CENTER));
            cell.addView(ring, new FrameLayout.LayoutParams(dp(this, 40), dp(this, 40), Gravity.CENTER));
            cell.setOnClickListener(v -> {
                Prefs.setAccent(this, colour);
                changed();
            });
            swatches.addView(cell, new LinearLayout.LayoutParams(0, dp(this, 44), 1));
        }
        accent.addView(swatches, topGap(10));
        Button custom = button(this, "Custom colour · " + Theme.hex(Prefs.accent(this)), false);
        custom.setOnClickListener(v -> customAccent());
        accent.addView(custom, topGap(10));
        content.addView(accent, topGap(8));

        content.addView(sectionLabel(this, "Colour style"));
        LinearLayout styles = panel();
        String[][] options = Theme.surfaces(this);
        for (int start = 0; start < options.length; start += 3) {
            int end = Math.min(options.length, start + 3);
            String[][] group = java.util.Arrays.copyOfRange(options, start, end);
            styles.addView(segmented(this, group, Prefs.surface(this), false, value -> {
                Prefs.setSurface(this, value);
                changed();
            }));
            if (end < options.length) styles.addView(new View(this), new LinearLayout.LayoutParams(1, dp(this, 6)));
        }
        TextView note = text(this,
                "AMOLED uses true-black surfaces. Dynamic reads Android's Material You palette. Liquid Glass enables Android's real backdrop blur on Android 12+; older versions use a translucent fallback.",
                12, MUTED);
        styles.addView(note, topGap(10));
        content.addView(styles, topGap(8));

        content.addView(sectionLabel(this, "Wallpaper and glass"));
        LinearLayout glass = panel();
        glass.addView(text(this, "Choose an image to show behind translucent Liquid Glass surfaces.", 13, MUTED));
        Button wallpaper = button(this, Prefs.appearanceWallpaper(this).isEmpty()
                ? "Choose wallpaper" : "Change wallpaper", false);
        wallpaper.setOnClickListener(v -> chooseWallpaper());
        glass.addView(wallpaper, topGap(10));
        if (!Prefs.appearanceWallpaper(this).isEmpty()) {
            Button clear = outlinedButton(this, "Remove wallpaper", MUTED);
            clear.setOnClickListener(v -> {
                Prefs.setAppearanceWallpaper(this, "");
                changed();
            });
            glass.addView(clear, topGap(8));
        }
        TextView blurLabel = text(this, "Backdrop blur · " + Prefs.glassBlur(this) + " dp", 13, TEXT);
        glass.addView(blurLabel, topGap(14));
        SeekBar blur = new SeekBar(this);
        blur.setMax(64);
        blur.setProgress(Prefs.glassBlur(this));
        blur.setEnabled(Build.VERSION.SDK_INT >= 31);
        blur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                blurLabel.setText("Backdrop blur · " + progress + " dp");
                if (fromUser) {
                    Prefs.setGlassBlur(AppearanceActivity.this, progress);
                    Theme.changed(AppearanceActivity.this);
                    rethemed();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { render(); }
        });
        glass.addView(blur, topGap(4));
        glass.addView(text(this, Build.VERSION.SDK_INT >= 31
                ? "Uses Android 12+ window backdrop blur. The effect is visible when Liquid Glass style is selected."
                : "System backdrop blur requires Android 12 or newer; your device uses the translucent fallback.", 12, MUTED), topGap(4));
        content.addView(glass, topGap(8));

        content.addView(sectionLabel(this, "Motion"));
        LinearLayout motion = panel();
        motion.addView(text(this, "Controls screen entrance transitions throughout BestBuddy.", 13, MUTED));
        motion.addView(segmented(this, new String[][]{
                {"off", "Off"}, {"subtle", "Subtle"}, {"full", "Full"}
        }, Prefs.motion(this), false, value -> {
            Prefs.setMotion(this, value);
            changed();
        }), topGap(10));
        content.addView(motion, topGap(8));

        content.addView(sectionLabel(this, "Typography"));
        LinearLayout type = panel();
        type.addView(text(this, "Select the font used throughout BestBuddy.", 13, MUTED));
        type.addView(segmented(this, new String[][]{
                {"geist", "Geist"}, {"system", "System"}, {"rounded", "Rounded"},
                {"serif", "Serif"}, {"mono", "Mono"}, {"condensed", "Condensed"}
        }, Prefs.fontFamily(this), false, value -> {
            Prefs.setFontFamily(this, value);
            changed();
        }), topGap(10));
        content.addView(type, topGap(8));

        content.addView(sectionLabel(this, "Live preview"));
        LinearLayout preview = panel();
        preview.setBackground(rounded(this, CARD2, 22));
        preview.addView(title(this, "Your BestBuddy", 21));
        preview.addView(text(this, "A preview of the selected palette and typography.", 13, MUTED), topGap(4));
        preview.addView(button(this, "Primary action", true), topGap(14));
        content.addView(preview, topGap(8));
    }

    private LinearLayout.LayoutParams topGap(int value) {
        LinearLayout.LayoutParams lp = full();
        lp.topMargin = dp(this, value);
        return lp;
    }

    private LinearLayout panel() {
        LinearLayout box = column(this);
        box.setPaddingRelative(dp(this, 14), dp(this, 14), dp(this, 14), dp(this, 14));
        box.setBackground(rounded(this, CARD, 18));
        return box;
    }

    private void customAccent() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("#6750A4");
        input.setText(Theme.hex(Prefs.accent(this)));
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(this, 14), dp(this, 10), dp(this, 14), dp(this, 10));
        input.setBackground(outlined(this, BG, 12, OUTLINE, 1));
        new AlertDialog.Builder(this)
                .setTitle("Custom accent colour")
                .setMessage("Enter a HEX value such as #6750A4.")
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    int parsed = Theme.parse(value, 0);
                    if (parsed == 0) {
                        Toast.makeText(this, "Enter a valid HEX colour.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Prefs.setAccent(this, parsed);
                    changed();
                }).show();
    }

    private void chooseWallpaper() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(pick, 7412);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 7412 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Prefs.setAppearanceWallpaper(this, uri.toString());
            changed();
        } catch (SecurityException e) {
            Toast.makeText(this, "Could not keep access to that image. Please choose it again.", Toast.LENGTH_LONG).show();
        }
    }

    private void changed() {
        Theme.changed(this);
        rethemed();
        render();
    }
}
