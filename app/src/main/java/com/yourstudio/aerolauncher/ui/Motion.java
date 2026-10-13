package com.yourstudio.aerolauncher.ui;

import android.content.Context;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * Springs and haptics for icon taps and drags. Animations are skipped when the
 * system "remove animations" / animator duration scale is off.
 */
public final class Motion {

    private Motion() {
    }

    public static boolean reduced(@NonNull Context context) {
        float scale = Settings.Global.getFloat(
                context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f);
        return scale == 0f;
    }

    public static void pop(@NonNull View view) {
        if (reduced(view.getContext())) {
            view.setScaleX(1f);
            view.setScaleY(1f);
            return;
        }
        view.setScaleX(0.86f);
        view.setScaleY(0.86f);
        springScale(view, 1f);
    }

    public static void springScale(@NonNull View view, float target) {
        if (reduced(view.getContext())) {
            view.setScaleX(target);
            view.setScaleY(target);
            return;
        }
        springAxis(view, DynamicAnimation.SCALE_X, target);
        springAxis(view, DynamicAnimation.SCALE_Y, target);
    }

    public static void tick(@NonNull View view) {
        view.performHapticFeedback(
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private static void springAxis(
            @NonNull View view,
            @NonNull DynamicAnimation.ViewProperty property,
            float target) {
        SpringForce force = new SpringForce(target)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_MEDIUM);
        SpringAnimation animation = new SpringAnimation(view, property);
        animation.setSpring(force);
        animation.start();
    }
}
