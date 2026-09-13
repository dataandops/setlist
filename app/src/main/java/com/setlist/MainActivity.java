package com.setlist;

import android.app.Activity;
import android.app.AlertDialog;
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

public final class MainActivity extends Activity {
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
        LinearLayout.LayoutParams width = new LinearLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels, Ui.dp(this, 960)), -1);
        width.gravity = Gravity.CENTER_HORIZONTAL; root.addView(content, width); setContentView(root);
        if (android.os.Build.VERSION.SDK_INT >= 30) Ui.insets(root);
        if (setId == null) home(); else editor();
        if (app.busy) {
            TextView busy = Ui.text(this, "Opening selected files…", Ui.BODY, Ui.ACCENT); busy.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            content.addView(busy, 0); disable(content);
        }
        if (app.message != null) {
            String message = app.message; app.message = null;
            new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", null).show();
        }
    }
    private void disable(View view) {
        view.setEnabled(false);
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) disable(group.getChildAt(i));
    }
    private void home() {
        content.addView(Ui.text(this, "YOUR STAGE, IN ORDER", Ui.CAPTION, Ui.ACCENT)); Ui.gap(content, 8);
        content.addView(Ui.heading(this, "Setlist", Ui.DISPLAY));
        content.addView(Ui.text(this, "The music. In the right order.", Ui.BODY, Ui.MUTED)); Ui.gap(content, 24);
        content.addView(Ui.button(this, "+ New setlist", true, () -> nameDialog("New setlist", "", name -> { setId = app.library.createSet(name); refresh(); })));
        Ui.gap(content, Ui.BODY);
        List<Models.Setlist> sets = app.library.sets();
        ScrollView scroll = new ScrollView(this); LinearLayout list = Ui.column(this); scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (sets.isEmpty()) {
            Ui.gap(list, 24); list.addView(Ui.heading(this, "Ready for your next gig", Ui.HEADING)); Ui.gap(list, 12);
            list.addView(Ui.text(this, "Create a setlist, select your PDFs, and put your songs in playing order.", Ui.BODY, Ui.MUTED));
        }
        for (Models.Setlist set : sets) {
            LinearLayout card = Ui.column(this); Ui.card(card);
            card.addView(Ui.heading(this, set.name(), Ui.TITLE)); Ui.gap(card, 8);
            card.addView(Ui.text(this, set.count() + (set.count() == 1 ? " song" : " songs") + "  ·  On this device", Ui.CAPTION, Ui.MUTED));
            card.setContentDescription("Open setlist " + set.name()); card.setFocusable(true); card.setOnClickListener(v -> { setId = set.id(); refresh(); });
            list.addView(card); Ui.gap(list, 12);
        }
        content.addView(Ui.button(this, "Try a sample set", false, () -> app.work(() -> { SampleScores.create(this, app.library); return "Sample set ready. Open it to explore three original sample charts."; })));
        content.addView(Ui.text(this, "Offline. No account. Your files stay where they are.", Ui.CAPTION, Ui.MUTED));
    }
    private void editor() {
        LinearLayout top = Ui.row(this);
        Ui.weighted(top, Ui.button(this, "‹ All setlists", false, () -> { setId = null; refresh(); }));
        Ui.weighted(top, Ui.button(this, "Set options", false, this::setOptions)); content.addView(top);
        Ui.gap(content, Ui.BODY); content.addView(Ui.heading(this, app.library.setName(setId), Ui.HEADING));
        entries = app.library.entries(setId);
        content.addView(Ui.text(this, entries.size() + " songs  ·  Hold ≡ to drag into order", Ui.CAPTION, Ui.MUTED)); Ui.gap(content, Ui.BODY);
        LinearLayout actions = Ui.row(this);
        Ui.weighted(actions, Ui.button(this, "+ Import PDFs", true, () -> pick(PDFS, null)));
        Ui.weighted(actions, Ui.button(this, "Add saved song", false, this::addSaved)); content.addView(actions);
        Ui.gap(content, 8);
        if (entries.isEmpty()) {
            LinearLayout empty = Ui.column(this); Ui.pad(empty, 24);
            empty.addView(Ui.heading(this, "Your first song goes here", Ui.TITLE)); Ui.gap(empty, 12);
            empty.addView(Ui.text(this, "Select one or more PDFs from device storage. Their filenames become song titles. Files open in place.", Ui.BODY, Ui.MUTED));
            content.addView(empty, new LinearLayout.LayoutParams(-1, 0, 1));
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
        Button start = Ui.button(this, "Start setlist  ▶", true, () -> startConcert(0)); start.setEnabled(!entries.isEmpty()); content.addView(start);
    }
    private void startConcert(int index) {
        startActivity(new Intent(this, ConcertActivity.class).putExtra("set", setId).putExtra("song", index));
    }
    private void setOptions() {
        new AlertDialog.Builder(this).setTitle("Set options").setItems(new String[]{"Rename setlist", "Delete setlist"}, (d, choice) -> {
            if (choice == 0) nameDialog("Rename setlist", app.library.setName(setId), name -> { app.library.renameSet(setId, name); refresh(); });
            else new AlertDialog.Builder(this).setTitle("Delete this setlist?").setMessage("Songs remain in your saved library. Original files are never deleted.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> { app.library.deleteSet(setId); setId = null; refresh(); }).show();
        }).show();
    }
    private void addSaved() {
        List<Models.Song> songs = app.library.songs();
        if (songs.isEmpty()) { new AlertDialog.Builder(this).setMessage("Import PDFs first. You can then reuse these songs in any setlist.").setPositiveButton("OK", null).show(); return; }
        String[] names = new String[songs.size()]; for (int i = 0; i < names.length; i++) names[i] = songs.get(i).title();
        new AlertDialog.Builder(this).setTitle("Add a saved song").setItems(names, (d, i) -> { app.library.addSong(setId, songs.get(i).id()); refresh(); }).setNegativeButton("Cancel", null).show();
    }
    private void songOptions(int position) {
        if (position < 0 || position >= entries.size()) return;
        Models.Entry entry = entries.get(position); Models.Song song = entry.song();
        String[] options = {"Open in concert", "Rename song", song.audio() == null ? "Attach MP3" : "Replace audio", "Locate / replace PDF", "Move up", "Move down", "Remove from setlist", "Song details"};
        new AlertDialog.Builder(this).setTitle(song.title()).setItems(options, (d, choice) -> {
            switch (choice) {
                case 0 -> startConcert(position);
                case 1 -> nameDialog("Rename song", song.title(), name -> { app.library.renameSong(song.id(), name); refresh(); });
                case 2 -> pick(AUDIO, song.id());
                case 3 -> pick(REPLACE, song.id());
                case 4, 5 -> { Models.move(entries, position, position + (choice == 4 ? -1 : 1)); app.library.reorder(setId, entries); refresh(); }
                case 6 -> new AlertDialog.Builder(this).setMessage("Remove “" + song.title() + "” from this setlist? The saved song and original files stay available.").setNegativeButton("Cancel", null).setPositiveButton("Remove", (dialog, which) -> { app.library.removeEntry(entry.id()); refresh(); }).show();
                case 7 -> {
                    AlertDialog.Builder details = new AlertDialog.Builder(this).setTitle("Song details").setMessage("Song ID\n" + song.id() + "\n\nSetlist entry ID\n" + entry.id() + "\n\nPDF\n" + song.pdf() + "\n\nAudio\n" + (song.audio() == null ? "None attached" : song.audio())).setPositiveButton("Close", null);
                    if (song.audio() != null) details.setNeutralButton("Detach audio", (dialog, which) -> { app.library.removeAudio(song); refresh(); });
                    details.show();
                }
            }
        }).show();
    }
    private interface NameAction { void accept(String name); }
    private void nameDialog(String title, String initial, NameAction action) {
        EditText input = new EditText(this); input.setSingleLine(true); input.setText(initial); input.setSelectAllOnFocus(true); input.setHint("Name");
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)});
        LinearLayout box = Ui.column(this); Ui.pad(box, 24); box.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
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
            TextView handle = Ui.text(MainActivity.this, "≡", Ui.HEADING, Ui.MUTED); handle.setGravity(Gravity.CENTER); row.addView(handle, new LinearLayout.LayoutParams(Ui.dp(MainActivity.this, 48), Ui.dp(MainActivity.this, 56)));
            return new SongHolder(row, number, title, subtitle, handle);
        }
        @Override public void onBindViewHolder(SongHolder h, int position) {
            Models.Song song = entries.get(position).song(); h.number.setText(String.format(java.util.Locale.US, "%02d", position + 1));
            h.title.setText(song.title()); h.subtitle.setText(getString(song.audio() == null ? R.string.song_pdf : R.string.song_audio, song.pages()));
            h.itemView.setContentDescription("Song " + (position + 1) + ": " + song.title() + ". Options"); h.itemView.setFocusable(true);
            h.itemView.setOnClickListener(v -> songOptions(h.getBindingAdapterPosition()));
            h.handle.setContentDescription("Reorder " + song.title()); h.handle.setFocusable(true);
            h.handle.setOnLongClickListener(v -> { drag.startDrag(h); return true; });
            h.handle.setOnClickListener(v -> songOptions(h.getBindingAdapterPosition()));
        }
        @Override public int getItemCount() { return entries.size(); }
    }
    private static final class SongHolder extends RecyclerView.ViewHolder {
        final TextView number, title, subtitle, handle;
        SongHolder(View view, TextView number, TextView title, TextView subtitle, TextView handle) { super(view); this.number = number; this.title = title; this.subtitle = subtitle; this.handle = handle; }
    }
}
