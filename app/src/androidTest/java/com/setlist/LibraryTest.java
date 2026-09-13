package com.setlist;

import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LibraryTest {
    private Context context;
    private Library library;
    private File source;
    @Before public void setup() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("test-library.db"); library = new Library(context, "test-library.db", "unused");
        source = new File(context.getCacheDir(), "Test score.pdf");
        PdfDocument document = new PdfDocument();
        try {
            for (int i = 0; i < 3; i++) {
                PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(612, 792, i + 1).create());
                page.getCanvas().drawColor(android.graphics.Color.WHITE); document.finishPage(page);
            }
            try (FileOutputStream out = new FileOutputStream(source)) { document.writeTo(out); }
        } finally { document.close(); }
    }
    @After public void cleanup() { library.close(); context.deleteDatabase("test-library.db"); source.delete(); }
    @Test public void importLinksOriginalAndSurvivesDatabaseReopen() throws Exception {
        String set = library.createSet("Gig"); Models.Song song = library.importPdf(set, Uri.fromFile(source));
        assertEquals(Uri.fromFile(source).toString(), song.pdf()); assertEquals(3, song.pages());
        library.close(); library = new Library(context, "test-library.db", "unused");
        assertEquals(song.id(), library.entries(set).get(0).song().id());
        library.deleteSet(set); assertTrue(source.exists()); assertEquals(1, library.songs().size());
    }
    @Test public void reorderAndRemoveKeepStableUniqueEntries() throws Exception {
        String set = library.createSet("Gig"); Models.Song song = library.importPdf(set, Uri.fromFile(source));
        library.addSong(set, song.id()); library.addSong(set, song.id());
        List<Models.Entry> list = new ArrayList<>(library.entries(set));
        String first = list.get(0).id(), last = list.get(2).id(); assertNotEquals(first, last);
        Models.move(list, 2, 0); library.reorder(set, list);
        assertEquals(last, library.entries(set).get(0).id());
        library.removeEntry(first); library.addSong(set, song.id());
        List<Models.Entry> updated = library.entries(set);
        assertEquals(3, updated.size()); assertEquals(last, updated.get(0).id());
    }
    @Test public void invalidReorderRollsBackAndCannotMoveAnotherSetsEntries() throws Exception {
        String set = library.createSet("One"), other = library.createSet("Two");
        Models.Song song = library.importPdf(set, Uri.fromFile(source)); library.addSong(other, song.id());
        String original = library.entries(set).get(0).id();
        try { library.reorder(set, library.entries(other)); fail("Expected invalid order rejection"); }
        catch (IllegalArgumentException expected) { assertEquals(original, library.entries(set).get(0).id()); }
    }
    @Test public void invalidPdfDoesNotAddASongOrAnEntry() throws Exception {
        String set = library.createSet("Gig"); File invalid = new File(context.getCacheDir(), "invalid.pdf");
        try {
            try (FileOutputStream out = new FileOutputStream(invalid)) { out.write("not a pdf".getBytes()); }
            try { library.importPdf(set, Uri.fromFile(invalid)); fail("Expected invalid PDF rejection"); }
            catch (java.io.IOException expected) { assertTrue(library.entries(set).isEmpty()); assertTrue(library.songs().isEmpty()); }
            assertTrue(invalid.exists());
        } finally { invalid.delete(); }
    }
    @Test public void relinkPreservesSongIdAndOrder() throws Exception {
        String set = library.createSet("Gig"); Models.Song song = library.importPdf(set, Uri.fromFile(source));
        String entryId = library.entries(set).get(0).id();
        library.replacePdf(song, Uri.fromFile(source)); library.renameSong(song.id(), "New title");
        Models.Entry entry = library.entries(set).get(0);
        assertEquals(entryId, entry.id()); assertEquals(song.id(), entry.song().id()); assertEquals("New title", entry.song().title());
    }
    @Test public void metadataSearchCombinesFieldsAndSetlistFilter() throws Exception {
        String one = library.createSet("One"), two = library.createSet("Two");
        Models.Song song = library.importPdf(one, Uri.fromFile(source));
        library.renameSong(song.id(), "Midnight Drive");
        library.updateMetadata(song.id(), "Beyoncé", "F#m", "120", "Piano intro");
        assertEquals(1, library.searchSongs(one, "BEYONCE piano 120").size());
        assertEquals(1, library.searchSongs(null, "midnight F#m").size());
        assertTrue(library.searchSongs(two, "midnight").isEmpty());
        assertTrue(library.searchSongs(one, "missing").isEmpty());
        library.addSong(two, song.id()); library.addSong(two, song.id());
        assertEquals(1, library.searchSongs(two, "intro").size());
        library.close(); library = new Library(context, "test-library.db", "unused");
        assertEquals("Beyoncé", library.entries(one).get(0).song().artist());
        try { library.updateMetadata(song.id(), "", "", "-1", ""); fail("Invalid tempo accepted"); }
        catch (IllegalArgumentException expected) { assertEquals("120", library.songs().get(0).bpm()); }
    }
    @Test public void versionOneUpgradePreservesSongsFilesAndRunningOrder() {
        library.close(); context.deleteDatabase("test-library.db");
        try (android.database.sqlite.SQLiteDatabase db = context.openOrCreateDatabase("test-library.db", 0, null)) {
            db.execSQL("CREATE TABLE setlists (id TEXT PRIMARY KEY, name TEXT NOT NULL)");
            db.execSQL("CREATE TABLE songs (id TEXT PRIMARY KEY, title TEXT NOT NULL, pdf TEXT NOT NULL, audio TEXT, pages INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY, setlist_id TEXT NOT NULL, song_id TEXT NOT NULL, position INTEGER NOT NULL)");
            db.execSQL("INSERT INTO setlists VALUES ('set','Gig')");
            db.execSQL("INSERT INTO songs VALUES ('song','Old song','content://original/score',NULL,3)");
            db.execSQL("INSERT INTO entries VALUES ('entry','set','song',0)"); db.setVersion(1);
        }
        library = new Library(context, "test-library.db", "unused");
        Models.Entry entry = library.entries("set").get(0);
        assertEquals("entry", entry.id()); assertEquals("song", entry.song().id());
        assertEquals("content://original/score", entry.song().pdf()); assertEquals("", entry.song().notes());
        library.updateMetadata("song", "Artist", "C", "90", "Intro");
        assertEquals(1, library.searchSongs("set", "Artist Intro").size());
    }
}
