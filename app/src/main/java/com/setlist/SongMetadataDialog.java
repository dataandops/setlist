package com.setlist;

import android.app.Activity;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

final class SongMetadataDialog {
    static void show(Activity activity, Library library, Models.Song song, Runnable updated) {
        ScrollView scroll = new ScrollView(activity); LinearLayout form = Ui.column(activity); Ui.pad(form, 24); scroll.addView(form);
        TextInputEditText artist = field(form, "Artist", song.artist(), 120);
        TextInputEditText key = field(form, "Musical key", song.key(), 40);
        TextInputEditText bpm = field(form, "Tempo (BPM)", song.bpm(), 6); bpm.setInputType(InputType.TYPE_CLASS_NUMBER);
        TextInputEditText notes = field(form, "Notes", song.notes(), 2000); notes.setSingleLine(false); notes.setMinLines(2); notes.setMaxLines(5); notes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setTitle("Edit metadata").setView(scroll).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try { library.updateMetadata(song.id(), artist.getText().toString(), key.getText().toString(), bpm.getText().toString(), notes.getText().toString()); dialog.dismiss(); updated.run(); }
            catch (IllegalArgumentException e) { bpm.setError(e.getMessage()); }
        })); dialog.show();
    }
    private static TextInputEditText field(LinearLayout form, String label, String value, int limit) {
        TextInputLayout wrapper = new TextInputLayout(form.getContext()); wrapper.setHint(label); wrapper.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText input = new TextInputEditText(form.getContext()); input.setSingleLine(true); input.setText(value); input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        wrapper.addView(input); form.addView(wrapper); Ui.gap(form, 12); return input;
    }
}
