package com.yourstudio.aerolauncher.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.yourstudio.aerolauncher.R;

/**
 * In-launcher quick panel (not system QS): flashlight, brightness, Settings.Panel shortcuts.
 */
public final class QuickPanelController {

    public interface PermissionBridge {
        void requestCameraPermission();
    }

    private final Activity activity;
    private final View root;
    private final View card;
    private final MaterialSwitch flashlight;
    private final SeekBar brightness;
    private final PermissionBridge permissionBridge;

    @Nullable
    private String torchCameraId;
    private boolean open;
    private boolean suppressBrightnessCallback;
    private boolean suppressTorchCallback;

    public QuickPanelController(
            @NonNull Activity activity,
            @NonNull View overlay,
            @NonNull PermissionBridge permissionBridge) {
        this.activity = activity;
        this.root = overlay;
        this.card = overlay.findViewById(R.id.quick_panel_card);
        this.flashlight = overlay.findViewById(R.id.quick_flashlight);
        this.brightness = overlay.findViewById(R.id.quick_brightness);
        this.permissionBridge = permissionBridge;

        root.setOnClickListener(v -> close());
        card.setOnClickListener(v -> {
            // Consume clicks so scrim does not close when tapping the panel.
        });

        flashlight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!suppressTorchCallback) {
                setTorch(isChecked);
            }
        });

        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !suppressBrightnessCallback) {
                    applyBrightness(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        MaterialButton wifi = overlay.findViewById(R.id.quick_wifi);
        MaterialButton bluetooth = overlay.findViewById(R.id.quick_bluetooth);
        MaterialButton sound = overlay.findViewById(R.id.quick_sound);
        wifi.setOnClickListener(v -> openPanel(Settings.Panel.ACTION_WIFI));
        bluetooth.setOnClickListener(v -> openPanel(Settings.Panel.ACTION_BLUETOOTH));
        sound.setOnClickListener(v -> openPanel(Settings.Panel.ACTION_VOLUME));
    }

    public boolean isOpen() {
        return open;
    }

    public void open() {
        if (open) {
            return;
        }
        open = true;
        refreshBrightnessSlider();
        root.setVisibility(View.VISIBLE);
        root.setAlpha(0f);
        card.setTranslationY(-card.getHeight() - 80f);
        root.animate().alpha(1f).setDuration(180).start();
        card.animate().translationY(0f).setDuration(220).start();
    }

    public void close() {
        if (!open) {
            return;
        }
        open = false;
        root.animate().alpha(0f).setDuration(150).withEndAction(() ->
                root.setVisibility(View.GONE)).start();
        card.animate().translationY(-120f).setDuration(150).start();
    }

    public void onCameraPermissionResult(boolean granted) {
        if (granted) {
            setFlashlightChecked(true);
            setTorch(true);
        } else {
            setFlashlightChecked(false);
            Toast.makeText(
                    activity,
                    R.string.quick_panel_camera_permission,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setTorch(boolean enabled) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (enabled) {
                setFlashlightChecked(false);
                permissionBridge.requestCameraPermission();
            }
            return;
        }
        try {
            CameraManager manager =
                    (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) {
                setFlashlightChecked(false);
                return;
            }
            if (torchCameraId == null) {
                torchCameraId = findTorchCameraId(manager);
            }
            if (torchCameraId == null) {
                setFlashlightChecked(false);
                return;
            }
            manager.setTorchMode(torchCameraId, enabled);
        } catch (Exception e) {
            setFlashlightChecked(false);
        }
    }

    private void setFlashlightChecked(boolean checked) {
        suppressTorchCallback = true;
        flashlight.setChecked(checked);
        suppressTorchCallback = false;
    }

    @Nullable
    private static String findTorchCameraId(@NonNull CameraManager manager) throws Exception {
        for (String id : manager.getCameraIdList()) {
            CameraCharacteristics chars = manager.getCameraCharacteristics(id);
            Boolean flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if (Boolean.TRUE.equals(flash)
                    && facing != null
                    && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }

    private void refreshBrightnessSlider() {
        try {
            int value = Settings.System.getInt(
                    activity.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
            suppressBrightnessCallback = true;
            brightness.setProgress(Math.max(0, Math.min(255, value)));
            suppressBrightnessCallback = false;
        } catch (Settings.SettingNotFoundException e) {
            brightness.setProgress(128);
        }
    }

    private void applyBrightness(int value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.System.canWrite(activity)) {
            Toast.makeText(
                    activity,
                    R.string.quick_panel_brightness_permission,
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
            return;
        }
        try {
            Settings.System.putInt(
                    activity.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    Math.max(0, Math.min(255, value)));
            WindowManager.LayoutParams params = activity.getWindow().getAttributes();
            params.screenBrightness = value / 255f;
            activity.getWindow().setAttributes(params);
        } catch (Exception ignored) {
        }
    }

    private void openPanel(@NonNull String action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Panels arrived in Q; fall back to full settings pages.
            if (Settings.Panel.ACTION_WIFI.equals(action)) {
                activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } else if (Settings.Panel.ACTION_BLUETOOTH.equals(action)) {
                activity.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            } else {
                activity.startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
            }
            return;
        }
        try {
            activity.startActivity(new Intent(action));
        } catch (Exception e) {
            if (Settings.Panel.ACTION_WIFI.equals(action)) {
                activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } else if (Settings.Panel.ACTION_BLUETOOTH.equals(action)) {
                activity.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            } else {
                activity.startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
            }
        }
    }
}
