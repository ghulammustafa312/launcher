package com.yourstudio.aerolauncher.launcher;

import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.yourstudio.aerolauncher.R;

/**
 * Maps {@link ApplicationInfo#category} (plus light package heuristics for Finance)
 * into App Library sections.
 */
public enum AppCategory {
    SOCIAL(R.string.library_social),
    ENTERTAINMENT(R.string.library_entertainment),
    FINANCE(R.string.library_finance),
    UTILITIES(R.string.library_utilities),
    GAMES(R.string.library_games),
    OTHER(R.string.library_other);

    @StringRes
    public final int titleRes;

    AppCategory(@StringRes int titleRes) {
        this.titleRes = titleRes;
    }

    @NonNull
    public static AppCategory from(@NonNull String packageName, @NonNull CharSequence label, int category) {
        String haystack = (packageName + " " + label).toLowerCase();
        if (looksLikeFinance(haystack)) {
            return FINANCE;
        }
        switch (category) {
            case ApplicationInfo.CATEGORY_SOCIAL:
                return SOCIAL;
            case ApplicationInfo.CATEGORY_AUDIO:
            case ApplicationInfo.CATEGORY_VIDEO:
            case ApplicationInfo.CATEGORY_IMAGE:
                return ENTERTAINMENT;
            case ApplicationInfo.CATEGORY_GAME:
                return GAMES;
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:
            case ApplicationInfo.CATEGORY_ACCESSIBILITY:
            case ApplicationInfo.CATEGORY_MAPS:
                return UTILITIES;
            case ApplicationInfo.CATEGORY_NEWS:
            default:
                return OTHER;
        }
    }

    private static boolean looksLikeFinance(@NonNull String haystack) {
        return haystack.contains("bank")
                || haystack.contains("finance")
                || haystack.contains("wallet")
                || haystack.contains("payment")
                || haystack.contains("pay.")
                || haystack.contains(".pay")
                || haystack.contains("invest")
                || haystack.contains("crypto")
                || haystack.contains("budget")
                || haystack.contains("money");
    }
}
