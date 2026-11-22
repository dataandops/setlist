# Bundled third-party components

All assets are local; the app does not download fonts, icons or code at runtime.

| Asset | Source | License |
| --- | --- | --- |
| Manrope variable font | [Google Fonts / Manrope](https://github.com/google/fonts/tree/main/ofl/manrope) | SIL Open Font License 1.1 |
| Inter variable font | [Google Fonts / Inter](https://github.com/google/fonts/tree/main/ofl/inter) | SIL Open Font License 1.1 |
| Material Symbols Rounded | [Google Material Design Icons](https://github.com/google/material-design-icons) | Apache 2.0 |
| Material Components for Android 1.13.0 | [Official release](https://github.com/material-components/material-components-android/releases/tag/1.13.0) | Apache 2.0 |
| PDFium (native PDF renderer) | [pdfium.googlesource.com](https://pdfium.googlesource.com/pdfium/) | BSD 3-Clause / Apache 2.0 |
| PdfiumAndroid 1.0.35 (`io.legere:pdfiumandroid`) | [PdfiumAndroidKt](https://github.com/johngray1965/PdfiumAndroidKt) | Apache 2.0 |

Font, icon and PDFium license notices are packaged in `app/src/main/assets/licenses/`.
`scripts/fetch_symbols.py` freezes the selected SVG icons as Android vector
drawables. The checked-in files are the build inputs; builds do not fetch assets.

## Why PDFium instead of the platform PdfRenderer

Android's `android.graphics.pdf.PdfRenderer` draws page content only and skips
annotations, so markups that musicians add in annotation apps (ink, boxes, free
text, stamps) were invisible. `PdfEngine` renders through PDFium with annotation
rendering on. Native libraries ship for `arm64-v8a`, `armeabi-v7a` and `x86_64`
(emulator/CI), and are compressed in the APK. Version 1.0.35 is the newest release
that builds against compileSdk 35; 2.x requires compileSdk 36+.
