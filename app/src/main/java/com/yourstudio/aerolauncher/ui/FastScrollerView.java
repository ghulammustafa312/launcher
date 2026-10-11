package com.yourstudio.aerolauncher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;

/**
 * Vertical A–Z fast scroller for the App Library alphabetical section.
 */
public class FastScrollerView extends View {

    public interface Listener {
        void onLetterSelected(@NonNull String letter);
    }

    private static final String[] LETTERS = {
            "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Nullable
    private Listener listener;

    public FastScrollerView(Context context) {
        super(context);
        init();
    }

    public FastScrollerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FastScrollerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.home_label));
        paint.setTextSize(getResources().getDisplayMetrics().scaledDensity * 10f);
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (height <= 0) {
            return;
        }
        float slot = height / LETTERS.length;
        float cx = getWidth() / 2f;
        for (int i = 0; i < LETTERS.length; i++) {
            float cy = getPaddingTop() + slot * i + slot * 0.7f;
            canvas.drawText(LETTERS[i], cx, cy, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN
                || action == MotionEvent.ACTION_MOVE
                || action == MotionEvent.ACTION_UP) {
            float height = getHeight() - getPaddingTop() - getPaddingBottom();
            if (height > 0 && listener != null) {
                float y = event.getY() - getPaddingTop();
                int index = (int) (y / (height / LETTERS.length));
                index = Math.max(0, Math.min(LETTERS.length - 1, index));
                listener.onLetterSelected(LETTERS[index]);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Scrolls {@code list} so the first adapter position mapped for {@code letter} is visible.
     */
    public static void scrollToLetter(
            @NonNull RecyclerView list,
            @NonNull String letter,
            @NonNull LetterIndex index) {
        Integer position = index.positionFor(letter);
        if (position != null) {
            list.scrollToPosition(position);
        }
    }

    public interface LetterIndex {
        @Nullable
        Integer positionFor(@NonNull String letter);
    }
}
