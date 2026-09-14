package com.setlist;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.res.ColorStateList;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.button.MaterialButton;

final class Ui {
    static int BG, SURFACE, ACCENT, TEXT, MUTED, ERROR, OUTLINE, DISABLED, RIPPLE, HIGH, ACCENT_CONTAINER, SUCCESS;
    private static Typeface bodyFont, headingFont;
    static final int CAPTION = 14, BODY = 16, TITLE = 20, HEADING = 28, DISPLAY = 36;
    static void init(Context c) {
        BG = c.getColor(R.color.stage_background); SURFACE = c.getColor(R.color.stage_surface);
        ACCENT = c.getColor(R.color.stage_accent); TEXT = c.getColor(R.color.stage_text);
        MUTED = c.getColor(R.color.stage_muted); ERROR = c.getColor(R.color.stage_error);
        OUTLINE = c.getColor(R.color.stage_outline); DISABLED = c.getColor(R.color.stage_disabled); RIPPLE = c.getColor(R.color.stage_ripple);
        HIGH = c.getColor(R.color.stage_surface_high); ACCENT_CONTAINER = c.getColor(R.color.stage_accent_container); SUCCESS = c.getColor(R.color.stage_success);
        bodyFont = ResourcesCompat.getFont(c, R.font.inter); headingFont = ResourcesCompat.getFont(c, R.font.manrope);
    }
    static int dp(Context c, float value) {
        int resource = switch ((int) value) {
            case 4 -> R.dimen.space_xs; case 8 -> R.dimen.space_sm; case 12 -> R.dimen.space_md;
            case 16 -> R.dimen.space_lg; case 24 -> R.dimen.space_xl; case 32 -> R.dimen.space_2xl;
            case 48 -> R.dimen.touch_min; case 56 -> R.dimen.control_height; case 960 -> R.dimen.editor_max_width;
            default -> 0;
        };
        return resource == 0 ? Math.round(value * c.getResources().getDisplayMetrics().density) : c.getResources().getDimensionPixelSize(resource);
    }
    static boolean compact(Context c) { return c.getResources().getConfiguration().smallestScreenWidthDp < 600; }
    // Phones in landscape: too short to stack a header, a list and footer controls.
    static boolean shortScreen(Context c) { return c.getResources().getConfiguration().screenHeightDp < 480; }
    // Phones step headings down one notch; body, caption and metadata sizes stay readable at full size.
    private static int typeSize(Context c, int size) {
        if (!compact(c)) return size;
        return switch (size) { case DISPLAY -> 30; case HEADING -> 24; case TITLE -> 18; default -> size; };
    }
    static LinearLayout column(Context c) { LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    static LinearLayout row(Context c) { LinearLayout v = new LinearLayout(c); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    static TextView text(Context c, String text, int size, int color) {
        TextView v = new TextView(c); v.setText(text); v.setTextSize(typeSize(c, size)); v.setTextColor(color);
        v.setTypeface(bodyFont); v.setFontVariationSettings("'wght' 400, 'opsz' 18");
        v.setIncludeFontPadding(false); v.setLineSpacing(0, 1.22f); return v;
    }
    static TextView heading(Context c, String text, int size) {
        TextView v = text(c, text, size, TEXT); v.setTypeface(headingFont); v.setFontVariationSettings("'wght' 650"); v.setLetterSpacing(-0.02f); return v;
    }
    static Button button(Context c, String text, boolean primary, Runnable action) {
        MaterialButton v = new MaterialButton(c); v.setText(text); v.setAllCaps(false); v.setTextSize(BODY);
        v.setTypeface(bodyFont); v.setFontVariationSettings("'wght' 600, 'opsz' 18"); v.setLetterSpacing(-0.015f);
        v.setTextColor(states(DISABLED, primary ? BG : TEXT));
        v.setMinHeight(dp(c, 56)); v.setMinimumWidth(dp(c, 48));
        v.setCornerRadius(dp(c, 16)); v.setInsetTop(0); v.setInsetBottom(0);
        v.setBackgroundTintList(states(SURFACE, primary ? ACCENT : HIGH));
        v.setRippleColor(ColorStateList.valueOf(RIPPLE)); v.setElevation(0); v.setStateListAnimator(null);
        v.setIconSize(dp(c, 22)); v.setIconPadding(dp(c, 8)); v.setIconTint(v.getTextColors());
        v.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(c, 4); params.bottomMargin = dp(c, 4); v.setLayoutParams(params);
        v.setOnClickListener(view -> action.run()); return v;
    }
    private static ColorStateList states(int disabled, int normal) {
        return new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}}, new int[]{disabled, normal});
    }
    static Button withIcon(Button button, int icon, boolean end) {
        MaterialButton b = (MaterialButton) button; b.setIconResource(icon);
        b.setIconGravity(end ? MaterialButton.ICON_GRAVITY_TEXT_END : MaterialButton.ICON_GRAVITY_TEXT_START); return b;
    }
    static Button quiet(Context c, String label, int icon, Runnable action) {
        MaterialButton b = (MaterialButton) withIcon(button(c, label, false, action), icon, false);
        b.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)); b.setMinHeight(dp(c, 48)); b.setTextColor(MUTED); b.setIconTint(ColorStateList.valueOf(MUTED)); return b;
    }
    static ImageView icon(Context c, int resource, int color, int size) {
        ImageView icon = new ImageView(c); icon.setImageResource(resource); icon.setImageTintList(ColorStateList.valueOf(color));
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(c, size), dp(c, size))); icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); return icon;
    }
    static TextView eyebrow(Context c, String label) {
        TextView v = text(c, label, 12, MUTED); v.setFontVariationSettings("'wght' 600, 'opsz' 14"); v.setLetterSpacing(.1f); return v;
    }
    static TextView badge(Context c, String label) {
        TextView v = text(c, label, 12, SUCCESS); v.setFontVariationSettings("'wght' 600, 'opsz' 14"); v.setLetterSpacing(.04f);
        v.setBackground(shape(SURFACE, c)); v.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8)); return v;
    }
    static View brand(Context c) {
        ImageView icon = new ImageView(c); icon.setImageResource(R.drawable.ic_setlist);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); return icon;
    }
    static LinearLayout emptyState(Context c, int illustration, String title, String message, String action, Runnable onAction) {
        LinearLayout box = column(c); box.setGravity(Gravity.CENTER_HORIZONTAL); pad(box, 24);
        ImageView art = new ImageView(c); art.setImageResource(illustration); art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        box.addView(art, compact(c) ? new LinearLayout.LayoutParams(dp(c, 160), dp(c, 109)) : new LinearLayout.LayoutParams(dp(c, 200), dp(c, 136))); gap(box, 20);
        TextView heading = heading(c, title, TITLE); heading.setGravity(Gravity.CENTER); box.addView(heading); gap(box, 12);
        TextView copy = text(c, message, BODY, MUTED); copy.setGravity(Gravity.CENTER); copy.setMaxWidth(dp(c, 400)); box.addView(copy);
        if (action != null) { gap(box, 20); Button button = button(c, action, true, onAction); box.addView(button, new LinearLayout.LayoutParams(-2, -2)); }
        return box;
    }
    static void pad(View v, int size) { int p = dp(v.getContext(), size); v.setPadding(p, p, p, p); }
    static void gap(LinearLayout v, int size) { View gap = new View(v.getContext()); v.addView(gap, new LinearLayout.LayoutParams(1, dp(v.getContext(), size))); }
    static void card(View v) {
        GradientDrawable shape = shape(SURFACE, v.getContext()); shape.setStroke(dp(v.getContext(), 1), OUTLINE);
        v.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(RIPPLE), shape, null)); pad(v, 16);
    }
    private static GradientDrawable shape(int color, Context context) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color);
        shape.setCornerRadius(context.getResources().getDimension(R.dimen.radius_surface)); return shape;
    }
    static void weighted(LinearLayout row, View view) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        if (row.getChildCount() > 0) p.setMarginStart(dp(row.getContext(), 8)); row.addView(view, p);
    }
    static void insets(View root) {
        if (android.os.Build.VERSION.SDK_INT < 30) return;
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        root.requestApplyInsets();
    }
}
