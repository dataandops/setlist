package com.setlist;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import java.util.function.BooleanSupplier;

/** One deliberate horizontal gesture turns one page, on finger release. */
final class PdfSwipeView extends FrameLayout {
    private final Runnable forward, backward;
    private final BooleanSupplier ready;
    private final float minimumDistance;
    private float startX, startY;
    private boolean tracking;

    PdfSwipeView(Context context, BooleanSupplier ready, Runnable forward, Runnable backward) {
        super(context);
        this.ready = ready; this.forward = forward; this.backward = backward;
        minimumDistance = Math.max(Ui.dp(context, 48), ViewConfiguration.get(context).getScaledTouchSlop() * 3);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                startX = event.getX(); startY = event.getY(); tracking = ready.getAsBoolean();
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> tracking = false;
            case MotionEvent.ACTION_UP -> {
                float dx = event.getX() - startX, dy = event.getY() - startY;
                boolean turn = tracking && ready.getAsBoolean()
                    && Math.abs(dx) >= minimumDistance && Math.abs(dx) > Math.abs(dy) * 1.5f;
                tracking = false;
                if (turn) { if (dx < 0) forward.run(); else backward.run(); }
                else performClick();
            }
            default -> { }
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
