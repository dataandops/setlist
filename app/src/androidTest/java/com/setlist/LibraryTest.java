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
}
