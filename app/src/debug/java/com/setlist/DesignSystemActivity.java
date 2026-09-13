package com.setlist;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/** Debug-only reference sheet; never included in the downloadable release. */
public final class DesignSystemActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = Ui.column(this); root.setBackgroundColor(Ui.BG); setContentView(root); Ui.insets(root);
        ScrollView scroll = new ScrollView(this); root.addView(scroll);
        LinearLayout page = Ui.column(this); Ui.pad(page, 24); scroll.addView(page);
        LinearLayout masthead = Ui.row(this); masthead.addView(Ui.brand(this));
        Ui.weighted(masthead, Ui.heading(this, "Setlist / Dark stage", Ui.HEADING)); masthead.addView(Ui.badge(this, "MATERIAL 3")); page.addView(masthead); Ui.gap(page, 16);
        page.addView(Ui.text(this, "Manrope + Inter  ·  Material Symbols Rounded  ·  Built for playing live", Ui.BODY, Ui.MUTED)); Ui.gap(page, 24);
        boolean wide = getResources().getConfiguration().screenWidthDp >= 800;
        LinearLayout columns = wide ? Ui.row(this) : Ui.column(this); columns.setGravity(Gravity.TOP); page.addView(columns);
        LinearLayout left = Ui.column(this), right = Ui.column(this);
        if (wide) { Ui.weighted(columns, left); Ui.weighted(columns, right); } else { columns.addView(left); columns.addView(right); }
        left.addView(Ui.eyebrow(this, "01 / TYPOGRAPHY")); Ui.gap(left, 16);
        left.addView(Ui.heading(this, "Your next great set.", Ui.DISPLAY)); Ui.gap(left, 12);
        left.addView(Ui.heading(this, "Friday night at the Blue Note", Ui.TITLE)); Ui.gap(left, 12);
        left.addView(Ui.text(this, "Clear at a glance. Comfortable through the encore.", Ui.BODY, Ui.TEXT)); Ui.gap(left, 8);
        left.addView(Ui.text(this, "Body 16 / Caption 14 / Metadata 12", Ui.CAPTION, Ui.MUTED)); Ui.gap(left, 24);
        left.addView(Ui.eyebrow(this, "02 / COLOR ROLES")); Ui.gap(left, 12);
        LinearLayout swatches = Ui.row(this); left.addView(swatches);
        int[] colors = {Ui.BG, Ui.SURFACE, Ui.HIGH, Ui.ACCENT, Ui.SUCCESS};
        String[] names = {"Ink", "Surface", "Tonal", "Amber", "Sage"};
        for (int i = 0; i < colors.length; i++) {
            LinearLayout item = Ui.column(this); Ui.weighted(swatches, item);
            View sample = new View(this); android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setColor(colors[i]); shape.setCornerRadius(Ui.dp(this, 12)); shape.setStroke(Ui.dp(this, 1), Ui.OUTLINE); sample.setBackground(shape);
            item.addView(sample, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56))); Ui.gap(item, 8); item.addView(Ui.text(this, names[i], 12, Ui.MUTED));
        }
        Ui.gap(left, 24); left.addView(Ui.eyebrow(this, "03 / ROUNDED ICONS")); Ui.gap(left, 12);
        int[] icons = {R.drawable.ic_queue_music, R.drawable.ic_add, R.drawable.ic_arrow_back, R.drawable.ic_chevron_right, R.drawable.ic_chevron_left, R.drawable.ic_play_arrow, R.drawable.ic_pause, R.drawable.ic_drag_indicator, R.drawable.ic_library_music, R.drawable.ic_more_horiz, R.drawable.ic_folder_open, R.drawable.ic_check, R.drawable.ic_music_note, R.drawable.ic_graphic_eq, R.drawable.ic_edit, R.drawable.ic_close, R.drawable.ic_skip_next, R.drawable.ic_description};
        for (int row = 0; row < 3; row++) {
            LinearLayout strip = Ui.row(this);
            for (int col = 0; col < 6; col++) { View icon = Ui.icon(this, icons[row * 6 + col], Ui.MUTED, 48); Ui.pad(icon, 12); strip.addView(icon); }
            left.addView(strip);
        }
        right.addView(Ui.eyebrow(this, "04 / ACTIONS + STATES")); Ui.gap(right, 12);
        right.addView(Ui.withIcon(Ui.button(this, "Start setlist", true, () -> {}), R.drawable.ic_play_arrow, false));
        right.addView(Ui.withIcon(Ui.button(this, "Import PDFs", false, () -> {}), R.drawable.ic_add, false));
        right.addView(Ui.quiet(this, "All setlists", R.drawable.ic_arrow_back, () -> {}));
        Button disabled = Ui.withIcon(Ui.button(this, "Previous", false, () -> {}), R.drawable.ic_chevron_left, false); disabled.setEnabled(false); right.addView(disabled);
        Ui.gap(right, 16); right.addView(Ui.eyebrow(this, "05 / SONG ROW")); Ui.gap(right, 12);
        LinearLayout song = Ui.row(this); Ui.card(song); song.addView(Ui.text(this, "01", Ui.TITLE, Ui.ACCENT));
        LinearLayout labels = Ui.column(this); labels.addView(Ui.heading(this, "Midnight Drive", Ui.TITLE)); Ui.gap(labels, 8);
        labels.addView(Ui.text(this, "3 pages · PDF + audio", Ui.CAPTION, Ui.MUTED)); Ui.weighted(song, labels); song.addView(Ui.icon(this, R.drawable.ic_drag_indicator, Ui.MUTED, 24)); right.addView(song);
        Ui.gap(right, 16); right.addView(Ui.text(this, "16 dp corners · 56 dp controls · 48 dp touch targets", 12, Ui.MUTED));        Ui.gap(page, 24); page.addView(Ui.eyebrow(this, "06 / EMPTY STATE"));
        page.addView(Ui.emptyState(this, R.drawable.illustration_empty_set, "The stage is yours", "Give your next gig a name. Then bring your songs together in playing order.", "Create your first setlist", () -> {}));

    }
}
