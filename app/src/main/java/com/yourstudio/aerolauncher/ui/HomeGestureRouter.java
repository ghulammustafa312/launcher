package com.yourstudio.aerolauncher.ui;

import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

/**
 * Observes home gestures without stealing touches from ViewPager2 / RecyclerView.
 * Call {@link #observe(MotionEvent)} from {@code Activity.dispatchTouchEvent}.
 * Top-edge swipe-down opens the in-launcher quick panel; swipe-down elsewhere opens search.
 */
public final class HomeGestureRouter {

    public interface Callbacks {
        void onSwipeDownSearch();

        void onSwipeDownQuickPanel();

        void onSwipeUpLibrary();

        void onDoubleTapLock();

        void onPinchSettings();

        boolean gesturesEnabled();
    }

    private final GestureDetectorCompat gestureDetector;
    private final ScaleGestureDetector scaleDetector;
    private final Callbacks callbacks;
    private final int topEdgePx;
    private boolean pinchHandled;
    private float downY;

    public HomeGestureRouter(@NonNull View host, @NonNull Callbacks callbacks) {
        this.callbacks = callbacks;
        topEdgePx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                72,
                host.getResources().getDisplayMetrics()));
        gestureDetector = new GestureDetectorCompat(host.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(@NonNull MotionEvent e) {
                        downY = e.getY();
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(@NonNull MotionEvent e) {
                        if (callbacks.gesturesEnabled()) {
                            callbacks.onDoubleTapLock();
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean onFling(
                            MotionEvent e1,
                            @NonNull MotionEvent e2,
                            float velocityX,
                            float velocityY) {
                        if (!callbacks.gesturesEnabled() || e1 == null) {
                            return false;
                        }
                        float dy = e2.getY() - e1.getY();
                        float dx = e2.getX() - e1.getX();
                        if (Math.abs(dy) < Math.abs(dx) * 1.25f) {
                            return false;
                        }
                        if (Math.abs(velocityY) < 900) {
                            return false;
                        }
                        if (dy > 140) {
                            if (downY <= topEdgePx || e1.getY() <= topEdgePx) {
                                callbacks.onSwipeDownQuickPanel();
                            } else {
                                callbacks.onSwipeDownSearch();
                            }
                            return true;
                        }
                        if (dy < -140) {
                            callbacks.onSwipeUpLibrary();
                            return true;
                        }
                        return false;
                    }
                });
        scaleDetector = new ScaleGestureDetector(host.getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                        pinchHandled = false;
                        return callbacks.gesturesEnabled();
                    }

                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        if (!pinchHandled && detector.getScaleFactor() < 0.85f) {
                            pinchHandled = true;
                            callbacks.onPinchSettings();
                            return true;
                        }
                        return false;
                    }
                });
    }

    public void observe(@NonNull MotionEvent event) {
        if (!callbacks.gesturesEnabled()) {
            return;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downY = event.getY();
        }
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
    }
}
