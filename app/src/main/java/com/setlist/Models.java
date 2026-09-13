package com.setlist;

import java.util.List;

public final class Models {
    private Models() {}
    public record Setlist(String id, String name, int count) {}
    public record Song(String id, String title, String pdf, String audio, int pages,
                       String artist, String key, String bpm, String notes) {
        public Song(String id, String title, String pdf, String audio, int pages) { this(id, title, pdf, audio, pages, "", "", "", ""); }
        public String metadataLine() {
            java.util.StringJoiner parts = new java.util.StringJoiner(" · ");
            if (!artist.isEmpty()) parts.add(artist);
            if (!key.isEmpty()) parts.add(key);
            if (!bpm.isEmpty()) parts.add(bpm + " BPM");
            return parts.toString();
        }
    }
    public record Entry(String id, Song song) {}

    public static void move(List<Entry> entries, int from, int to) {
        if (from < 0 || to < 0 || from >= entries.size() || to >= entries.size()) return;
        entries.add(to, entries.remove(from));
    }
}
