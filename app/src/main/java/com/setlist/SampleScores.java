package com.setlist;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;

public final class SampleScores {
    private SampleScores() {}
    public static String create(Context context, Library library) throws Exception {
        String set = library.createSet("Friday night · sample set");
        try {
            String[] titles = {"Midnight Drive", "Amber Lights", "Last Train Home"};
            for (int song = 0; song < titles.length; song++) {
                File file = new File(context.getFilesDir(), titles[song] + ".pdf");
                PdfDocument document = new PdfDocument();
                try {
                    for (int page = 0; page < 3; page++) {
                        PdfDocument.Page sheet = document.startPage(new PdfDocument.PageInfo.Builder(612, 792, page + 1).create());
                        Canvas c = sheet.getCanvas(); Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                        c.drawColor(Color.WHITE); p.setColor(Color.BLACK); p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); p.setTextSize(30);
                        c.drawText(titles[song], 48, 70, p); p.setTextSize(13); p.setTypeface(Typeface.DEFAULT);
                        c.drawText("SAMPLE CHART  /  Keyboard  /  96 BPM  /  C major", 48, 100, p);
                        c.drawText(new String[]{"INTRO + VERSE", "CHORUS + SOLO", "OUTRO"}[page], 48, 146, p);
                        for (int line = 0; line < 5; line++) {
                            int top = 190 + line * 100;
                            p.setStrokeWidth(0.6f);
                            for (int staff = 0; staff < 5; staff++) c.drawLine(48, top + staff * 9, 564, top + staff * 9, p);
                            for (int bar = 0; bar < 4; bar++) {
                                int x = 60 + bar * 126; p.setTextSize(18);
                                c.drawText(new String[]{"Cmaj7", "Am7", "Dm7", "G7"}[(bar + song) % 4], x, top - 14, p);
                                c.drawLine(x + 112, top, x + 112, top + 36, p);
                                for (int note = 0; note < 3; note++) {
                                    int nx = x + 14 + note * 28, ny = top + 9 * ((note + bar + page) % 5);
                                    c.drawOval(nx - 5, ny - 3, nx + 5, ny + 3, p); c.drawLine(nx + 5, ny, nx + 5, ny - 25, p);
                                }
                            }
                        }
                        p.setTextSize(12); c.drawText("Original demo score · Page " + (page + 1) + " of 3", 48, 752, p);
                        document.finishPage(sheet);
                    }
                    try (FileOutputStream output = new FileOutputStream(file)) { document.writeTo(output); }
                    Models.Song imported = library.importPdf(set, Uri.fromFile(file));
                    library.renameSong(imported.id(), titles[song]);
                } finally { document.close(); }
            }
            return set;
        } catch (Exception e) { library.deleteSet(set); throw e; }
    }
}
