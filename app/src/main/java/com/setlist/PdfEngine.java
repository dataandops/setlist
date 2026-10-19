package com.setlist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.util.LruCache;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** One PDF worker, priority for visible pages, bounded bitmaps, no permanent file copies. */
public final class PdfEngine implements AutoCloseable {
    public record Frame(Bitmap bitmap, int page, int pageCount, boolean cacheHit, long elapsedMs) {}
    public interface Callback { void ready(Frame frame); void failed(String message); }
    private record Key(String uri, int page, int width, int height) {}
    private record Cached(Bitmap bitmap, int pageCount) {}
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicLong sequence = new AtomicLong();
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>());
    private final LinkedHashMap<String, PdfRenderer> documents = new LinkedHashMap<>(3, .75f, true);
    private final LruCache<Key, Cached> pages;
    private volatile long generation, detailGeneration;
    private volatile boolean closed;

    public PdfEngine(Context context) {
        this.context = context.getApplicationContext();
        int budget = (int) Math.min(48L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 6);
        pages = new LruCache<>(budget) {
            @Override protected int sizeOf(Key key, Cached value) { return value.bitmap().getAllocationByteCount(); }
        };
    }
    private final class Task implements Runnable, Comparable<Task> {
        final int priority; final long number = sequence.incrementAndGet(); final Runnable action;
        Task(int priority, Runnable action) { this.priority = priority; this.action = action; }
        @Override public void run() { action.run(); }
        @Override public int compareTo(Task other) { int compare = Integer.compare(priority, other.priority); return compare != 0 ? compare : Long.compare(number, other.number); }
    }
    public void request(Models.Song song, int page, int width, int height, Callback callback) {
        if (closed) return;
        long token = ++generation, start = SystemClock.elapsedRealtime();
        // Remove stale speculative work before enqueuing a user's next page.
        worker.getQueue().clear();
        Key key = new Key(song.pdf(), page, Math.max(1, width), Math.max(1, height));
        Cached hit = pages.get(key);
        if (hit != null) { deliver(token, start, key, hit, true, callback); return; }
        worker.execute(new Task(0, () -> {
            if (closed || token != generation) return;
            try {
                Cached cached = pages.get(key); boolean cacheHit = cached != null;
                if (cached == null) cached = render(key);
                deliver(token, start, key, cached, cacheHit, callback);
            } catch (Exception | OutOfMemoryError e) {
                Log.w("SetlistPdf", "Unable to render selected page", e);
                main.post(() -> {
                    if (!closed && token == generation) callback.failed("Cannot open this score. The PDF may have moved, become unavailable, or be protected. Return to the setlist and choose Locate / replace PDF.");
                });
            }
        }));
    }
    private void deliver(long token, long start, Key key, Cached cached, boolean hit, Callback callback) {
        main.post(() -> {
            if (closed || token != generation) return;
            long elapsed = SystemClock.elapsedRealtime() - start;
            Log.i("SetlistPdf", "page=" + (key.page() + 1) + " cacheHit=" + hit + " readyMs=" + elapsed);
            callback.ready(new Frame(cached.bitmap(), key.page(), cached.pageCount(), hit, elapsed));
        });
    }
    public void prefetch(Models.Song song, int page, int width, int height) {
        if (closed || page < 0 || page >= song.pages()) return;
        long token = generation;
        Key key = new Key(song.pdf(), page, Math.max(1, width), Math.max(1, height));
        if (pages.get(key) != null) return;
        worker.execute(new Task(1, () -> {
            if (closed || generation != token || pages.get(key) != null) return;
            try { render(key); }
            catch (Exception | OutOfMemoryError e) { Log.d("SetlistPdf", "Lookahead unavailable; visible request will report any error."); }
        }));
    }
    /**
     * Renders what a zoomed view shows, at screen resolution, so zoomed notation stays sharp.
     * The view is width x height; zoom and pan match {@link PdfSwipeView}. Not cached: each request replaces the last.
     */
    public void detail(Models.Song song, int page, int width, int height, float zoom, float panX, float panY, Callback callback) {
        if (closed || width <= 0 || height <= 0) return;
        long token = generation, detailToken = ++detailGeneration, start = SystemClock.elapsedRealtime();
        worker.execute(new Task(0, () -> {
            if (closed || token != generation || detailToken != detailGeneration) return;
            try {
                PdfRenderer document = open(song.pdf());
                if (page < 0 || page >= document.getPageCount()) return;
                Bitmap bitmap;
                try (PdfRenderer.Page source = document.openPage(page)) {
                    // Same geometry as the fitted page, then the zoom about the view centre, then the pan.
                    float fit = Math.min((float) width / source.getWidth(), (float) height / source.getHeight());
                    android.graphics.Matrix matrix = new android.graphics.Matrix();
                    matrix.setScale(fit, fit); matrix.postTranslate((width - source.getWidth() * fit) / 2f, (height - source.getHeight() * fit) / 2f);
                    matrix.postScale(zoom, zoom, width / 2f, height / 2f); matrix.postTranslate(panX, panY);
                    float shrink = (float) Math.min(1, Math.sqrt(4_000_000d / ((double) width * height)));
                    matrix.postScale(shrink, shrink);
                    bitmap = Bitmap.createBitmap(Math.max(1, Math.round(width * shrink)), Math.max(1, Math.round(height * shrink)), Bitmap.Config.ARGB_8888);
                    android.graphics.RectF paper = new android.graphics.RectF(0, 0, source.getWidth(), source.getHeight()); matrix.mapRect(paper);
                    android.graphics.Paint white = new android.graphics.Paint(); white.setColor(Color.WHITE);
                    new android.graphics.Canvas(bitmap).drawRect(paper, white);
                    source.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                }
                Cached result = new Cached(bitmap, document.getPageCount());
                main.post(() -> {
                    if (closed || token != generation || detailToken != detailGeneration) return;
                    callback.ready(new Frame(result.bitmap(), page, result.pageCount(), false, SystemClock.elapsedRealtime() - start));
                });
            } catch (Exception | OutOfMemoryError e) { Log.d("SetlistPdf", "Zoom detail unavailable; the fitted page stays visible."); }
        }));
    }
    /** Drops any pending zoom detail, for example when the view zooms back out. */
    public void cancelDetail() { ++detailGeneration; }
    private PdfRenderer open(String uri) throws IOException {
        PdfRenderer document = documents.get(uri);
        if (document == null) {
            ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(Uri.parse(uri), "r");
            if (descriptor == null) throw new IOException("No readable file descriptor");
            try { document = new PdfRenderer(descriptor); }
            catch (Exception e) { descriptor.close(); throw e; }
            documents.put(uri, document);
            if (documents.size() > 2) {
                String oldest = documents.keySet().iterator().next(); documents.remove(oldest).close();
            }
        }
        return document;
    }
    private Cached render(Key key) throws IOException {
        PdfRenderer document = open(key.uri());
        if (key.page() < 0 || key.page() >= document.getPageCount()) throw new IOException("This page no longer exists. Relink the PDF to refresh its page count.");
        try (PdfRenderer.Page page = document.openPage(key.page())) {
            double scale = Math.min((double) key.width() / page.getWidth(), (double) key.height() / page.getHeight());
            scale = Math.min(scale, Math.sqrt(Math.min(4_000_000d, pages.maxSize() / 20d) / ((double) page.getWidth() * page.getHeight())));
            int width = Math.max(1, (int) Math.round(page.getWidth() * scale)), height = Math.max(1, (int) Math.round(page.getHeight() * scale));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            Cached result = new Cached(bitmap, document.getPageCount()); if (!closed) pages.put(key, result); return result;
        }
    }
    public int cacheBytes() { return pages.size(); }
    public int cacheBudgetBytes() { return pages.maxSize(); }
    @Override public void close() {
        if (closed) return; closed = true; ++generation; worker.getQueue().clear(); pages.evictAll();
        // Closing on the worker ensures no renderer is closed while a page is open.
        worker.execute(new Task(0, () -> { for (PdfRenderer renderer : documents.values()) renderer.close(); documents.clear(); }));
        worker.shutdown();
    }
}
