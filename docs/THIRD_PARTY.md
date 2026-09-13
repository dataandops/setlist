# Bundled design assets

All assets are local; the app does not download fonts or icons at runtime.

| Asset | Source | License |
| --- | --- | --- |
| Manrope variable font | [Google Fonts / Manrope](https://github.com/google/fonts/tree/main/ofl/manrope) | SIL Open Font License 1.1 |
| Inter variable font | [Google Fonts / Inter](https://github.com/google/fonts/tree/main/ofl/inter) | SIL Open Font License 1.1 |
| Material Symbols Rounded | [Google Material Design Icons](https://github.com/google/material-design-icons) | Apache 2.0 |
| Material Components for Android 1.13.0 | [Official release](https://github.com/material-components/material-components-android/releases/tag/1.13.0) | Apache 2.0 |

Font and icon license notices are packaged in `app/src/main/assets/licenses/`.
`scripts/fetch_symbols.py` freezes the selected SVG icons as Android vector
drawables. The checked-in files are the build inputs; builds do not fetch assets.
