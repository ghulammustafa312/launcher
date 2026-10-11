package com.yourstudio.aerolauncher.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yourstudio.aerolauncher.ThemePreferences;

/**
 * Applies adaptive-icon style masks: rounded square, circle, or squircle.
 */
public final class IconShaper {

    private IconShaper() {
    }

    @Nullable
    public static Drawable apply(
            @Nullable Drawable source,
            @NonNull String style,
            int sizePx,
            @NonNull android.content.res.Resources resources) {
        if (source == null || sizePx <= 0) {
            return source;
        }
        Bitmap bitmap = drawableToBitmap(source, sizePx);
        Path mask = maskPath(style, sizePx);
        Bitmap output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        canvas.drawPath(mask, paint);
        return new BitmapDrawable(resources, output);
    }

    @NonNull
    private static Path maskPath(@NonNull String style, int size) {
        Path path = new Path();
        RectF bounds = new RectF(0, 0, size, size);
        switch (style) {
            case ThemePreferences.ICON_CIRCLE:
                path.addOval(bounds, Path.Direction.CW);
                break;
            case ThemePreferences.ICON_SQUIRCLE:
                // Continuous corner-ish squircle approximation via rounded rect at ~42% radius.
                float squircle = size * 0.42f;
                path.addRoundRect(bounds, squircle, squircle, Path.Direction.CW);
                break;
            case ThemePreferences.ICON_ROUNDED:
            default:
                float radius = size * 0.22f;
                path.addRoundRect(bounds, radius, radius, Path.Direction.CW);
                break;
        }
        return path;
    }

    @NonNull
    private static Bitmap drawableToBitmap(@NonNull Drawable drawable, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return bitmap;
    }
}
