package com.setlist;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import java.io.File;
import java.io.FileOutputStream;

final class TestScores {
    static File pdf(Context context, String name, int pages) throws Exception {
        File file = new File(context.getCacheDir(), name + ".pdf"); PdfDocument document = new PdfDocument();
        try {
            for (int i = 0; i < pages; i++) {
                PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(612, 792, i + 1).create());
                page.getCanvas().drawColor(Color.WHITE); Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(Color.BLACK); paint.setTextSize(24);
                page.getCanvas().drawText(name + " / " + (i + 1), 40, 60, paint);
                for (int row = 0; row < 12; row++) for (int col = 0; col < 32; col++) {
                    float x = 40 + col * 16, y = 100 + row * 48;
                    page.getCanvas().drawOval(x, y, x + 8, y + 5, paint); page.getCanvas().drawLine(x + 8, y, x + 8, y - 20, paint);
                }
                document.finishPage(page);
            }
            try (FileOutputStream output = new FileOutputStream(file)) { document.writeTo(output); }
        } finally { document.close(); }
        return file;
    }
    static File audio(Context context) throws Exception {
        File file = new File(context.getCacheDir(), "test-tone.mp3");
        try (java.io.InputStream input = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("test-tone.mp3");
             FileOutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        return file;
    }
}
