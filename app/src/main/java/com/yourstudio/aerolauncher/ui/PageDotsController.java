package com.yourstudio.aerolauncher.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.R;

/**
 * Simple page indicator dots under the home ViewPager2.
 */
public final class PageDotsController {

    private final LinearLayout container;
    private final int dotSizePx;
    private final int dotMarginPx;
    private int selected;

    public PageDotsController(@NonNull LinearLayout container) {
        this.container = container;
        Context context = container.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        dotSizePx = Math.round(8f * density);
        dotMarginPx = Math.round(5f * density);
    }

    public void setPageCount(int count) {
        container.removeAllViews();
        int pages = Math.max(1, count);
        for (int i = 0; i < pages; i++) {
            View dot = new View(container.getContext());
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dotSizePx, dotSizePx);
            params.setMarginStart(dotMarginPx);
            params.setMarginEnd(dotMarginPx);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.page_dot);
            container.addView(dot);
        }
        setSelected(selected);
    }

    public void setSelected(int index) {
        selected = index;
        int childCount = container.getChildCount();
        for (int i = 0; i < childCount; i++) {
            container.getChildAt(i).setSelected(i == index);
        }
    }
}
