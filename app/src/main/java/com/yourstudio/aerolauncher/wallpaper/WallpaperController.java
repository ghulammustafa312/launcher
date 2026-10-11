package com.yourstudio.aerolauncher.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
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
    private int loadToken;

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

    public void shutdown() {
        executor.shutdownNow();
    }
}
