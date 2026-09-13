"""Freeze Google's Apache-2.0 Material Symbols Rounded as native vector assets."""
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
names = ["search", "queue_music", "add", "arrow_back", "chevron_right", "chevron_left", "play_arrow", "pause", "drag_indicator", "library_music", "more_horiz", "folder_open", "check", "music_note", "graphic_eq", "edit", "close", "skip_next", "description"]
for name in names:
    if (root / f"app/src/main/res/drawable/ic_{name}.xml").exists():
        continue
    url = f"https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/{name}/default/24px.svg"
    svg = ET.fromstring(subprocess.check_output(["curl", "-fsSL", url]))
    x, y, width, height = map(float, svg.attrib["viewBox"].split())
    paths = "\n".join(f'        <path android:fillColor="@color/stage_text" android:pathData="{element.attrib["d"]}" />' for element in svg.iter() if element.tag.endswith("path"))
    (root / f"app/src/main/res/drawable/ic_{name}.xml").write_text(f'''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="{width:g}" android:viewportHeight="{height:g}">
    <group android:translateX="{-x:g}" android:translateY="{-y:g}">
{paths}
    </group>
</vector>
''')
subprocess.run(["curl", "-fsSL", "https://raw.githubusercontent.com/google/material-design-icons/master/LICENSE", "-o", str(root / "app/src/main/assets/licenses/Material-Symbols-Apache-2.0.txt")], check=True)
print(f"Saved {len(names)} native rounded icons and license.")
