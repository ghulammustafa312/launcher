package com.yourstudio.aerolauncher.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.yourstudio.aerolauncher.R;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Applies built-in or custom wallpapers with optional blur.
 */
public final class WallpaperController {

    private final Context appContext;
    private final ImageView target;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SensorEventListener parallaxListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
                return;
            }
            float[] rotation = new float[9];
            float[] orientation = new float[3];
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            SensorManager.getOrientation(rotation, orientation);
            float density = target.getResources().getDisplayMetrics().density;
            float max = 12f * density;
            float x = clamp(orientation[2] * 8f * density, -max, max);
            float y = clamp(-orientation[1] * 8f * density, -max, max);
            target.post(() -> {
                target.setTranslationX(x);
                target.setTranslationY(y);
            });
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private int loadToken;
    private SensorManager sensorManager;
    private boolean parallaxRegistered;

    public WallpaperController(@NonNull Context context, @NonNull ImageView target) {
        this.appContext = context.getApplicationContext();
        this.target = target;
        this.target.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    public void refresh() {
        String source = WallpaperPreferences.getSource(appContext);
        int blur = WallpaperPreferences.getBlur(appContext);
        final int token = ++loadToken;

        clearBlur();
        if (WallpaperPreferences.SOURCE_NONE.equals(source)) {
            target.setImageDrawable(null);
            target.setVisibility(View.GONE);
            return;
        }
        target.setVisibility(View.VISIBLE);

        if (WallpaperPreferences.SOURCE_BUILTIN.equals(source)) {
            Drawable drawable = ContextCompat.getDrawable(
                    appContext, builtinRes(WallpaperPreferences.getBuiltinId(appContext)));
            if (drawable == null) {
                target.setVisibility(View.GONE);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || blur <= 0) {
                target.setImageDrawable(drawable);
                applyHardwareBlur(blur);
            } else {
                executor.execute(() -> {
                    Bitmap bitmap = drawableToBitmap(drawable, 1080, 1920);
                    Bitmap blurred = softBlur(bitmap, blur);
                    target.post(() -> {
                        if (token != loadToken) {
                            return;
                        }
                        target.setImageBitmap(blurred);
                    });
                });
            }
            return;
        }

        Uri uri = WallpaperPreferences.getCustomUri(appContext);
        if (uri == null) {
            target.setVisibility(View.GONE);
            return;
        }
        executor.execute(() -> {
            Bitmap bitmap = decodeUri(uri);
            target.post(() -> {
                if (token != loadToken) {
                    return;
                }
                if (bitmap == null) {
                    target.setVisibility(View.GONE);
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || blur <= 0) {
                    target.setImageBitmap(bitmap);
                    applyHardwareBlur(blur);
                } else {
                    target.setImageBitmap(softBlur(bitmap, blur));
                }
            });
        });
    }

    private void applyHardwareBlur(int blur) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        if (blur <= 0) {
            target.setRenderEffect(null);
        } else {
            float radius = blur * 1.6f;
            target.setRenderEffect(
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }
    }

    private void clearBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            target.setRenderEffect(null);
        }
    }

    @NonNull
    static Bitmap softBlur(@NonNull Bitmap source, int blur) {
        int sample = Math.max(2, Math.min(12, blur / 2 + 2));
        int w = Math.max(1, source.getWidth() / sample);
        int h = Math.max(1, source.getHeight() / sample);
        Bitmap small = Bitmap.createScaledBitmap(source, w, h, true);
        return Bitmap.createScaledBitmap(small, source.getWidth(), source.getHeight(), true);
    }

    @NonNull
    private static Bitmap drawableToBitmap(@NonNull Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    @Nullable
    private Bitmap decodeUri(@NonNull Uri uri) {
        try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
            return BitmapFactory.decodeStream(in, null, opts);
        } catch (Exception e) {
            return null;
        }
    }

    private static int builtinRes(@NonNull String id) {
        switch (id) {
            case WallpaperPreferences.BUILTIN_AURORA:
                return R.drawable.wallpaper_aurora;
            case WallpaperPreferences.BUILTIN_MIST:
                return R.drawable.wallpaper_mist;
            case WallpaperPreferences.BUILTIN_EMBER:
                return R.drawable.wallpaper_ember;
            case WallpaperPreferences.BUILTIN_DUSK:
            default:
                return R.drawable.wallpaper_dusk;
        }
    }

    /**
     * Optional tilt parallax. Unregistered unless the user turns it on, and the
     * listener uses the slowest sensor rate.
     */
    public void setParallaxEnabled(boolean enabled) {
        if (sensorManager == null) {
            sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        }
        if (sensorManager != null && parallaxRegistered) {
            sensorManager.unregisterListener(parallaxListener);
            parallaxRegistered = false;
        }
        if (!enabled || sensorManager == null) {
            target.setTranslationX(0f);
            target.setTranslationY(0f);
            target.setScaleX(1f);
            target.setScaleY(1f);
            return;
        }
        Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotation == null) {
            return;
        }
        target.setScaleX(1.06f);
        target.setScaleY(1.06f);
        parallaxRegistered = sensorManager.registerListener(
                parallaxListener,
                rotation,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void shutdown() {
        setParallaxEnabled(false);
        executor.shutdownNow();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
