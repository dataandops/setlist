package com.setlist;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

/** Activity-scoped audio: never autoplays and never continues after leaving a song. */
final class AudioController {
    interface Listener { void changed(); void failed(String message); }
    private final Context context;
    private final AudioManager audio;
    private final Listener listener;
    private final AudioFocusRequest focus;
    private MediaPlayer player;
    private boolean prepared, loading;
    private final AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    AudioController(Context context, Listener listener) {
        this.context = context; this.listener = listener; audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(change -> { if (change != AudioManager.AUDIOFOCUS_GAIN) pause(); }).build();
    }
    boolean isPlaying() { return player != null && prepared && player.isPlaying(); }
    boolean isLoading() { return loading; }
    int position() { return prepared && player != null ? player.getCurrentPosition() : 0; }
    int duration() { return prepared && player != null ? player.getDuration() : 0; }
    void toggle(String uri) {
        if (isPlaying()) { pause(); return; }
        if (loading || uri == null) return;
        if (player != null && prepared) { play(); return; }
        loading = true; listener.changed(); MediaPlayer next = new MediaPlayer(); player = next;
        try {
            next.setAudioAttributes(attributes); next.setDataSource(context, Uri.parse(uri));
            next.setOnPreparedListener(mp -> {
                if (player != mp) return; loading = false; prepared = true; play();
            });
            next.setOnCompletionListener(mp -> { audio.abandonAudioFocusRequest(focus); listener.changed(); });
            next.setOnErrorListener((mp, what, extra) -> { if (player == mp) fail(); return true; });
            next.prepareAsync();
        } catch (Exception e) { fail(); }
    }
    private void play() {
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            listener.failed("Audio is in use by another app. Try Play again."); listener.changed(); return;
        }
        player.start(); listener.changed();
    }
    void pause() {
        if (isPlaying()) player.pause(); audio.abandonAudioFocusRequest(focus); listener.changed();
    }
    void seek(int millis) { if (prepared && player != null) player.seekTo(Math.max(0, Math.min(millis, duration()))); }
    void stop() {
        MediaPlayer old = player; player = null; prepared = false; loading = false;
        if (old != null) old.release(); audio.abandonAudioFocusRequest(focus); listener.changed();
    }
    private void fail() { stop(); listener.failed("Cannot play this audio. Locate the file again from the song options."); }
}
