package com.setlist;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConcertFlowTest {
    private SetlistApp app;
    private Library original, library;
    private String set;
    private File first, second, audio;
    private UiDevice device;
    private ActivityScenario<?> scenario;
    @Before public void setup() throws Exception {
        app = (SetlistApp) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        original = app.library; app.deleteDatabase("test-flow.db"); library = new Library(app, "test-flow.db", "unused"); app.library = library;
        first = TestScores.pdf(app, "First song", 3); second = TestScores.pdf(app, "Second song", 2); audio = TestScores.audio(app);
        set = library.createSet("Flow test set"); Models.Song song = library.importPdf(set, Uri.fromFile(first)); library.renameSong(song.id(), "First song");
        library.attachAudio(song, Uri.fromFile(audio)); song = library.importPdf(set, Uri.fromFile(second)); library.renameSong(song.id(), "Second song");
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }
    @After public void cleanup() {
        if (scenario != null) scenario.close(); device.pressBack();
        try { device.setOrientationNatural(); device.unfreezeRotation(); } catch (android.os.RemoteException ignored) {}
        app.listener = null; app.library = original; library.close(); app.deleteDatabase("test-flow.db"); first.delete(); second.delete(); audio.delete();
    }
    private UiObject2 waitFor(androidx.test.uiautomator.BySelector selector) {
        UiObject2 object = device.wait(Until.findObject(selector), 5000); assertNotNull("Missing UI: " + selector, object); return object;
    }
    private void click(String text) { waitFor(By.text(java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(text), java.util.regex.Pattern.CASE_INSENSITIVE))).click(); }
    private void openReader() {
        scenario = ActivityScenario.launch(new Intent(app, ConcertActivity.class).putExtra("set", set).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitFor(By.desc("First song, page 1 of 3"));
    }
    @Test public void pageTurnsAdvanceToNextSongAndFinish() {
        openReader(); click("Next page"); waitFor(By.desc("First song, page 2 of 3"));
        click("Next page"); waitFor(By.desc("First song, page 3 of 3"));
        click("Next song"); waitFor(By.desc("Second song, page 1 of 2"));
        click("Previous"); waitFor(By.desc("First song, page 3 of 3"));
        click("Next song"); waitFor(By.desc("Second song, page 1 of 2"));
        click("Next page"); waitFor(By.desc("Second song, page 2 of 2"));
        device.findObject(By.desc("Finish setlist")).click(); waitFor(By.text("Set complete"));
        click("Play again"); waitFor(By.desc("First song, page 1 of 3"));
    }
    private void swipeScore(boolean forward, String current, String expected) {
        android.graphics.Rect bounds = waitFor(By.desc(current)).getVisibleBounds();
        int left = bounds.left + bounds.width() / 4, right = bounds.right - bounds.width() / 4;
        assertTrue(device.swipe(forward ? right : left, bounds.centerY(), forward ? left : right, bounds.centerY(), 30));
        waitFor(By.desc(expected));
    }
    @Test public void swipesTurnPagesAcrossSongsAndRotation() throws Exception {
        device.setOrientationNatural(); openReader();
        swipeScore(false, "First song, page 1 of 3", "First song, page 1 of 3");
        swipeScore(true, "First song, page 1 of 3", "First song, page 2 of 3");
        swipeScore(false, "First song, page 2 of 3", "First song, page 1 of 3");
        boolean landscape = device.getDisplayWidth() > device.getDisplayHeight();
        device.setOrientationLeft(); waitForOrientation(!landscape);
        swipeScore(true, "First song, page 1 of 3", "First song, page 2 of 3");
        swipeScore(true, "First song, page 2 of 3", "First song, page 3 of 3");
        swipeScore(true, "First song, page 3 of 3", "Second song, page 1 of 2");
        swipeScore(false, "Second song, page 1 of 2", "First song, page 3 of 3");
        swipeScore(true, "First song, page 3 of 3", "Second song, page 1 of 2");
        swipeScore(true, "Second song, page 1 of 2", "Second song, page 2 of 2");
        android.graphics.Rect bounds = waitFor(By.desc("Second song, page 2 of 2")).getVisibleBounds();
        device.swipe(bounds.right - bounds.width() / 4, bounds.centerY(), bounds.left + bounds.width() / 4, bounds.centerY(), 30);
        waitFor(By.text("Set complete"));
    }
    @Test public void tapsAndVerticalDragsDoNotTurnPages() {
        openReader(); android.graphics.Rect bounds = waitFor(By.desc("First song, page 1 of 3")).getVisibleBounds();
        device.click(bounds.centerX(), bounds.centerY());
        device.swipe(bounds.centerX(), bounds.bottom - bounds.height() / 4, bounds.centerX(), bounds.top + bounds.height() / 4, 30);
        device.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX() + 10, bounds.centerY(), 10);
        device.waitForIdle(); waitFor(By.desc("First song, page 1 of 3"));
    }
    @Test public void scoreMenuReturnsDirectlyHome() {
        scenario = ActivityScenario.launch(MainActivity.class);
        waitFor(By.desc("Open setlist Flow test set")).click(); click("Start setlist");
        waitFor(By.desc("First song, page 1 of 3")); click("Play MP3"); waitFor(By.text("Pause"));
        click("Setlist"); click("Home");
        waitFor(By.text("Your next great set.")); waitFor(By.desc("Open setlist Flow test set"));
        assertFalse(device.hasObject(By.desc("First song, page 1 of 3")));
        waitFor(By.desc("Open setlist Flow test set")).click(); click("Start setlist");
        waitFor(By.desc("First song, page 1 of 3")); waitFor(By.text("Play MP3"));
    }
    @Test public void setlistPickerJumpsAndRecreationKeepsPage() {
        openReader(); click("Setlist"); waitFor(By.textContains("02  Second song")).click();
        waitFor(By.desc("Second song, page 1 of 2")); click("Next page"); waitFor(By.desc("Second song, page 2 of 2"));
        scenario.recreate(); waitFor(By.desc("Second song, page 2 of 2"));
    }
    @Test public void rotationPreservesScoreInPortraitAndLandscape() throws Exception {
        device.setOrientationNatural(); openReader(); click("Next page"); waitFor(By.desc("First song, page 2 of 3"));
        boolean naturalLandscape = device.getDisplayWidth() > device.getDisplayHeight();
        device.setOrientationLeft(); waitForOrientation(!naturalLandscape); assertAdaptiveLayout(!naturalLandscape);
        waitFor(By.desc("First song, page 2 of 3")); click("Next page"); waitFor(By.desc("First song, page 3 of 3"));
        device.setOrientationNatural(); waitForOrientation(naturalLandscape); assertAdaptiveLayout(naturalLandscape);
        waitFor(By.desc("First song, page 3 of 3"));
        click("Next song"); waitFor(By.desc("Second song, page 1 of 2"));
    }
    private void assertAdaptiveLayout(boolean landscape) {
        scenario.onActivity(activity -> {
            android.view.View score = activity.getWindow().getDecorView().findViewWithTag("score_canvas");
            android.view.View controls = activity.getWindow().getDecorView().findViewWithTag("concert_controls");
            android.graphics.Rect scoreBounds = new android.graphics.Rect(), controlBounds = new android.graphics.Rect();
            assertTrue(score.getGlobalVisibleRect(scoreBounds)); assertTrue(controls.getGlobalVisibleRect(controlBounds));
            if (landscape && activity.getResources().getConfiguration().screenWidthDp >= 600)
                assertTrue("Landscape controls beside score", controlBounds.left >= scoreBounds.right);
            else assertTrue("Portrait controls below score", controlBounds.top >= scoreBounds.bottom);
        });
    }
    private void waitForOrientation(boolean landscape) {
        long deadline = android.os.SystemClock.elapsedRealtime() + 5000;
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            if ((device.getDisplayWidth() > device.getDisplayHeight()) == landscape) { device.waitForIdle(); return; }
            android.os.SystemClock.sleep(50);
        }
        fail("Expected " + (landscape ? "landscape" : "portrait") + " display orientation");
    }
    @Test public void audioPausesAndStopsOnSongChange() {
        openReader(); click("Play MP3"); waitFor(By.text("Pause"));
        click("Pause"); waitFor(By.text("Play MP3"));
        click("Play MP3"); waitFor(By.text("Pause"));
        waitFor(By.desc("Skip to next song: Second song")).click(); waitFor(By.desc("Second song, page 1 of 2"));
        assertFalse(device.hasObject(By.text("Pause")));
        click("Setlist"); waitFor(By.textContains("01  First song")).click();
        waitFor(By.text("Play MP3")); assertFalse(device.hasObject(By.text("Pause")));
    }
    @Test public void missingPdfCanBeSkippedWithoutDisplayingWrongSong() {
        assertTrue(first.delete());
        scenario = ActivityScenario.launch(new Intent(app, ConcertActivity.class).putExtra("set", set).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitFor(By.textContains("Cannot open this score"));
        assertFalse(device.hasObject(By.desc("First song, page 1 of 3")));
        waitFor(By.desc("Skip to next song: Second song")).click(); waitFor(By.desc("Second song, page 1 of 2"));
    }
    @Test public void savedSongsPaginationResetsForSearchAndSetlistFilter() {
        String source = library.createSet("Paged source");
        for (int i = 1; i <= 31; i++) {
            String id = "paged-" + i;
            library.getWritableDatabase().execSQL("INSERT INTO songs (id,title,pdf,pages) VALUES (?,?,?,1)",
                new Object[]{id, String.format(java.util.Locale.US, "Paged song %02d", i), Uri.fromFile(first).toString()});
            library.addSong(source, id);
        }
        scenario = ActivityScenario.launch(new Intent(app, SongLibraryActivity.class).putExtra("set", set).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        click("All setlists"); click("Paged source");
        waitFor(By.text("1–25 of 31 songs")); waitFor(By.text("Page 1 of 2"));
        assertFalse(waitFor(By.desc("Previous results page")).isEnabled());
        waitFor(By.desc("Next results page")).click(); waitFor(By.text("26–31 of 31 songs"));
        waitFor(By.desc("Add Paged song 26")); assertFalse(waitFor(By.desc("Next results page")).isEnabled());
        scenario.recreate(); waitFor(By.text("Page 2 of 2")); waitFor(By.desc("Add Paged song 26"));
        waitFor(By.clazz(android.widget.EditText.class)).setText("Paged song 01");
        waitFor(By.text("1–1 of 1 songs")); waitFor(By.text("Page 1 of 1"));
        waitFor(By.clazz(android.widget.EditText.class)).setText("");
        waitFor(By.desc("Next results page")).click(); waitFor(By.text("Page 2 of 2"));
        click("Paged source"); click("Flow test set");
        waitFor(By.text("Page 1 of 1")); waitFor(By.text("1–2 of 2 songs"));
        waitFor(By.desc("Add First song")).click(); assertEquals(3, library.entries(set).size());
    }
    @Test public void savedSongsSearchFilterAndEmptyRecovery() {
        String song = library.entries(set).get(0).song().id();
        library.updateMetadata(song, "Beyonce", "Am", "110", "Piano intro");
        String empty = library.createSet("Empty source");
        scenario = ActivityScenario.launch(new Intent(app, SongLibraryActivity.class).putExtra("set", empty).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitFor(By.clazz(android.widget.EditText.class)).setText("piano 110");
        waitFor(By.desc("Add First song")); assertFalse(device.hasObject(By.desc("Add Second song")));
        click("All setlists"); click("Empty source"); waitFor(By.text("No songs found"));
        click("Clear search and filters"); waitFor(By.desc("Add Second song"));
        waitFor(By.desc("Add First song")).click();
        assertEquals(song, library.entries(empty).get(0).song().id());
    }
    @Test public void editorDragOrderPersistsAcrossRecreation() throws Exception {
        scenario = ActivityScenario.launch(MainActivity.class);
        waitFor(By.desc("Open setlist Flow test set")).click();
        androidx.test.uiautomator.UiObject handle = device.findObject(new androidx.test.uiautomator.UiSelector().description("Reorder Second song"));
        android.graphics.Rect firstBounds = waitFor(By.desc("Reorder First song")).getVisibleBounds();
        assertTrue(handle.dragTo(firstBounds.centerX(), firstBounds.centerY() - 20, 40));
        device.waitForIdle();
        assertEquals("Second song", library.entries(set).get(0).song().title());
        scenario.recreate(); waitFor(By.desc("Song 1: Second song. Options"));
    }
    @Test @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    public void systemPickerImportsMultipleOriginalFilesWithPersistentAccess() throws Exception {
        android.content.ContentResolver resolver = app.getContentResolver();
        java.util.ArrayList<Uri> published = new java.util.ArrayList<>();
        try {
            String[] names = {"Setlist import one.pdf", "Setlist import two.pdf"};
            for (String name : names) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Setlist checks");
                values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values); assertNotNull(uri); published.add(uri);
                try (java.io.InputStream input = new java.io.FileInputStream(first); java.io.OutputStream output = resolver.openOutputStream(uri)) {
                    assertNotNull(output); byte[] buffer = new byte[8192]; int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
                values.clear(); values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0); resolver.update(uri, values, null, null);
            }
            scenario = ActivityScenario.launch(MainActivity.class); waitFor(By.desc("Open setlist Flow test set")).click(); click("Import PDFs");
            waitFor(By.text(names[0])).longClick(); waitFor(By.text(names[1])).click();
            UiObject2 confirm = device.wait(Until.findObject(By.res(java.util.regex.Pattern.compile(".*:id/action_menu_select"))), 3000);
            if (confirm == null) confirm = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("Open|Select", java.util.regex.Pattern.CASE_INSENSITIVE))), 3000);
            if (confirm == null) {
                java.io.ByteArrayOutputStream hierarchy = new java.io.ByteArrayOutputStream(); device.dumpWindowHierarchy(hierarchy);
                android.util.Log.e("SetlistPickerTest", hierarchy.toString()); fail("Cannot find picker confirmation");
            }
            confirm.click();
            waitFor(By.textContains("2 PDF(s) added.")); click("OK");
            List<Models.Entry> imported = library.entries(set); assertEquals(4, imported.size());
            for (Models.Entry entry : imported.subList(2, 4)) {
                assertTrue(entry.song().pdf().startsWith("content://"));
                assertTrue(resolver.getPersistedUriPermissions().stream().anyMatch(permission -> permission.isReadPermission() && permission.getUri().toString().equals(entry.song().pdf())));
            }
            scenario.recreate(); waitFor(By.desc("Song 3: Setlist import one. Options"));
        } finally {
            for (Uri uri : published) resolver.delete(uri, null, null);
        }
    }
}
