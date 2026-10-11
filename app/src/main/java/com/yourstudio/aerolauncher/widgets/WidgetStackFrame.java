package com.yourstudio.aerolauncher.widgets;

import android.content.Context;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import com.yourstudio.aerolauncher.R;

import java.util.ArrayList;
import java.util.List;

/**
 * One widget zone cell: a single widget or a swipeable stack with page dots.
 */
public final class WidgetStackFrame extends FrameLayout {

    public interface Listener {
        void onRemove(@NonNull WidgetPlacement placement);

        void onResize(@NonNull WidgetPlacement placement, int spanRows);

        void onLongPress(@NonNull WidgetPlacement placement, @NonNull View anchor);
    }

    private static final int ROW_HEIGHT_DP = 72;

    private final FrameLayout hostContainer;
    private final LinearLayout dots;
    private final LinearLayout editBar;
    private final ImageButton resizeUp;
    private final ImageButton resizeDown;
    private final ImageButton remove;
    private final GestureDetectorCompat flingDetector;
    private final List<View> pageViews = new ArrayList<>();
    private final int touchSlop;

    private WidgetPlacement placement;
    private Listener listener;
    private int currentIndex;
    private float downX;
    private float downY;
    private boolean interceptingSwipe;

    public WidgetStackFrame(@NonNull Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.item_widget_frame, this, true);
        hostContainer = findViewById(R.id.widget_host_container);
        dots = findViewById(R.id.widget_stack_dots);
        editBar = findViewById(R.id.widget_edit_bar);
        resizeUp = findViewById(R.id.widget_resize_up);
        resizeDown = findViewById(R.id.widget_resize_down);
        remove = findViewById(R.id.widget_remove);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        flingDetector = new GestureDetectorCompat(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(@NonNull MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onFling(
                            MotionEvent e1,
                            @NonNull MotionEvent e2,
                            float velocityX,
                            float velocityY) {
                        if (pageViews.size() < 2) {
                            return false;
                        }
                        if (Math.abs(velocityX) < Math.abs(velocityY)) {
                            return false;
                        }
                        if (velocityX < -400 && currentIndex < pageViews.size() - 1) {
                            showPage(currentIndex + 1);
                            return true;
                        }
                        if (velocityX > 400 && currentIndex > 0) {
                            showPage(currentIndex - 1);
                            return true;
                        }
                        return false;
                    }
                });

        resizeUp.setOnClickListener(v -> {
            if (placement != null && listener != null && placement.spanRows < 4) {
                listener.onResize(placement, placement.spanRows + 1);
            }
        });
        resizeDown.setOnClickListener(v -> {
            if (placement != null && listener != null && placement.spanRows > 2) {
                listener.onResize(placement, placement.spanRows - 1);
            }
        });
        remove.setOnClickListener(v -> {
            if (placement != null && listener != null) {
                listener.onRemove(placement);
            }
        });
        setOnLongClickListener(v -> {
            if (listener != null && placement != null) {
                listener.onLongPress(placement, this);
                return true;
            }
            return false;
        });
    }

    public void bind(
            @NonNull WidgetPlacement placement,
            @NonNull WidgetHostManager hostManager,
            boolean editMode,
            @Nullable Listener listener) {
        this.placement = placement;
        this.listener = listener;
        hostContainer.removeAllViews();
        pageViews.clear();
        currentIndex = 0;
        interceptingSwipe = false;

        int heightPx = dp(placement.spanRows * ROW_HEIGHT_DP);
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp == null) {
            lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        } else {
            lp.height = heightPx;
        }
        setLayoutParams(lp);

        int widthDp = Math.max(110, pxToDp(getResources().getDisplayMetrics().widthPixels) - 32);
        int heightDp = placement.spanRows * ROW_HEIGHT_DP;

        for (Integer appWidgetId : placement.appWidgetIds) {
            View hostView = hostManager.createView(appWidgetId, widthDp, heightDp);
            if (hostView == null) {
                continue;
            }
            hostView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            hostView.setVisibility(View.GONE);
            hostContainer.addView(hostView);
            pageViews.add(hostView);
        }

        if (!pageViews.isEmpty()) {
            showPage(0);
        }
        bindDots();
        editBar.setVisibility(editMode ? View.VISIBLE : View.GONE);
        resizeUp.setEnabled(placement.spanRows < 4);
        resizeDown.setEnabled(placement.spanRows > 2);
    }

    private void showPage(int index) {
        if (index < 0 || index >= pageViews.size()) {
            return;
        }
        currentIndex = index;
        for (int i = 0; i < pageViews.size(); i++) {
            pageViews.get(i).setVisibility(i == index ? View.VISIBLE : View.GONE);
        }
        for (int i = 0; i < dots.getChildCount(); i++) {
            dots.getChildAt(i).setSelected(i == index);
        }
    }

    private void bindDots() {
        dots.removeAllViews();
        boolean show = pageViews.size() > 1;
        dots.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }
        int size = dp(7);
        int margin = dp(3);
        for (int i = 0; i < pageViews.size(); i++) {
            View dot = new View(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(size, size);
            layoutParams.setMarginStart(margin);
            layoutParams.setMarginEnd(margin);
            dot.setLayoutParams(layoutParams);
            dot.setBackgroundResource(R.drawable.widget_stack_dot);
            dot.setSelected(i == currentIndex);
            final int page = i;
            dot.setOnClickListener(v -> showPage(page));
            dots.addView(dot);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (pageViews.size() < 2) {
            return super.onInterceptTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                interceptingSwipe = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - downX);
                float dy = Math.abs(ev.getY() - downY);
                if (dx > touchSlop && dx > dy) {
                    interceptingSwipe = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            default:
                break;
        }
        return interceptingSwipe || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (pageViews.size() > 1 && interceptingSwipe) {
            flingDetector.onTouchEvent(event);
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                // Also advance on drag distance when fling velocity is low.
                float dx = event.getX() - downX;
                if (Math.abs(dx) > dp(48)) {
                    if (dx < 0 && currentIndex < pageViews.size() - 1) {
                        showPage(currentIndex + 1);
                    } else if (dx > 0 && currentIndex > 0) {
                        showPage(currentIndex - 1);
                    }
                }
                interceptingSwipe = false;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()));
    }

    private int pxToDp(int px) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(px / density);
    }
}
