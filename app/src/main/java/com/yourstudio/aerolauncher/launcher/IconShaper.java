package com.yourstudio.aerolauncher.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yourstudio.aerolauncher.ThemePreferences;

/**
 * Draws adaptive and legacy icons onto one rounded-square plate, then masks
 * them with a continuous squircle whose corner radius is about 22% of the side.
 */
public final class IconShaper {

    /** Corner radius as a fraction of the icon side. */
    private static final float CORNER_FRACTION = 0.22f;
    /** Opaque glyph fills this fraction of the plate so every icon shares a scale. */
    private static final float CONTENT_FRACTION = 0.94f;
    /** Adaptive icon viewport is 108dp with a 25% inset outside the mask. */
    private static final float ADAPTIVE_INSET_FRACTION = 0.25f;
    private static final int PLATE_COLOR = 0xFFF2F3F5;

    private IconShaper() {
    }

    @Nullable
    public static synchronized Drawable apply(
            @Nullable Drawable source,
            @NonNull String style,
            int sizePx,
            @NonNull android.content.res.Resources resources) {
        if (source == null || sizePx <= 0) {
            return source;
        }
        Bitmap normalized = normalize(source, sizePx);
        Bitmap output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(new BitmapShader(normalized, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawPath(maskPath(style, sizePx), paint);
        normalized.recycle();
        return new BitmapDrawable(resources, output);
    }

    @NonNull
    private static Bitmap normalize(@NonNull Drawable source, int size) {
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(PLATE_COLOR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source instanceof AdaptiveIconDrawable) {
            drawAdaptive(canvas, (AdaptiveIconDrawable) source, size);
        } else {
            drawLegacy(canvas, source, size);
        }
        return output;
    }

    private static void drawAdaptive(
            @NonNull Canvas canvas,
            @NonNull AdaptiveIconDrawable adaptive,
            int size) {
        int extra = Math.round(size * ADAPTIVE_INSET_FRACTION);
        Drawable background = adaptive.getBackground();
        Drawable foreground = adaptive.getForeground();
        if (background != null) {
            Drawable layer = background.mutate();
            layer.setBounds(-extra, -extra, size + extra, size + extra);
            layer.draw(canvas);
        }
        if (foreground != null) {
            Drawable layer = foreground.mutate();
            layer.setBounds(-extra, -extra, size + extra, size + extra);
            layer.draw(canvas);
        }
    }

    private static void drawLegacy(@NonNull Canvas canvas, @NonNull Drawable source, int size) {
        Bitmap raw = drawableToBitmap(source, size);
        canvas.drawColor(plateColor(raw));
        Rect opaque = opaqueBounds(raw);
        float longest = Math.max(opaque.width(), opaque.height());
        float scale = longest <= 0 ? 1f : (size * CONTENT_FRACTION) / longest;
        float drawW = opaque.width() * scale;
        float drawH = opaque.height() * scale;
        float left = (size - drawW) / 2f;
        float top = (size - drawH) / 2f;
        RectF dest = new RectF(left, top, left + drawW, top + drawH);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(raw, opaque, dest, paint);
        raw.recycle();
    }

    @NonNull
    private static Path maskPath(@NonNull String style, int size) {
        Path path = new Path();
        RectF bounds = new RectF(0, 0, size, size);
        if (ThemePreferences.ICON_CIRCLE.equals(style)) {
            path.addOval(bounds, Path.Direction.CW);
            return path;
        }
        if (ThemePreferences.ICON_ROUNDED.equals(style)) {
            float radius = size * CORNER_FRACTION;
            path.addRoundRect(bounds, radius, radius, Path.Direction.CW);
            return path;
        }
        return continuousRoundedSquare(size);
    }

    /**
     * Squircle approximation: a rounded square at {@link #CORNER_FRACTION} with
     * longer bezier handles so the corner eases in instead of a circular arc.
     */
    @NonNull
    private static Path continuousRoundedSquare(int size) {
        float radius = size * CORNER_FRACTION;
        float handle = radius * 0.8f;
        float side = size;
        Path path = new Path();
        path.moveTo(radius, 0);
        path.lineTo(side - radius, 0);
        path.cubicTo(side - radius + handle, 0, side, radius - handle, side, radius);
        path.lineTo(side, side - radius);
        path.cubicTo(side, side - radius + handle, side - radius + handle, side, side - radius, side);
        path.lineTo(radius, side);
        path.cubicTo(radius - handle, side, 0, side - radius + handle, 0, side - radius);
        path.lineTo(0, radius);
        path.cubicTo(0, radius - handle, radius - handle, 0, radius, 0);
        path.close();
        return path;
    }

    private static int plateColor(@NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] corners = {
                bitmap.getPixel(0, 0),
                bitmap.getPixel(width - 1, 0),
                bitmap.getPixel(0, height - 1),
                bitmap.getPixel(width - 1, height - 1)
        };
        int red = 0;
        int green = 0;
        int blue = 0;
        int count = 0;
        for (int color : corners) {
            if (Color.alpha(color) < 200) {
                continue;
            }
            red += Color.red(color);
            green += Color.green(color);
            blue += Color.blue(color);
            count++;
        }
        if (count == 0) {
            return PLATE_COLOR;
        }
        return Color.rgb(red / count, green / count, blue / count);
    }

    @NonNull
    private static Rect opaqueBounds(@NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int left = width;
        int top = height;
        int right = 0;
        int bottom = 0;
        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width; x += 2) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 40) {
                    if (x < left) {
                        left = x;
                    }
                    if (y < top) {
                        top = y;
                    }
                    if (x > right) {
                        right = x;
                    }
                    if (y > bottom) {
                        bottom = y;
                    }
                }
            }
        }
        if (right <= left || bottom <= top) {
            return new Rect(0, 0, width, height);
        }
        return new Rect(left, top, Math.min(width, right + 2), Math.min(height, bottom + 2));
    }

    @NonNull
    private static Bitmap drawableToBitmap(@NonNull Drawable drawable, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable copy = drawable.mutate();
        copy.setBounds(0, 0, size, size);
        copy.draw(canvas);
        return bitmap;
    }
}
