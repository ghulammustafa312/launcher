package com.yourstudio.aerolauncher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yourstudio.aerolauncher.ui.EdgeToEdge;
import com.yourstudio.aerolauncher.wallpaper.WallpaperPreferences;

/**
 * Built-in wallpapers, system photo picker, and blur strength.
 */
public class WallpaperActivity extends AppCompatActivity {

    private TextView blurLabel;

    private final ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoPicked);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemePreferences.applyAccentOverlay(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper);
        EdgeToEdge.apply(this, findViewById(R.id.root));

        findViewById(R.id.wallpaper_none).setOnClickListener(v -> {
            WallpaperPreferences.setNone(this);
            finish();
        });
        findViewById(R.id.wallpaper_dusk).setOnClickListener(v ->
                pickBuiltin(WallpaperPreferences.BUILTIN_DUSK));
        findViewById(R.id.wallpaper_aurora).setOnClickListener(v ->
                pickBuiltin(WallpaperPreferences.BUILTIN_AURORA));
        findViewById(R.id.wallpaper_mist).setOnClickListener(v ->
                pickBuiltin(WallpaperPreferences.BUILTIN_MIST));
        findViewById(R.id.wallpaper_ember).setOnClickListener(v ->
                pickBuiltin(WallpaperPreferences.BUILTIN_EMBER));
        findViewById(R.id.wallpaper_photo).setOnClickListener(v ->
                photoPicker.launch("image/*"));

        blurLabel = findViewById(R.id.wallpaper_blur_label);
        SeekBar blur = findViewById(R.id.wallpaper_blur);
        int current = WallpaperPreferences.getBlur(this);
        blur.setProgress(current);
        refreshBlurLabel(current);
        SwitchCompat parallax = findViewById(R.id.wallpaper_parallax);
        parallax.setChecked(WallpaperPreferences.isParallaxEnabled(this));
        parallax.setOnCheckedChangeListener((CompoundButton button, boolean checked) ->
                WallpaperPreferences.setParallax(this, checked));

        blur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    WallpaperPreferences.setBlur(WallpaperActivity.this, progress);
                    refreshBlurLabel(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void pickBuiltin(@NonNull String id) {
        WallpaperPreferences.setBuiltin(this, id);
        finish();
    }

    private void onPhotoPicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // Some providers only grant temporary read access for this session.
        }
        WallpaperPreferences.setCustomUri(this, uri);
        finish();
    }

    private void refreshBlurLabel(int value) {
        blurLabel.setText(getString(R.string.wallpaper_blur_value, value));
    }
}
