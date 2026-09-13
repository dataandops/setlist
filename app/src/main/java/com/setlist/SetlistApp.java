package com.setlist;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SetlistApp extends Application {
    public Library library;
    public final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    public Runnable listener;
    public boolean busy;
    public String message;
    @Override public void onCreate() { super.onCreate(); Ui.init(this); library = new Library(this); }
    interface Work { String run() throws Exception; }
    public void work(Work task) {
        if (busy) return;
        busy = true; changed();
        io.execute(() -> {
            String result;
            try { result = task.run(); }
            catch (Exception e) { result = e.getMessage() == null ? "Something went wrong. Please try again." : e.getMessage(); }
            String finalResult = result;
            main.post(() -> { busy = false; message = finalResult; changed(); });
        });
    }
    private void changed() { if (listener != null) listener.run(); }
}
