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

    /** Orders names the way people number charts: "2 Blues" before "10 Ballad" before "101 Coda". */
    public static final java.util.Comparator<String> NATURAL = (a, b) -> {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char x = a.charAt(i), y = b.charAt(j);
            if (Character.isDigit(x) && Character.isDigit(y)) {
                int si = i, sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) i++;
                while (j < b.length() && Character.isDigit(b.charAt(j))) j++;
                java.math.BigInteger m = new java.math.BigInteger(a.substring(si, i)), n = new java.math.BigInteger(b.substring(sj, j));
                if (m.compareTo(n) != 0) return m.compareTo(n);
            } else {
                int compare = Character.compare(Character.toLowerCase(x), Character.toLowerCase(y));
                if (compare != 0) return compare;
                i++; j++;
            }
        }
        int compare = Integer.compare(a.length() - i, b.length() - j);
        return compare != 0 ? compare : a.compareTo(b);
    };

    public static void move(List<Entry> entries, int from, int to) {
        if (from < 0 || to < 0 || from >= entries.size() || to >= entries.size()) return;
        entries.add(to, entries.remove(from));
    }
}
