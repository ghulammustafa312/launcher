package com.yourstudio.aerolauncher;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.Nullable;

/**
 * Optional AccessibilityService used only to lock the screen on double-tap.
 * Enabled solely after the user opts in on {@link LockOptInActivity}.
 */
public class LockAccessibilityService extends AccessibilityService {

    @Nullable
    private static LockAccessibilityService instance;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No event processing; this service exists only for GLOBAL_ACTION_LOCK_SCREEN.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        FeaturePreferences.setLockServiceReportedEnabled(this, true);
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        FeaturePreferences.setLockServiceReportedEnabled(this, false);
        super.onDestroy();
    }

    public static boolean isConnected() {
        return instance != null;
    }

    /**
     * @return true if the lock action was performed
     */
    public static boolean lockScreen() {
        LockAccessibilityService service = instance;
        if (service == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
        return false;
    }
}
