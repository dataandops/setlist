package com.setlist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends AppCompatActivity {
    private static final int PDFS = 10, AUDIO = 11, REPLACE = 12;
    private SetlistApp app;
    private String setId, pendingSongId, pendingSetId;
    private LinearLayout content;
    private List<Models.Entry> entries = new ArrayList<>();
    private ItemTouchHelper drag;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); app = (SetlistApp) getApplication();
        if (saved != null) {
            setId = saved.getString("set"); pendingSongId = saved.getString("pendingSong"); pendingSetId = saved.getString("pendingSet");
        }
    }
    @Override protected void onResume() { super.onResume(); app.listener = this::refresh; refresh(); }
    @Override protected void onPause() { app.listener = null; super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putString("set", setId); out.putString("pendingSong", pendingSongId); out.putString("pendingSet", pendingSetId);
    }
    @Override public void onBackPressed() {
        if (setId != null) { setId = null; refresh(); } else super.onBackPressed();
    }
    private void refresh() {
        LinearLayout root = Ui.column(this); root.setBackgroundColor(Ui.BG);
        content = Ui.column(this); Ui.pad(content, Ui.BODY);
        // Fill the space left after system-bar/cutout insets; only cap the column on screens wider than 960 dp.
        int maxWidth = Ui.dp(this, 960);
        LinearLayout.LayoutParams width = new LinearLayout.LayoutParams(getResources().getDisplayMetrics().widthPixels > maxWidth ? maxWidth : -1, -1);
        width.gravity = Gravity.CENTER_HORIZONTAL;
        if (homeScrolls()) {
            ScrollView page = new ScrollView(this); page.setFillViewport(true);
            page.addView(content, new ViewGroup.LayoutParams(-1, -2)); root.addView(page, width);
        } else root.addView(content, width);
        setContentView(root);
        if (android.os.Build.VERSION.SDK_INT >= 30) Ui.insets(root);
        if (setId == null) home(); else editor();
        if (app.busy) {
            TextView busy = Ui.text(this, "Opening selected files…", Ui.BODY, Ui.ACCENT); busy.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            content.addView(busy, 0); disable(content);
        }
        if (app.message != null) {
            String message = app.message; app.message = null;
            new MaterialAlertDialogBuilder(this).setMessage(message).setPositiveButton("OK", null).show();
        }
    }
    // On short screens Home scrolls as one page; the editor keeps its drag list and condenses its header instead.
    private boolean homeScrolls() { return setId == null && Ui.shortScreen(this); }
    private void disable(View view) {
        view.setEnabled(false);
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) disable(group.getChildAt(i));
    }
    private void home() {
        LinearLayout brand = Ui.row(this); brand.addView(Ui.brand(this));
        TextView wordmark = Ui.heading(this, "Setlist", Ui.TITLE); Ui.weighted(brand, wordmark); brand.addView(Ui.badge(this, "OFFLINE")); content.addView(brand);
        Ui.gap(content, 24); content.addView(Ui.heading(this, "Your next great set.", Ui.DISPLAY)); Ui.gap(content, 8);
        content.addView(Ui.text(this, "Your music, ready for the stage.", Ui.BODY, Ui.MUTED)); Ui.gap(content, 24);
        if (!app.library.sets().isEmpty()) content.addView(Ui.withIcon(Ui.button(this, "New setlist", true, () -> nameDialog("New setlist", "", name -> { setId = app.library.createSet(name); refresh(); })), R.drawable.ic_add, false));
        Ui.gap(content, 24); content.addView(Ui.eyebrow(this, "YOUR SETLISTS")); Ui.gap(content, 12);
        List<Models.Setlist> sets = app.library.sets();
        LinearLayout list = Ui.column(this);
        if (homeScrolls()) content.addView(list);
        else { ScrollView scroll = new ScrollView(this); scroll.addView(list); content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); }
        if (sets.isEmpty()) {
            list.addView(Ui.emptyState(this, R.drawable.illustration_empty_set, "The stage is yours",
                "Give your next gig a name. Then bring your songs together in playing order.", "Create your first setlist",
                () -> nameDialog("New setlist", "", name -> { setId = app.library.createSet(name); refresh(); })));
        }
        for (Models.Setlist set : sets) {
            LinearLayout card = Ui.row(this); Ui.card(card); card.addView(Ui.icon(this, R.drawable.ic_queue_music, Ui.ACCENT, 32));
            LinearLayout labels = Ui.column(this); labels.addView(Ui.heading(this, set.name(), Ui.TITLE)); Ui.gap(labels, 8);
            labels.addView(Ui.text(this, set.count() + (set.count() == 1 ? " song" : " songs") + "  ·  On this device", Ui.CAPTION, Ui.MUTED));
            Ui.weighted(card, labels); card.addView(Ui.icon(this, R.drawable.ic_chevron_right, Ui.MUTED, 24));
            card.setContentDescription("Open setlist " + set.name()); card.setFocusable(true); card.setOnClickListener(v -> { setId = set.id(); refresh(); });
            list.addView(card); Ui.gap(list, 12);
        }
        Ui.gap(content, 12);
        content.addView(Ui.withIcon(Ui.button(this, "Try a sample set", false, () -> app.work(() -> { SampleScores.create(this, app.library); return "Sample set ready. Open it to explore three original sample charts."; })), R.drawable.ic_play_arrow, false));
        Ui.gap(content, 8); TextView footer = Ui.text(this, "No account. Your files stay yours.", 12, Ui.MUTED); footer.setGravity(Gravity.CENTER); content.addView(footer);
    }
    private void editor() {
        boolean condensed = Ui.shortScreen(this);
        entries = app.library.entries(setId);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.quiet(this, "All setlists", R.drawable.ic_arrow_back, () -> { setId = null; refresh(); }), new LinearLayout.LayoutParams(-2, -2));
        if (condensed) {
            // Title shares the top row so the running order keeps most of the height.
            TextView name = Ui.heading(this, app.library.setName(setId), Ui.TITLE); name.setSingleLine(true); name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            Ui.pad(name, 8); top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        } else top.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1));
        top.addView(Ui.quiet(this, "Options", R.drawable.ic_more_horiz, this::setOptions), new LinearLayout.LayoutParams(-2, -2)); content.addView(top);
        if (!condensed) {
            Ui.gap(content, 16); content.addView(Ui.eyebrow(this, "THE RUNNING ORDER"));
            Ui.gap(content, Ui.BODY); content.addView(Ui.heading(this, app.library.setName(setId), Ui.HEADING));
            content.addView(Ui.text(this, entries.size() + " songs  ·  Hold a handle to reorder", Ui.CAPTION, Ui.MUTED)); Ui.gap(content, Ui.BODY);
        }
        LinearLayout actions = Ui.row(this);
        if (!entries.isEmpty()) Ui.weighted(actions, Ui.withIcon(Ui.button(this, "Import PDFs", false, () -> pick(PDFS, null)), R.drawable.ic_add, false));
        Ui.weighted(actions, Ui.withIcon(Ui.button(this, "Saved songs", false, this::addSaved), R.drawable.ic_library_music, false)); content.addView(actions);
        Button start = Ui.withIcon(Ui.button(this, "Start setlist", true, () -> startConcert(0)), R.drawable.ic_play_arrow, false); start.setEnabled(!entries.isEmpty());
        if (condensed) Ui.weighted(actions, start);
        Ui.gap(content, 8);
        if (entries.isEmpty()) {
            ScrollView emptyScroll = new ScrollView(this);
            emptyScroll.addView(Ui.emptyState(this, R.drawable.illustration_empty_set, "Every set starts with a song",
                "Choose your PDF charts from device storage, or reuse a saved song. Your original files stay where they are.", "Import PDFs", () -> pick(PDFS, null)));
            content.addView(emptyScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            RecyclerView list = new RecyclerView(this); list.setLayoutManager(new LinearLayoutManager(this));
            SongAdapter adapter = new SongAdapter(); list.setAdapter(adapter);
            drag = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
                    int a = from.getBindingAdapterPosition(), b = to.getBindingAdapterPosition();
                    if (a < 0 || b < 0) return false;
                    Models.move(entries, a, b); app.library.reorder(setId, entries); adapter.notifyItemMoved(a, b); return true;
                }
                @Override public void onSwiped(RecyclerView.ViewHolder holder, int direction) {}
                @Override public void clearView(RecyclerView rv, RecyclerView.ViewHolder holder) {
                    super.clearView(rv, holder); adapter.notifyItemRangeChanged(0, entries.size());
                }
            });
            drag.attachToRecyclerView(list); content.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        if (!condensed) content.addView(start);
    }
    private void startConcert(int index) {
        startActivity(new Intent(this, ConcertActivity.class).putExtra("set", setId).putExtra("song", index));
    }
    private void setOptions() {
        new MaterialAlertDialogBuilder(this).setTitle("Set options").setItems(new String[]{"Rename setlist", "Delete setlist"}, (d, choice) -> {
            if (choice == 0) nameDialog("Rename setlist", app.library.setName(setId), name -> { app.library.renameSet(setId, name); refresh(); });
            else new MaterialAlertDialogBuilder(this).setTitle("Delete this setlist?").setMessage("Songs remain in your saved library. Original files are never deleted.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> { app.library.deleteSet(setId); setId = null; refresh(); }).show();
        }).show();
    }
    private void addSaved() {
        startActivity(new Intent(this, SongLibraryActivity.class).putExtra("set", setId));
    }
    private void songOptions(int position) {
        if (position < 0 || position >= entries.size()) return;
        Models.Entry entry = entries.get(position); Models.Song song = entry.song();
        String[] options = {"Open in concert", "Rename song", song.audio() == null ? "Attach MP3" : "Replace audio", "Locate / replace PDF", "Move up", "Move down", "Remove from setlist", "Song details", "Edit metadata"};
        new MaterialAlertDialogBuilder(this).setTitle(song.title()).setItems(options, (d, choice) -> {
            switch (choice) {
                case 0 -> startConcert(position);
                case 1 -> nameDialog("Rename song", song.title(), name -> { app.library.renameSong(song.id(), name); refresh(); });
                case 2 -> pick(AUDIO, song.id());
                case 3 -> pick(REPLACE, song.id());
                case 4, 5 -> { Models.move(entries, position, position + (choice == 4 ? -1 : 1)); app.library.reorder(setId, entries); refresh(); }
                case 6 -> new MaterialAlertDialogBuilder(this).setMessage("Remove “" + song.title() + "” from this setlist? The saved song and original files stay available.").setNegativeButton("Cancel", null).setPositiveButton("Remove", (dialog, which) -> { app.library.removeEntry(entry.id()); refresh(); }).show();
                case 8 -> SongMetadataDialog.show(this, app.library, song, this::refresh);
                case 7 -> {
                    AlertDialog.Builder details = new MaterialAlertDialogBuilder(this).setTitle("Song details").setMessage("Song ID\n" + song.id() + "\n\nSetlist entry ID\n" + entry.id() + "\n\nPDF\n" + song.pdf() + "\n\nAudio\n" + (song.audio() == null ? "None attached" : song.audio())).setPositiveButton("Close", null);
                    if (song.audio() != null) details.setNeutralButton("Detach audio", (dialog, which) -> { app.library.removeAudio(song); refresh(); });
                    details.show();
                }
            }
        }).show();
    }
    private interface NameAction { void accept(String name); }
    private void nameDialog(String title, String initial, NameAction action) {
        EditText input = new com.google.android.material.textfield.TextInputEditText(this); input.setSingleLine(true); input.setText(initial); input.setSelectAllOnFocus(true); input.setHint("Name");
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)});
        LinearLayout box = Ui.column(this); Ui.pad(box, 24);
        com.google.android.material.textfield.TextInputLayout field = new com.google.android.material.textfield.TextInputLayout(this);
        field.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE); field.setHint("Name"); field.addView(input); box.addView(field);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(title).setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (input.getText().toString().trim().isEmpty()) { input.setError("Enter a name"); return; }
            action.accept(input.getText().toString()); dialog.dismiss();
        })); dialog.show();
    }
    private void pick(int request, String songId) {
        pendingSongId = songId; pendingSetId = setId;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType(request == AUDIO ? "audio/*" : "application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, request == PDFS);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, request);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || (request != PDFS && request != AUDIO && request != REPLACE)) return;
        ArrayList<Uri> uris = new ArrayList<>(); ClipData clip = data.getClipData();
        if (clip != null) for (int i = 0; i < clip.getItemCount(); i++) uris.add(clip.getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        String targetSet = pendingSetId, targetSong = pendingSongId;
        app.work(() -> {
            int success = 0; List<String> failures = new ArrayList<>();
            for (Uri uri : uris) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (request == PDFS) app.library.importPdf(targetSet, uri);
                    else {
                        Models.Song song = app.library.songs().stream().filter(s -> s.id().equals(targetSong)).findFirst().orElseThrow(() -> new IllegalStateException("Song no longer exists."));
                        if (request == AUDIO) app.library.attachAudio(song, uri); else app.library.replacePdf(song, uri);
                    }
                    success++;
                } catch (Exception e) { failures.add(app.library.displayName(uri) + ": " + (e.getMessage() == null ? "Could not open file." : e.getMessage())); }
            }
            return success + (request == PDFS ? " PDF(s) added." : " file(s) linked.") + (failures.isEmpty() ? "" : "\n\n" + String.join("\n", failures));
        });
    }
    private final class SongAdapter extends RecyclerView.Adapter<SongHolder> {
        @Override public SongHolder onCreateViewHolder(ViewGroup parent, int type) {
            LinearLayout row = Ui.row(MainActivity.this); Ui.card(row);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2); params.bottomMargin = Ui.dp(MainActivity.this, 8); row.setLayoutParams(params);
            TextView number = Ui.text(MainActivity.this, "", Ui.TITLE, Ui.ACCENT); row.addView(number, new LinearLayout.LayoutParams(Ui.dp(MainActivity.this, 40), -2));
            LinearLayout labels = Ui.column(MainActivity.this); TextView title = Ui.heading(MainActivity.this, "", Ui.TITLE); TextView subtitle = Ui.text(MainActivity.this, "", Ui.CAPTION, Ui.MUTED);
            labels.addView(title); Ui.gap(labels, 4); labels.addView(subtitle); Ui.weighted(row, labels);
            android.widget.ImageView handle = Ui.icon(MainActivity.this, R.drawable.ic_drag_indicator, Ui.MUTED, 24); Ui.pad(handle, 12); row.addView(handle, new LinearLayout.LayoutParams(Ui.dp(MainActivity.this, 48), Ui.dp(MainActivity.this, 56)));
            return new SongHolder(row, number, title, subtitle, handle);
        }
        @Override public void onBindViewHolder(SongHolder h, int position) {
            Models.Song song = entries.get(position).song(); h.number.setText(String.format(java.util.Locale.US, "%02d", position + 1));
            h.title.setText(song.title()); h.subtitle.setText((song.metadataLine().isEmpty() ? "" : song.metadataLine() + " · ") + getString(song.audio() == null ? R.string.song_pdf : R.string.song_audio, song.pages()));
            h.itemView.setContentDescription("Song " + (position + 1) + ": " + song.title() + ". Options"); h.itemView.setFocusable(true);
            h.itemView.setOnClickListener(v -> songOptions(h.getBindingAdapterPosition()));
            h.handle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES); h.handle.setContentDescription("Reorder " + song.title()); h.handle.setFocusable(true);
            h.handle.setOnLongClickListener(v -> { drag.startDrag(h); return true; });
            h.handle.setOnClickListener(v -> songOptions(h.getBindingAdapterPosition()));
        }
        @Override public int getItemCount() { return entries.size(); }
    }
    private static final class SongHolder extends RecyclerView.ViewHolder {
        final TextView number, title, subtitle; final android.widget.ImageView handle;
        SongHolder(View view, TextView number, TextView title, TextView subtitle, android.widget.ImageView handle) { super(view); this.number = number; this.title = title; this.subtitle = subtitle; this.handle = handle; }
    }
}
