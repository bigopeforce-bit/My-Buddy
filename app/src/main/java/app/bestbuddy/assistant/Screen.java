package app.bestbuddy.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/** Base for BestBuddy's screens: applies the theme and rebuilds when the appearance settings change. */
abstract class Screen extends Activity {
    private String themeKey;

    @Override
    protected void onCreate(Bundle state) {
        Theme.apply(this);
        themeKey = Theme.key();
        super.onCreate(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Theme.load(this);
        if (!Theme.key().equals(themeKey)) {
            recreate();
            return;
        }
        applyAppearanceMotion();
    }

    private void applyAppearanceMotion() {
        String motion = Prefs.motion(this);
        if ("off".equals(motion)) return;
        View content = findViewById(android.R.id.content);
        if (content == null) return;
        long duration = "full".equals(motion) ? 240L : 120L;
        content.setAlpha(0.96f);
        content.setTranslationY(Ui.dp(this, "full".equals(motion) ? 10 : 4));
        content.animate().alpha(1f).translationY(0f).setDuration(duration).start();
    }

    /** Re-apply the theme in place (after a change made on this screen). */
    protected void rethemed() {
        Theme.apply(this);
        themeKey = Theme.key();
    }
}
