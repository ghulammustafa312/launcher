package com.yourstudio.aerolauncher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yourstudio.aerolauncher.layout.HomeLayout;
import com.yourstudio.aerolauncher.layout.LayoutBackup;
import com.yourstudio.aerolauncher.layout.LayoutRepository;
import com.yourstudio.aerolauncher.ui.EdgeToEdge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Settings: theme, accent, icons, wallpaper, privacy, focus, badges, grid, backup.
 */
public class SettingsActivity extends AppCompatActivity {

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private TextView columnsLabel;
    private TextView rowsLabel;
    private int columns;
    private int rows;
    private LayoutRepository layoutRepository;

    private final ActivityResultLauncher<String> createBackup =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(LayoutBackup.MIME_TYPE),
                    this::onBackupUri);

    private final ActivityResultLauncher<String[]> openBackup =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onRestoreUri);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemePreferences.applyAccentOverlay(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EdgeToEdge.apply(this, findViewById(R.id.root));
        layoutRepository = ((AeroLauncherApp) getApplication()).getLayoutRepository();

        bindThemeControls();
        bindIconStyleControls();
        bindAccentSwatches();

        columns = HomePreferences.getColumns(this);
        rows = HomePreferences.getRows(this);
        columnsLabel = findViewById(R.id.grid_columns_label);
        rowsLabel = findViewById(R.id.grid_rows_label);
        refreshGridLabels();

        findViewById(R.id.columns_minus).setOnClickListener(v -> adjustGrid(-1, 0));
        findViewById(R.id.columns_plus).setOnClickListener(v -> adjustGrid(1, 0));
        findViewById(R.id.rows_minus).setOnClickListener(v -> adjustGrid(0, -1));
        findViewById(R.id.rows_plus).setOnClickListener(v -> adjustGrid(0, 1));

        findViewById(R.id.setup_lock).setOnClickListener(v ->
                startActivity(new Intent(this, LockOptInActivity.class)));
        findViewById(R.id.open_wallpaper).setOnClickListener(v ->
                startActivity(new Intent(this, WallpaperActivity.class)));
        findViewById(R.id.open_hidden_apps).setOnClickListener(v ->
                startActivity(new Intent(this, HiddenAppsActivity.class)));
        findViewById(R.id.open_focus).setOnClickListener(v ->
                startActivity(new Intent(this, FocusModeActivity.class)));
        findViewById(R.id.open_badges).setOnClickListener(v ->
                startActivity(new Intent(this, BadgeOptInActivity.class)));

        findViewById(R.id.backup_layout).setOnClickListener(v ->
                createBackup.launch(LayoutBackup.DEFAULT_FILE_NAME));
        findViewById(R.id.restore_layout).setOnClickListener(v ->
                openBackup.launch(new String[]{LayoutBackup.MIME_TYPE, "application/*", "text/*"}));
    }

    private void bindThemeControls() {
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

    private void bindIconStyleControls() {
        RadioGroup group = findViewById(R.id.icon_style_group);
        RadioButton rounded = findViewById(R.id.icon_rounded);
        RadioButton circle = findViewById(R.id.icon_circle);
        RadioButton squircle = findViewById(R.id.icon_squircle);
        switch (ThemePreferences.getIconStyle(this)) {
            case ThemePreferences.ICON_CIRCLE:
                circle.setChecked(true);
                break;
            case ThemePreferences.ICON_SQUIRCLE:
                squircle.setChecked(true);
                break;
            case ThemePreferences.ICON_ROUNDED:
            default:
                rounded.setChecked(true);
                break;
        }
        group.setOnCheckedChangeListener((g, checkedId) -> {
            if (checkedId == R.id.icon_circle) {
                ThemePreferences.setIconStyle(this, ThemePreferences.ICON_CIRCLE);
            } else if (checkedId == R.id.icon_squircle) {
                ThemePreferences.setIconStyle(this, ThemePreferences.ICON_SQUIRCLE);
            } else {
                ThemePreferences.setIconStyle(this, ThemePreferences.ICON_ROUNDED);
            }
            ((AeroLauncherApp) getApplication()).getAppRepository().getIconCache().invalidateAllShapes();
        });
    }

    private void bindAccentSwatches() {
        findViewById(R.id.swatch_midnight).setOnClickListener(v ->
                applyAccent(ThemePreferences.ACCENT_MIDNIGHT));
        findViewById(R.id.swatch_violet).setOnClickListener(v ->
                applyAccent(ThemePreferences.ACCENT_VIOLET));
        findViewById(R.id.swatch_aqua).setOnClickListener(v ->
                applyAccent(ThemePreferences.ACCENT_AQUA));
        findViewById(R.id.swatch_orange).setOnClickListener(v ->
                applyAccent(ThemePreferences.ACCENT_ORANGE));
    }

    private void applyAccent(@NonNull String accent) {
        ThemePreferences.setAccent(this, accent);
        recreate();
    }

    @Override
    protected void onDestroy() {
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

    private void adjustGrid(int columnDelta, int rowDelta) {
        HomePreferences.setGridSize(
                this, HomePreferences.DEFAULT_COLUMNS, HomePreferences.DEFAULT_ROWS);
        columns = HomePreferences.getColumns(this);
        rows = HomePreferences.getRows(this);
        refreshGridLabels();
    }

    private void refreshGridLabels() {
        columnsLabel.setText(getString(R.string.grid_columns, columns));
        rowsLabel.setText(getString(R.string.grid_rows, rows));
    }

    private void onBackupUri(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        HomeLayout layout = LayoutRepository.deepCopy(layoutRepository.getLayout());
        ioExecutor.execute(() -> {
            try {
                LayoutBackup.exportToUri(this, uri, layout);
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.backup_success, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.backup_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void onRestoreUri(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                HomeLayout restored = LayoutBackup.importFromUri(this, uri);
                runOnUiThread(() -> {
                    layoutRepository.replaceFromBackup(restored);
                    Toast.makeText(this, R.string.restore_success, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
