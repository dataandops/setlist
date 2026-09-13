package com.setlist;

import java.util.List;

public final class Models {
    private Models() {}
    public record Setlist(String id, String name, int count) {}
    public record Song(String id, String title, String pdf, String audio, int pages) {}
    public record Entry(String id, Song song) {}

    public static void move(List<Entry> entries, int from, int to) {
        if (from < 0 || to < 0 || from >= entries.size() || to >= entries.size()) return;
        entries.add(to, entries.remove(from));
    }
}
