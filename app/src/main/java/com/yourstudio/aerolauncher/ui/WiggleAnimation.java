package com.yourstudio.aerolauncher.ui;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import androidx.annotation.NonNull;

/**
 * Subtle continuous wiggle used while the home screen is in edit mode.
 */
public final class WiggleAnimation {

    private static final float DEGREES = 3f;
    private static final long DURATION_MS = 120L;

    private WiggleAnimation() {
    }

    public static void start(@NonNull View view) {
        if (view.getAnimation() instanceof RotateAnimation) {
            return;
        }
        RotateAnimation animation = new RotateAnimation(
                -DEGREES,
                DEGREES,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        animation.setDuration(DURATION_MS);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(Animation.INFINITE);
        // Slight phase offset per view so icons do not sync perfectly.
        animation.setStartOffset(Math.abs(view.hashCode() % 80));
        view.startAnimation(animation);
    }

    public static void stop(@NonNull View view) {
        view.clearAnimation();
        view.setRotation(0f);
    }
}
