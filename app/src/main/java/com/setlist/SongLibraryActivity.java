package com.setlist;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

public final class SongLibraryActivity extends AppCompatActivity {
    private Library library;
    private String target, source;
    private TextInputEditText search;
    private LinearLayout results;
    private android.widget.Button filter;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); library = ((SetlistApp) getApplication()).library;
        target = getIntent().getStringExtra("set");
        if (target == null) { finish(); return; }
        source = saved == null ? null : saved.getString("source");
        LinearLayout root = Ui.column(this); root.setBackgroundColor(Ui.BG); setContentView(root); Ui.insets(root);
        LinearLayout page = Ui.column(this); Ui.pad(page, 16); root.addView(page, new LinearLayout.LayoutParams(-1, -1));
        page.addView(Ui.quiet(this, "Back to setlist", R.drawable.ic_arrow_back, this::finish));
        page.addView(Ui.heading(this, "Saved songs", Ui.HEADING)); Ui.gap(page, 8);
        page.addView(Ui.text(this, "Add to " + library.setName(target), Ui.CAPTION, Ui.MUTED)); Ui.gap(page, 16);
        TextInputLayout field = new TextInputLayout(this); field.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        field.setHint("Search name or metadata"); field.setStartIconDrawable(R.drawable.ic_search); field.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        search = new TextInputEditText(this); search.setSingleLine(true); field.addView(search); page.addView(field);
        filter = Ui.withIcon(Ui.button(this, "", false, this::chooseSource), R.drawable.ic_queue_music, false); page.addView(filter);
        ScrollView scroll = new ScrollView(this); results = Ui.column(this); scroll.addView(results); page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        search.setText(saved == null ? "" : saved.getString("query", ""));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { refresh(); }
            public void afterTextChanged(Editable e) {}
        }); refresh();
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("source", source); out.putString("query", search.getText().toString()); }
    private void chooseSource() {
        List<Models.Setlist> sets = library.sets(); String[] names = new String[sets.size() + 1]; names[0] = "All setlists";
        int selected = 0;
        for (int i = 0; i < sets.size(); i++) { names[i + 1] = sets.get(i).name(); if (sets.get(i).id().equals(source)) selected = i + 1; }
        new MaterialAlertDialogBuilder(this).setTitle("Filter by setlist").setSingleChoiceItems(names, selected, (dialog, which) -> {
            source = which == 0 ? null : sets.get(which - 1).id(); dialog.dismiss(); refresh();
        }).setNegativeButton("Cancel", null).show();
    }
    private void refresh() {
        filter.setText(source == null ? "All setlists" : library.setName(source)); results.removeAllViews();
        List<Models.Song> songs = library.searchSongs(source, search.getText().toString());
        if (songs.isEmpty()) {
            boolean empty = library.songs().isEmpty();
            results.addView(Ui.emptyState(this, empty ? R.drawable.illustration_empty_set : R.drawable.ic_search,
                empty ? "Your songs will live here" : "No songs found",
                empty ? "Import PDFs into a setlist to build your library. Then reuse them for your next gig." : "Try another title, artist, key, tempo or note, or search all setlists.",
                empty ? "Back to setlist" : "Clear search and filters", () -> { if (empty) finish(); else { source = null; search.setText(""); refresh(); } })); return;
        }
        TextView count = Ui.text(this, songs.size() + (songs.size() == 1 ? " song" : " songs"), Ui.CAPTION, Ui.MUTED); results.addView(count); Ui.gap(results, 12);
        for (Models.Song song : songs) {
            LinearLayout row = Ui.row(this); Ui.card(row); LinearLayout labels = Ui.column(this);
            labels.addView(Ui.heading(this, song.title(), Ui.TITLE)); Ui.gap(labels, 6);
            labels.addView(Ui.text(this, song.metadataLine().isEmpty() ? song.pages() + " pages" : song.metadataLine(), Ui.CAPTION, Ui.MUTED));
            Ui.weighted(row, labels); row.addView(Ui.icon(this, R.drawable.ic_add, Ui.ACCENT, 24));
            row.setContentDescription("Add " + song.title()); row.setFocusable(true); row.setOnClickListener(v -> { library.addSong(target, song.id()); finish(); });
            results.addView(row); Ui.gap(results, 8);
        }
    }
}
