package com.yourstudio.aerolauncher;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yourstudio.aerolauncher.ui.EdgeToEdge;

/**
 * Settings: theme mode, home grid size, and palette swatches.
 */
public class SettingsActivity extends AppCompatActivity {

    private TextView columnsLabel;
    private TextView rowsLabel;
    private int columns;
    private int rows;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EdgeToEdge.apply(this, findViewById(R.id.root));

        RadioGroup themeGroup = findViewById(R.id.theme_group);
        RadioButton light = findViewById(R.id.theme_light);
        RadioButton dark = findViewById(R.id.theme_dark);
        RadioButton auto = findViewById(R.id.theme_auto);

        switch (ThemePreferences.getThemeMode(this)) {
            case ThemePreferences.MODE_LIGHT:
                light.setChecked(true);
                break;
            case ThemePreferences.MODE_DARK:
                dark.setChecked(true);
                break;
            case ThemePreferences.MODE_AUTO:
            default:
                auto.setChecked(true);
                break;
        }

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.theme_light) {
                ThemePreferences.setThemeMode(this, ThemePreferences.MODE_LIGHT);
            } else if (checkedId == R.id.theme_dark) {
                ThemePreferences.setThemeMode(this, ThemePreferences.MODE_DARK);
            } else if (checkedId == R.id.theme_auto) {
                ThemePreferences.setThemeMode(this, ThemePreferences.MODE_AUTO);
            }
        });

        columns = HomePreferences.getColumns(this);
        rows = HomePreferences.getRows(this);
        columnsLabel = findViewById(R.id.grid_columns_label);
        rowsLabel = findViewById(R.id.grid_rows_label);
        refreshGridLabels();

        findViewById(R.id.columns_minus).setOnClickListener(v -> adjustGrid(-1, 0));
        findViewById(R.id.columns_plus).setOnClickListener(v -> adjustGrid(1, 0));
        findViewById(R.id.rows_minus).setOnClickListener(v -> adjustGrid(0, -1));
        findViewById(R.id.rows_plus).setOnClickListener(v -> adjustGrid(0, 1));
    }

    private void adjustGrid(int columnDelta, int rowDelta) {
        columns = Math.max(3, Math.min(6, columns + columnDelta));
        rows = Math.max(4, Math.min(8, rows + rowDelta));
        HomePreferences.setGridSize(this, columns, rows);
        refreshGridLabels();
    }

    private void refreshGridLabels() {
        columnsLabel.setText(getString(R.string.grid_columns, columns));
        rowsLabel.setText(getString(R.string.grid_rows, rows));
    }
}
