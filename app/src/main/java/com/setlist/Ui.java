package com.setlist;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static int BG, SURFACE, ACCENT, TEXT, MUTED, ERROR, OUTLINE, DISABLED, RIPPLE;
    static final int CAPTION = 14, BODY = 16, TITLE = 20, HEADING = 28, DISPLAY = 36;
    static void init(Context c) {
        BG = c.getColor(R.color.stage_background); SURFACE = c.getColor(R.color.stage_surface);
        ACCENT = c.getColor(R.color.stage_accent); TEXT = c.getColor(R.color.stage_text);
        MUTED = c.getColor(R.color.stage_muted); ERROR = c.getColor(R.color.stage_error);
        OUTLINE = c.getColor(R.color.stage_outline); DISABLED = c.getColor(R.color.stage_disabled); RIPPLE = c.getColor(R.color.stage_ripple);
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
    static LinearLayout column(Context c) { LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    static LinearLayout row(Context c) { LinearLayout v = new LinearLayout(c); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    static TextView text(Context c, String text, int size, int color) {
        TextView v = new TextView(c); v.setText(text); v.setTextSize(size); v.setTextColor(color);
        v.setLineSpacing(0, 1.15f); return v;
    }
    static TextView heading(Context c, String text, int size) {
        TextView v = text(c, text, size, TEXT); v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); return v;
    }
    static Button button(Context c, String text, boolean primary, Runnable action) {
        Button v = new Button(c); v.setText(text); v.setAllCaps(false); v.setTextSize(16);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}}, new int[]{DISABLED, primary ? BG : TEXT}));
        v.setMinHeight(dp(c, 56)); v.setMinimumWidth(dp(c, 48));
        GradientDrawable shape = shape(primary ? ACCENT : SURFACE, c);
        if (!primary) shape.setStroke(dp(c, 1), OUTLINE);
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled}, shape(SURFACE, c)); states.addState(new int[]{}, shape);
        v.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(RIPPLE), states, null));
        v.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(c, 4); params.bottomMargin = dp(c, 4); v.setLayoutParams(params);
        v.setOnClickListener(view -> action.run()); return v;
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
