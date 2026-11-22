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
    /**
     * One blank US Letter page carrying a single /Square markup annotation, drawn as a solid red
     * box over PDF rect [100 400 300 600]. The platform PdfRenderer leaves it out; the reader must show it.
     */
    static File annotatedPdf(Context context, String name) throws Exception {
        String appearance = "1 0 0 rg 0 0 200 200 re f\n";
        String[] objects = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Annots [4 0 R] >>",
            "<< /Type /Annot /Subtype /Square /Rect [100 400 300 600] /F 4 /C [1 0 0] /IC [1 0 0] /AP << /N 5 0 R >> >>",
            "<< /Type /XObject /Subtype /Form /BBox [0 0 200 200] /Length " + appearance.length() + " >>\nstream\n" + appearance + "endstream",
        };
        StringBuilder pdf = new StringBuilder("%PDF-1.7\n"); int[] offsets = new int[objects.length];
        for (int i = 0; i < objects.length; i++) { offsets[i] = pdf.length(); pdf.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n"); }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) pdf.append(String.format(java.util.Locale.US, "%010d 00000 n \n", offset));
        pdf.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        File file = new File(context.getCacheDir(), name + ".pdf");
        try (FileOutputStream output = new FileOutputStream(file)) { output.write(pdf.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII)); }
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
