# Dark stage design system

Setlist is an offline concert companion. Its two main tasks are assembling a running order and turning score pages while playing. The design is calm, readable and tactile, on phones and tablets in either orientation.

## Foundations

| Role | Token |
| --- | --- |
| Background | Ink `#11161C` |
| Surface / raised surface | `#1B232C` / `#26333E` |
| Primary | Warm amber `#E6BF83`, ink labels |
| Text / supporting text | `#F4F1EA` / `#ADB9C6` |
| Outline | `#34414D` |
| Success / error | Sage `#A4D1BE` / `#FFB4AB` |
| Headings | Bundled Manrope, weight 650; 36 / 28 / 20 sp (phones: 30 / 24 / 18 sp) |
| Body / caption / eyebrow | Bundled Inter; 16 / 14 / 12 sp on every device |
| Spacing | 4 / 8 / 12 / 16 / 24 / 32 dp (phones: 4 / 8 / 12 / 12 / 16 / 24 dp) |
| Shape | 16 dp corners, no decorative shadows |
| Targets | 48 dp minimum; principal controls 56 dp (phones: 48 dp) |

Phones (smallest width under 600 dp) use the compact scale in `values/dimens.xml` and `Ui.typeSize`; tablets use `values-sw600dp`.

Material 3 supplies dialogs, buttons and outlined inputs. Material Symbols Rounded supply interface glyphs. Fonts and vectors are packaged in the APK and work offline. See [attributions](THIRD_PARTY.md).

The custom launcher mark combines a setlist sheet with piano keys. Adaptive foreground/background resources accommodate launcher masks; Android 13+ has a monochrome themed variant. The same mark is used in the app header.

## Components and layouts

Use `Ui.java`, `colors.xml`, `dimens.xml` and the Material theme as the shared source of truth. Primary buttons are amber; supporting actions use tonal surfaces or quiet text. Buttons have pressed, disabled and focus states. Song cards show title first, then artist/key/tempo and file details. Drag handles also offer menu-based movement.

The editor uses a single column, capped at 960 dp. Reader controls sit below the PDF in portrait. In landscape at widths of at least 600 dp, they move into a 224 dp sidebar. The sidebar card wraps its contents and scrolls on short screens. Next page is primary, Previous is quiet, the upcoming song has a separate title, and audio occupies its own group. Rotating preserves the active song and page, and re-renders the PDF at its original aspect ratio.

Empty states share an original vector score illustration, centered heading, short explanation and one primary action. Empty home invites the first setlist; an empty set invites importing PDFs; an empty library explains how songs become reusable. No search results offers a single action to clear the query and setlist filter. Decorative illustrations are excluded from accessibility traversal.

Pre-code review: hierarchy obvious in three seconds—yes; at most two focal points—yes; consistent spacing—yes; normal text contrast against its intended surface meets AA. Font scaling and TalkBack on physical devices remain acceptance checks.

## Review gallery

The debug-only `DesignSystemActivity` displays the actual shared components. Launch with:

```sh
.tools/android-sdk/platform-tools/adb shell am start -n com.setlist.debug/com.setlist.DesignSystemActivity
```

[Tablet editor](screenshots/tablet-editor.png) · [Landscape reader](screenshots/tablet-reader.png) · [Portrait reader](screenshots/tablet-reader-portrait.png)

Swipe left on the score to advance a page; swipe right to go back. At a song boundary, swiping continues to the adjacent song. The last page opens the set-complete dialog. Taps, short movements and vertical drags do not turn pages. Buttons remain available.

Saved-song results show 25 songs per page, with Previous/Next controls, a result range and a page count. Search and setlist-filter changes return to page one; rotation preserves the selected page.
