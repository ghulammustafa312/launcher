package com.yourstudio.aerolauncher.ui;

import android.content.Context;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yourstudio.aerolauncher.R;

/**
 * Opaque dialogs for the transparent home window (PopupMenu inherits
 * the wallpaper theme and becomes unreadable).
 */
public final class HomeMenus {

    private HomeMenus() {
    }

    @NonNull
    public static Context opaque(@NonNull Context context) {
        return new ContextThemeWrapper(context, R.style.Theme_AeroLauncher_Menus);
    }

    @NonNull
    public static MaterialAlertDialogBuilder builder(@NonNull Context context) {
        return new MaterialAlertDialogBuilder(opaque(context));
    }

    public static void showActions(
            @NonNull Context context,
            @NonNull CharSequence title,
            @NonNull CharSequence[] labels,
            @NonNull AlertDialog.OnClickListener onClick) {
        builder(context)
                .setTitle(title)
                .setItems(labels, onClick)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
