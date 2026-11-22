package com.setlist;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PdfEngineTest {
    private File source;
    private PdfEngine engine;
    private Models.Song song;
    @Before public void setup() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext(); source = TestScores.pdf(context, "Engine score", 12);
        engine = new PdfEngine(context); song = new Models.Song("test", "Engine score", Uri.fromFile(source).toString(), null, 12);
    }
    @After public void close() { engine.close(); source.delete(); }
    private PdfEngine.Frame load(Models.Song target, int page) throws Exception {
        CountDownLatch done = new CountDownLatch(1); AtomicReference<PdfEngine.Frame> result = new AtomicReference<>(); AtomicReference<String> error = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> engine.request(target, page, 1600, 2000, new PdfEngine.Callback() {
            @Override public void ready(PdfEngine.Frame frame) { result.set(frame); done.countDown(); }
            @Override public void failed(String message) { error.set(message); done.countDown(); }
        }));
        assertTrue("Page request completed", done.await(5, TimeUnit.SECONDS));
        assertNull(error.get(), error.get()); return result.get();
    }
    @Test public void coldAndCachedPagesMeetTwoSecondBudget() throws Exception {
        long max = 0;
        for (int i = 0; i < 12; i++) { PdfEngine.Frame frame = load(song, i); assertFalse(frame.cacheHit()); max = Math.max(max, frame.elapsedMs()); }
        PdfEngine.Frame hit = load(song, 11); assertTrue(hit.cacheHit());
        assertTrue("Cold max " + max + "ms", max < 2000); assertTrue("Cache hit " + hit.elapsedMs() + "ms", hit.elapsedMs() < 2000);
        assertTrue(engine.cacheBytes() <= engine.cacheBudgetBytes());
        android.util.Log.i("SetlistBenchmark", "12 pages at 1600x2000: coldMaxMs=" + max + " cacheMs=" + hit.elapsedMs() + " cacheBytes=" + engine.cacheBytes());
    }
    @Test public void latestRequestWinsWhenSongsChangeRapidly() throws Exception {
        CountDownLatch done = new CountDownLatch(1); AtomicReference<Integer> delivered = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int i = 0; i < 10; i++) {
                engine.request(song, i, 900, 1200, new PdfEngine.Callback() {
                    @Override public void ready(PdfEngine.Frame frame) { delivered.set(frame.page()); done.countDown(); }
                    @Override public void failed(String message) { fail(message); }
                });
            }
        });
        assertTrue(done.await(5, TimeUnit.SECONDS)); assertEquals(Integer.valueOf(9), delivered.get());
    }
    @Test public void lookaheadIsServedFromCache() throws Exception {
        load(song, 0);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> engine.prefetch(song, 1, 1600, 2000));
        // A visible page queued after lookahead waits for any active render and coalesces via cache.
        SystemClock.sleep(300);
        assertTrue(load(song, 1).cacheHit());
    }
    private static void assertRed(String where, int pixel) {
        assertTrue(where + " should be red, was #" + Integer.toHexString(pixel),
                android.graphics.Color.red(pixel) > 200 && android.graphics.Color.green(pixel) < 80 && android.graphics.Color.blue(pixel) < 80);
    }
    private static void assertWhite(String where, int pixel) {
        assertTrue(where + " should be white, was #" + Integer.toHexString(pixel),
                android.graphics.Color.red(pixel) > 230 && android.graphics.Color.green(pixel) > 230 && android.graphics.Color.blue(pixel) > 230);
    }
    @Test public void markupAnnotationsAreRendered() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File annotated = TestScores.annotatedPdf(context, "Annotated score");
        try {
            Models.Song marked = new Models.Song("marked", "Annotated score", Uri.fromFile(annotated).toString(), null, 1);
            // Fitted page: the box centre is PDF (200, 500), which is (200, 292) from the top-left of a 612x792 page.
            android.graphics.Bitmap page = load(marked, 0).bitmap();
            int w = page.getWidth(), h = page.getHeight();
            assertRed("Markup centre", page.getPixel(Math.round(200f / 612 * w), Math.round(292f / 792 * h)));
            assertWhite("Paper outside markup", page.getPixel(Math.round(50f / 612 * w), Math.round(50f / 792 * h)));

            // Zoomed detail at 2x with no pan uses the same geometry as PdfSwipeView.
            int viewW = 900, viewH = 1200; float zoom = 2f;
            CountDownLatch done = new CountDownLatch(1); AtomicReference<PdfEngine.Frame> detail = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> engine.detail(marked, 0, viewW, viewH, zoom, 0, 0, new PdfEngine.Callback() {
                @Override public void ready(PdfEngine.Frame frame) { detail.set(frame); done.countDown(); }
                @Override public void failed(String message) { fail(message); }
            }));
            assertTrue("Detail render completed", done.await(5, TimeUnit.SECONDS));
            float fit = Math.min(viewW / 612f, viewH / 792f);
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setScale(fit, fit); matrix.postTranslate((viewW - 612 * fit) / 2f, (viewH - 792 * fit) / 2f);
            matrix.postScale(zoom, zoom, viewW / 2f, viewH / 2f);
            float[] centre = {200, 292}, paper = {400, 292}; matrix.mapPoints(centre); matrix.mapPoints(paper);
            android.graphics.Bitmap zoomed = detail.get().bitmap();
            assertRed("Zoomed markup centre", zoomed.getPixel(Math.round(centre[0]), Math.round(centre[1])));
            assertWhite("Zoomed paper", zoomed.getPixel(Math.round(paper[0]), Math.round(paper[1])));
        } finally { annotated.delete(); }
    }
    @Test public void missingFileReportsErrorAndReaderCanRecover() throws Exception {
        CountDownLatch done = new CountDownLatch(1); AtomicReference<String> error = new AtomicReference<>();
        Models.Song missing = new Models.Song("missing", "Missing", "file:///does-not-exist.pdf", null, 1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> engine.request(missing, 0, 900, 1200, new PdfEngine.Callback() {
            @Override public void ready(PdfEngine.Frame frame) { fail("Missing file rendered"); }
            @Override public void failed(String message) { error.set(message); done.countDown(); }
        }));
        assertTrue(done.await(5, TimeUnit.SECONDS)); assertTrue(error.get().contains("Locate / replace PDF"));
        assertNotNull(load(song, 0).bitmap());
    }
}
