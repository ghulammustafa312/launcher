package com.yourstudio.aerolauncher;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yourstudio.aerolauncher.ui.EdgeToEdge;

/**
 * Explains why AccessibilityService is needed for double-tap lock, then opens system settings.
 */
public class LockOptInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemePreferences.applyAccentOverlay(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_opt_in);
        EdgeToEdge.apply(this, findViewById(R.id.root));

        findViewById(R.id.open_accessibility).setOnClickListener(v -> {
            FeaturePreferences.setOptedInToLock(this, true);
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });
        findViewById(R.id.not_now).setOnClickListener(v -> {
            FeaturePreferences.setOptedInToLock(this, false);
            finish();
        });
    }
}
