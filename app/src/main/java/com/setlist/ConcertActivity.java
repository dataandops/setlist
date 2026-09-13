package com.setlist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

public final class ConcertActivity extends AppCompatActivity {
    private List<Models.Entry> entries;
    private String setId;
    private int songIndex, pageIndex, renderWidth, renderHeight;
    private int[] pageCounts;
    private boolean ready, pendingDraw, lastCacheHit, wideLayout;
    private long requestedAt;
    private PdfEngine pdf;
    private AudioController audio;
    private ImageView score;
    private FrameLayout canvas;
    private TextView title, progress, status, audioTime, upcomingTitle;
    private Button previous, next, skip, play;
    private LinearLayout audioBar;
    private SeekBar seek;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() { updateAudio(); main.postDelayed(this, 500); }
    };
    private final BroadcastReceiver unplugged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) { audio.stop(); }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        setId = getIntent().getStringExtra("set");
        entries = ((SetlistApp) getApplication()).library.entries(setId);
        if (entries.isEmpty()) { finish(); return; }
        songIndex = Math.max(0, Math.min(entries.size() - 1, saved == null ? getIntent().getIntExtra("song", 0) : saved.getInt("song")));
        pageCounts = new int[entries.size()]; for (int i = 0; i < entries.size(); i++) pageCounts[i] = entries.get(i).song().pages();
        pageIndex = saved == null ? 0 : Math.max(0, Math.min(pageCounts[songIndex] - 1, saved.getInt("page")));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        pdf = new PdfEngine(this);
        audio = new AudioController(this, new AudioController.Listener() {
            @Override public void changed() { updateAudio(); }
            @Override public void failed(String message) { new MaterialAlertDialogBuilder(ConcertActivity.this).setMessage(message).setPositiveButton("OK", null).show(); }
        });
        build();
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(unplugged, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(unplugged, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }
    @Override protected void onResume() { super.onResume(); if (audio != null) main.post(ticker); }
    @Override protected void onPause() { main.removeCallbacks(ticker); if (audio != null) audio.stop(); super.onPause(); }
    @Override protected void onDestroy() {
        if (pdf != null) { pdf.close(); unregisterReceiver(unplugged); }
        if (score != null) score.setImageDrawable(null); super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putInt("song", songIndex); out.putInt("page", pageIndex); }
    private Models.Song song() { return entries.get(songIndex).song(); }
    private void build() {
        android.content.res.Configuration configuration = getResources().getConfiguration();
        wideLayout = configuration.screenWidthDp >= 600 && configuration.screenWidthDp > configuration.screenHeightDp;
        LinearLayout root = Ui.column(this); root.setBackgroundColor(Ui.BG); setContentView(root); Ui.insets(root);
        LinearLayout body = Ui.column(this); Ui.pad(body, 12); root.addView(body, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout header = Ui.row(this);
        header.addView(Ui.quiet(this, "Setlist", R.drawable.ic_queue_music, this::chooseSong), new LinearLayout.LayoutParams(-2, -2));
        LinearLayout labels = Ui.column(this); Ui.pad(labels, 8);
        title = Ui.heading(this, "", Ui.TITLE); title.setMaxLines(2); title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        progress = Ui.text(this, "", 12, Ui.MUTED); labels.addView(title); Ui.gap(labels, 4); labels.addView(progress); Ui.weighted(header, labels);
        body.addView(header); Ui.gap(body, 8);
        LinearLayout stage = wideLayout ? Ui.row(this) : Ui.column(this); body.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));
        canvas = new PdfSwipeView(this, () -> ready, this::next, this::previous); canvas.setTag("score_canvas"); canvas.setBackgroundColor(Ui.BG);
        score = new ImageView(this); score.setScaleType(ImageView.ScaleType.FIT_CENTER);
        canvas.addView(score, new FrameLayout.LayoutParams(-1, -1));
        score.getViewTreeObserver().addOnDrawListener(() -> {
            if (!pendingDraw) return;
            pendingDraw = false;
            android.util.Log.i("SetlistFrame", "page=" + (pageIndex + 1) + " drawMs=" + (android.os.SystemClock.elapsedRealtime() - requestedAt) + " cacheHit=" + lastCacheHit);
        });
        status = Ui.text(this, "Opening score…", Ui.BODY, Ui.MUTED); status.setGravity(Gravity.CENTER); Ui.pad(status, 24);
        status.setBackgroundColor(Ui.BG); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        canvas.addView(status, new FrameLayout.LayoutParams(-1, -1));
        stage.addView(canvas, wideLayout ? new LinearLayout.LayoutParams(0, -1, 1) : new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout deck = Ui.column(this); deck.setTag("concert_controls");
        if (wideLayout) {
            android.widget.ScrollView sidebar = new android.widget.ScrollView(this); sidebar.setFillViewport(false); sidebar.addView(deck);
            LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(Ui.dp(this, 224), -1); side.setMarginStart(Ui.dp(this, 16)); side.gravity = Gravity.TOP; stage.addView(sidebar, side);
            deck.setGravity(Gravity.TOP); Ui.card(deck); deck.addView(Ui.eyebrow(this, "PAGE CONTROLS")); Ui.gap(deck, 8);
        } else stage.addView(deck);
        audioBar = wideLayout ? Ui.column(this) : Ui.row(this);
        if (wideLayout) { Ui.gap(audioBar, 20); audioBar.addView(Ui.eyebrow(this, "BACKING TRACK")); Ui.gap(audioBar, 8); }
        play = Ui.withIcon(Ui.button(this, "Play MP3", false, () -> audio.toggle(song().audio())), R.drawable.ic_play_arrow, false);
        audioBar.addView(play, new LinearLayout.LayoutParams(wideLayout ? -1 : -2, -2));
        seek = new SeekBar(this); seek.setContentDescription("Audio playback position");
        if (wideLayout) audioBar.addView(seek, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48))); else Ui.weighted(audioBar, seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) { if (fromUser) audio.seek(value); }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        audioTime = Ui.text(this, "0:00", 12, Ui.MUTED); audioBar.addView(audioTime); if (!wideLayout) deck.addView(audioBar);
        Ui.gap(deck, 12); LinearLayout controls = wideLayout ? Ui.column(this) : Ui.row(this);
        previous = Ui.withIcon(Ui.button(this, "Previous", false, this::previous), R.drawable.ic_chevron_left, false);
        next = Ui.withIcon(Ui.button(this, "Next page", true, this::next), R.drawable.ic_chevron_right, true);
        if (wideLayout) {
            previous.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            controls.addView(next); controls.addView(previous);
        }
        else { Ui.weighted(controls, previous); Ui.weighted(controls, next); }
        deck.addView(controls); Ui.gap(deck, 8);
        skip = Ui.withIcon(Ui.button(this, "Next song", false, () -> { if (songIndex + 1 < entries.size()) goSong(songIndex + 1); else finishSet(); }), R.drawable.ic_skip_next, true);
        if (wideLayout) {
            Ui.gap(deck, 12); deck.addView(Ui.eyebrow(this, "UP NEXT")); Ui.gap(deck, 8);
            upcomingTitle = Ui.heading(this, "", Ui.TITLE); upcomingTitle.setMaxLines(2); upcomingTitle.setEllipsize(android.text.TextUtils.TruncateAt.END); deck.addView(upcomingTitle); Ui.gap(deck, 8);
        }
        deck.addView(skip);
        if (wideLayout) deck.addView(audioBar);
        canvas.addOnLayoutChangeListener((v, left, top, right, bottom, ol, ot, or, ob) -> {
            int width = right - left, height = bottom - top;
            if (width > 0 && height > 0 && (width != renderWidth || height != renderHeight)) { renderWidth = width; renderHeight = height; showPage(); }
        });
        updateLabels();
    }
    private void updateLabels() {
        title.setText(song().title());
        progress.setText(getString(R.string.reader_progress, songIndex + 1, entries.size(), pageIndex + 1, pageCounts[songIndex]));
        previous.setEnabled(songIndex > 0 || pageIndex > 0);
        next.setText(pageIndex + 1 < pageCounts[songIndex] ? "Next page" : songIndex + 1 < entries.size() ? "Next song" : "Finish setlist");
        Ui.withIcon(next, pageIndex + 1 < pageCounts[songIndex] ? R.drawable.ic_chevron_right : songIndex + 1 < entries.size() ? R.drawable.ic_skip_next : R.drawable.ic_check, true);
        next.setEnabled(ready);
        boolean hasNext = songIndex + 1 < entries.size();
        skip.setText(hasNext ? (wideLayout ? "Skip to song" : "Up next: " + entries.get(songIndex + 1).song().title()) : "Finish setlist");
        if (upcomingTitle != null) upcomingTitle.setText(hasNext ? entries.get(songIndex + 1).song().title() : "End of setlist");
        skip.setContentDescription(songIndex + 1 < entries.size() ? "Skip to next song: " + entries.get(songIndex + 1).song().title() : "Finish setlist");
        audioBar.setVisibility(song().audio() == null ? View.GONE : View.VISIBLE); updateAudio();
    }
    private void showPage() {
        if (renderWidth == 0 || renderHeight == 0) return;
        requestedAt = android.os.SystemClock.elapsedRealtime(); pendingDraw = false;
        ready = false; updateLabels(); status.setText("Opening score…"); status.setTextColor(Ui.MUTED); status.setVisibility(View.VISIBLE);
        // Clear the previous song immediately so a failure never displays the wrong score.
        score.setImageDrawable(null); score.setContentDescription(null);
        pdf.request(song(), pageIndex, renderWidth, renderHeight, new PdfEngine.Callback() {
            @Override public void ready(PdfEngine.Frame frame) {
                if (isDestroyed()) return;
                ready = true; pendingDraw = true; lastCacheHit = frame.cacheHit(); pageCounts[songIndex] = frame.pageCount(); score.setImageBitmap(frame.bitmap());
                score.setContentDescription(song().title() + ", page " + (pageIndex + 1) + " of " + frame.pageCount());
                status.setVisibility(View.GONE); updateLabels();
                pdf.prefetch(song(), pageIndex + 1, renderWidth, renderHeight);
                if (songIndex + 1 < entries.size()) {
                    Models.Song upcoming = entries.get(songIndex + 1).song();
                    int upcomingHeight = wideLayout ? renderHeight : renderHeight + (song().audio() == null ? 0 : Ui.dp(ConcertActivity.this, 56)) - (upcoming.audio() == null ? 0 : Ui.dp(ConcertActivity.this, 56));
                    pdf.prefetch(upcoming, 0, renderWidth, upcomingHeight);
                }
                pdf.prefetch(song(), pageIndex + 2, renderWidth, renderHeight);
                pdf.prefetch(song(), pageIndex - 1, renderWidth, renderHeight);
            }
            @Override public void failed(String message) {
                if (isDestroyed()) return; status.setText(message); status.setTextColor(Ui.ERROR); status.setVisibility(View.VISIBLE);
                next.setEnabled(false); // Setlist and explicit song-skip remain usable.
            }
        });
    }
    private void next() {
        if (!ready) return;
        if (pageIndex + 1 < pageCounts[songIndex]) { pageIndex++; showPage(); }
        else if (songIndex + 1 < entries.size()) goSong(songIndex + 1);
        else finishSet();
    }
    private void previous() {
        if (pageIndex > 0) { pageIndex--; showPage(); }
        else if (songIndex > 0) { audio.stop(); songIndex--; pageIndex = pageCounts[songIndex] - 1; showPage(); }
    }
    private void goSong(int index) { audio.stop(); songIndex = index; pageIndex = 0; showPage(); }
    private void chooseSong() {
        String[] names = new String[entries.size()];
        for (int i = 0; i < names.length; i++) names[i] = String.format(Locale.US, "%02d  %s%s", i + 1, entries.get(i).song().title(), i == songIndex ? "  ·  Playing" : "");
        new MaterialAlertDialogBuilder(this).setTitle(((SetlistApp) getApplication()).library.setName(setId))
            .setSingleChoiceItems(names, songIndex, (dialog, index) -> { dialog.dismiss(); if (index != songIndex) goSong(index); })
            .setNegativeButton("Home", (dialog, which) -> {
                audio.stop();
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            })
            .setPositiveButton("Back to score", null).setNeutralButton("Edit setlist", (dialog, which) -> finish()).show();
    }
    private void finishSet() {
        audio.stop();
        new MaterialAlertDialogBuilder(this).setTitle("Set complete").setMessage("All " + entries.size() + " songs. Take a bow.")
            .setPositiveButton("Return to setlist", (dialog, which) -> finish())
            .setNeutralButton("Play again", (dialog, which) -> goSong(0)).setNegativeButton("Stay on score", null).show();
    }
    private void updateAudio() {
        if (play == null || audio == null) return;
        play.setText(audio.isLoading() ? "Loading…" : audio.isPlaying() ? "Pause" : "Play MP3"); play.setEnabled(!audio.isLoading());
        Ui.withIcon(play, audio.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow, false);
        seek.setMax(Math.max(1, audio.duration())); seek.setProgress(audio.position()); seek.setEnabled(audio.duration() > 0);
        audioTime.setText(String.format(Locale.US, "%d:%02d / %d:%02d", audio.position() / 60000, audio.position() / 1000 % 60, audio.duration() / 60000, audio.duration() / 1000 % 60));
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0 && (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_PAGE_DOWN || keyCode == KeyEvent.KEYCODE_SPACE)) { next(); return true; }
        if (event.getRepeatCount() == 0 && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_PAGE_UP)) { previous(); return true; }
        return super.onKeyDown(keyCode, event);
    }
}
