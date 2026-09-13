package com.setlist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Library extends SQLiteOpenHelper {
    private final Context context;

    public Library(Context context) { this(context, "setlist.db", "scores"); }
    Library(Context context, String database, String directory) {
        super(context, database, null, 2);
        this.context = context.getApplicationContext();
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE setlists (id TEXT PRIMARY KEY, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE songs (id TEXT PRIMARY KEY, title TEXT NOT NULL, pdf TEXT NOT NULL, audio TEXT, pages INTEGER NOT NULL, artist TEXT NOT NULL DEFAULT '', musical_key TEXT NOT NULL DEFAULT '', bpm TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY, setlist_id TEXT NOT NULL REFERENCES setlists(id) ON DELETE CASCADE, song_id TEXT NOT NULL REFERENCES songs(id), position INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX entry_order ON entries(setlist_id, position)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            for (String column : new String[]{"artist", "musical_key", "bpm", "notes"}) db.execSQL("ALTER TABLE songs ADD COLUMN " + column + " TEXT NOT NULL DEFAULT ''");
        } else throw new IllegalStateException("No migration from " + oldVersion + " to " + newVersion);
    }
    private static String id() { return UUID.randomUUID().toString(); }
    private static String name(String value) {
        String result = value.trim();
        if (result.isEmpty()) throw new IllegalArgumentException("Please enter a name.");
        return result;
    }
    public String createSet(String title) {
        String id = id();
        ContentValues values = new ContentValues(); values.put("id", id); values.put("name", name(title));
        getWritableDatabase().insertOrThrow("setlists", null, values);
        return id;
    }
    public void renameSet(String id, String title) {
        ContentValues values = new ContentValues(); values.put("name", name(title));
        getWritableDatabase().update("setlists", values, "id=?", new String[]{id});
    }
    public List<Models.Setlist> sets() {
        List<Models.Setlist> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT s.id,s.name,COUNT(e.id) FROM setlists s LEFT JOIN entries e ON e.setlist_id=s.id GROUP BY s.id ORDER BY s.rowid DESC", null)) {
            while (c.moveToNext()) result.add(new Models.Setlist(c.getString(0), c.getString(1), c.getInt(2)));
        }
        return result;
    }
    public String setName(String id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT name FROM setlists WHERE id=?", new String[]{id})) {
            return c.moveToFirst() ? c.getString(0) : "Setlist";
        }
    }
    public List<Models.Entry> entries(String setId) {
        List<Models.Entry> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT e.id,s.id,s.title,s.pdf,s.audio,s.pages,s.artist,s.musical_key,s.bpm,s.notes FROM entries e JOIN songs s ON e.song_id=s.id WHERE e.setlist_id=? ORDER BY e.position,e.rowid", new String[]{setId})) {
            while (c.moveToNext()) result.add(new Models.Entry(c.getString(0), readSong(c, 1)));
        }
        return result;
    }
    public List<Models.Song> songs() {
        List<Models.Song> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,title,pdf,audio,pages,artist,musical_key,bpm,notes FROM songs ORDER BY title COLLATE NOCASE", null)) {
            while (c.moveToNext()) result.add(readSong(c, 0));
        }
        return result;
    }
    private static Models.Song readSong(Cursor c, int offset) {
        return new Models.Song(c.getString(offset), c.getString(offset + 1), c.getString(offset + 2), c.getString(offset + 3), c.getInt(offset + 4), c.getString(offset + 5), c.getString(offset + 6), c.getString(offset + 7), c.getString(offset + 8));
    }
    public List<Models.Song> searchSongs(String sourceSet, String query) {
        java.util.Set<String> allowed = new java.util.HashSet<>();
        if (sourceSet != null) for (Models.Entry entry : entries(sourceSet)) allowed.add(entry.song().id());
        String[] words = normalized(query).split("\\s+"); List<Models.Song> result = new ArrayList<>();
        for (Models.Song song : songs()) {
            if (sourceSet != null && !allowed.contains(song.id())) continue;
            String haystack = normalized(song.title() + " " + song.artist() + " " + song.key() + " " + song.bpm() + " bpm " + song.notes() + " " + song.id() + " " + Uri.decode(song.pdf()) + " " + (song.audio() == null ? "" : Uri.decode(song.audio())));
            boolean matches = true;
            for (String word : words) if (!haystack.contains(word)) { matches = false; break; }
            if (matches) result.add(song);
        }
        return result;
    }
    private static String normalized(String text) {
        return java.text.Normalizer.normalize(text.trim(), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }
    public void updateMetadata(String songId, String artist, String key, String bpm, String notes) {
        String tempo = bpm.trim();
        if (!tempo.isEmpty()) {
            try { if (Integer.parseInt(tempo) <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a positive whole number for BPM, or leave it empty."); }
        }
        ContentValues values = new ContentValues(); values.put("artist", artist.trim()); values.put("musical_key", key.trim()); values.put("bpm", tempo); values.put("notes", notes.trim());
        getWritableDatabase().update("songs", values, "id=?", new String[]{songId});
    }
    public void addSong(String setId, String songId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int next;
            try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM entries WHERE setlist_id=?", new String[]{setId})) { c.moveToFirst(); next = c.getInt(0); }
            ContentValues values = new ContentValues(); values.put("id", id()); values.put("setlist_id", setId);
            values.put("song_id", songId); values.put("position", next);
            db.insertOrThrow("entries", null, values); db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    public void reorder(String setId, List<Models.Entry> entries) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            List<Models.Entry> existing = entries(setId);
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (Models.Entry e : existing) ids.add(e.id());
            if (entries.size() != ids.size()) throw new IllegalArgumentException("The setlist changed. Please try again.");
            for (int i = 0; i < entries.size(); i++) {
                if (!ids.remove(entries.get(i).id())) throw new IllegalArgumentException("Invalid setlist order");
                ContentValues values = new ContentValues(); values.put("position", i);
                db.update("entries", values, "id=? AND setlist_id=?", new String[]{entries.get(i).id(), setId});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    public void removeEntry(String entryId) { getWritableDatabase().delete("entries", "id=?", new String[]{entryId}); }
    public void deleteSet(String setId) { getWritableDatabase().delete("setlists", "id=?", new String[]{setId}); }
    public void renameSong(String songId, String title) {
        ContentValues values = new ContentValues(); values.put("title", name(title));
        getWritableDatabase().update("songs", values, "id=?", new String[]{songId});
    }
    public String displayName(Uri uri) {
        try (Cursor c = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (RuntimeException ignored) { /* Providers may not expose a display name. */ }
        return "file".equals(uri.getScheme()) && uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "Untitled song";
    }
    public Models.Song importPdf(String setId, Uri uri) throws IOException {
        try {
            int pages;
            try (ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                 PdfRenderer pdf = new PdfRenderer(descriptor)) { pages = pdf.getPageCount(); }
            if (pages == 0) throw new IOException("This PDF has no pages.");
            String title = displayName(uri).replaceFirst("(?i)\\.pdf$", "").trim();
            if (title.isEmpty()) title = "Untitled song";
            Models.Song song = new Models.Song(id(), title, uri.toString(), null, pages);
            SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
            try {
                ContentValues values = new ContentValues(); values.put("id", song.id()); values.put("title", song.title());
                values.put("pdf", song.pdf()); values.put("pages", pages);
                db.insertOrThrow("songs", null, values); addSong(setId, song.id()); db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            return song;
        } catch (Exception e) { throw new IOException("Cannot import " + displayName(uri) + ". Choose an unprotected, readable PDF.", e); }
    }
    public void attachAudio(Models.Song song, Uri uri) throws IOException {
        android.media.MediaMetadataRetriever probe = new android.media.MediaMetadataRetriever();
        try {
            probe.setDataSource(context, uri);
            String duration = probe.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration == null || Long.parseLong(duration) <= 0) throw new IOException("Choose a playable audio file.");
            ContentValues values = new ContentValues(); values.put("audio", uri.toString());
            if (getWritableDatabase().update("songs", values, "id=?", new String[]{song.id()}) != 1) throw new IOException("Song no longer exists.");
        } catch (Exception e) { throw new IOException("Cannot attach this audio file. Choose a playable MP3.", e); }
        finally { probe.release(); }
    }
    public void replacePdf(Models.Song song, Uri uri) throws IOException {
        try (ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "r");
             PdfRenderer pdf = new PdfRenderer(descriptor)) {
            if (pdf.getPageCount() == 0) throw new IOException("This PDF has no pages.");
            ContentValues values = new ContentValues(); values.put("pdf", uri.toString()); values.put("pages", pdf.getPageCount());
            getWritableDatabase().update("songs", values, "id=?", new String[]{song.id()});
        } catch (Exception e) { throw new IOException("Cannot open this PDF. Choose a readable, unprotected file stored on this device.", e); }
    }
    public void removeAudio(Models.Song song) {
        ContentValues values = new ContentValues(); values.putNull("audio");
        getWritableDatabase().update("songs", values, "id=?", new String[]{song.id()});
    }
}
