package com.yourstudio.aerolauncher.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Host view that cooperates better with parent scroll/swipe (stacks, ViewPager).
 */
public final class LauncherAppWidgetHostView extends AppWidgetHostView {

    private float downX;
    private float downY;
    private final int touchSlop;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - downX);
                float dy = Math.abs(ev.getY() - downY);
                if (dx > touchSlop || dy > touchSlop) {
                    // Allow ViewPager2 stacks to take horizontal swipes.
                    if (dx > dy) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
