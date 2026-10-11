package com.yourstudio.aerolauncher.launcher;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Prompts the user to set Aero Launcher as the default home app.
 * Uses {@link RoleManager#ROLE_HOME} on API 29+, otherwise HOME settings.
 */
public final class DefaultLauncherHelper {

    private DefaultLauncherHelper() {
    }

    public static boolean isDefaultLauncher(@NonNull Activity activity) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = activity.getPackageManager()
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return false;
        }
        return activity.getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    /**
     * Starts the system role / settings UI so the user can choose this app as Home.
     *
     * @param roleLauncher launcher for {@link RoleManager#createRequestRoleIntent} results (API 29+)
     */
    public static void requestDefaultLauncher(
            @NonNull Activity activity,
            @NonNull ActivityResultLauncher<Intent> roleLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (requestHomeRole(activity, roleLauncher)) {
                return;
            }
        }
        openHomeSettings(activity);
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static boolean requestHomeRole(
            @NonNull Activity activity,
            @NonNull ActivityResultLauncher<Intent> roleLauncher) {
        RoleManager roleManager = activity.getSystemService(RoleManager.class);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return false;
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            return true;
        }
        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
        roleLauncher.launch(intent);
        return true;
    }

    public static void openHomeSettings(@NonNull Activity activity) {
        try {
            activity.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            return;
        } catch (Exception ignored) {
            // Older devices may lack ACTION_HOME_SETTINGS.
        }
        Intent chooser = new Intent(Intent.ACTION_MAIN);
        chooser.addCategory(Intent.CATEGORY_HOME);
        chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(Intent.createChooser(
                    chooser,
                    activity.getString(com.yourstudio.aerolauncher.R.string.set_default_launcher)));
        } catch (Exception ignored) {
            // No home picker available.
        }
    }
}
