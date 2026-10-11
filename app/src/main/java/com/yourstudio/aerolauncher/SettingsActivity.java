package com.yourstudio.aerolauncher;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yourstudio.aerolauncher.ui.EdgeToEdge;

/**
 * Settings shell: light / dark / auto theme, plus palette swatches.
 */
public class SettingsActivity extends AppCompatActivity {

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
    }
}
