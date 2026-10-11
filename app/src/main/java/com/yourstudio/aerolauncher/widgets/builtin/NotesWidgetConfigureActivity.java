package com.yourstudio.aerolauncher.widgets.builtin;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.ui.EdgeToEdge;

/**
 * Configure / edit a local notes widget.
 */
public class NotesWidgetConfigureActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_notes_widget_configure);
        View root = findViewById(R.id.notes_configure_root);
        EdgeToEdge.apply(this, root != null ? root : findViewById(android.R.id.content));

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        EditText input = findViewById(R.id.notes_input);
        input.setText(NotesStorage.get(this, appWidgetId));
        MaterialButton save = findViewById(R.id.notes_save);
        save.setOnClickListener(v -> {
            String text = input.getText() != null ? input.getText().toString() : "";
            NotesStorage.set(this, appWidgetId, text);
            NotesWidgetProvider.update(this, AppWidgetManager.getInstance(this), appWidgetId);
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, result);
            finish();
        });
    }
}
