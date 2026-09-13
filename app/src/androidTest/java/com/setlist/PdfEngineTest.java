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
