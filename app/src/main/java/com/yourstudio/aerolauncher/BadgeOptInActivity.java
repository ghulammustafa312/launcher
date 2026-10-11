package com.yourstudio.aerolauncher;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yourstudio.aerolauncher.privacy.BadgePreferences;
import com.yourstudio.aerolauncher.ui.EdgeToEdge;

/**
 * Explains notification badges before the user enables NotificationListener access.
 */
public class BadgeOptInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemePreferences.applyAccentOverlay(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badge_opt_in);
        EdgeToEdge.apply(this, findViewById(R.id.root));

        findViewById(R.id.badge_enable).setOnClickListener(v -> {
            BadgePreferences.setOptedIn(this, true);
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        });
        findViewById(R.id.badge_disable).setOnClickListener(v -> {
            BadgePreferences.setOptedIn(this, false);
            finish();
        });
    }
}
