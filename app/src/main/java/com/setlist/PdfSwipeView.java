package com.setlist;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * One deliberate horizontal gesture turns one page, on finger release. A single tap toggles the
 * stage chrome. Pinch or double-tap zooms the score; while zoomed, one finger pans instead of turning.
 */
final class PdfSwipeView extends FrameLayout {
    interface Listener {
        boolean ready();
        void forward();
        void backward();
        void tapped();
        /** Zoom or pan changed; settled is true once fingers lift. */
        void zoomed(boolean settled);
    }
    static final float MAX_ZOOM = 4f, DOUBLE_TAP_ZOOM = 2.5f;
    private final Listener listener;
    private final float minimumDistance;
    private final ScaleGestureDetector scaler;
    private final GestureDetector taps;
    private View target;
    private float startX, startY, lastX, lastY, zoom = 1, panX, panY;
    private boolean tracking, scaled;

    PdfSwipeView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        minimumDistance = Math.max(Ui.dp(context, 48), ViewConfiguration.get(context).getScaledTouchSlop() * 3);
        scaler = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                scaled = true; zoomAround(zoom * detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY()); return true;
            }
        });
        taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapConfirmed(MotionEvent event) { listener.tapped(); return true; }
            @Override public boolean onDoubleTap(MotionEvent event) {
                zoomAround(zoom > 1 ? 1 : DOUBLE_TAP_ZOOM, event.getX(), event.getY()); listener.zoomed(true); return true;
            }
        });
        // A slightly long, still press is still a tap; nothing here uses long-press.
        taps.setIsLongpressEnabled(false);
    }

    /** The view that zooms and pans; its scale and translation belong to this gesture layer. */
    void setTarget(View target) { this.target = target; apply(); }
    float zoom() { return zoom; }
    float panX() { return panX; }
    float panY() { return panY; }
    boolean zoomed() { return zoom > 1.01f; }
    void resetZoom() { zoom = 1; panX = 0; panY = 0; apply(); }

    private void zoomAround(float next, float focusX, float focusY) {
        next = Math.max(1, Math.min(MAX_ZOOM, next));
        // Keep the content under the focus point still while the scale changes.
        float centerX = getWidth() / 2f, centerY = getHeight() / 2f;
        float contentX = (focusX - centerX - panX) / zoom, contentY = (focusY - centerY - panY) / zoom;
        zoom = next; panX = focusX - centerX - contentX * zoom; panY = focusY - centerY - contentY * zoom;
        apply();
    }
    private void apply() {
        float limitX = getWidth() * (zoom - 1) / 2f, limitY = getHeight() * (zoom - 1) / 2f;
        panX = Math.max(-limitX, Math.min(limitX, panX)); panY = Math.max(-limitY, Math.min(limitY, panY));
        if (target == null) return;
        target.setPivotX(getWidth() / 2f); target.setPivotY(getHeight() / 2f);
        target.setScaleX(zoom); target.setScaleY(zoom); target.setTranslationX(panX); target.setTranslationY(panY);
    }
    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight); apply();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        scaler.onTouchEvent(event); taps.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                startX = lastX = event.getX(); startY = lastY = event.getY(); scaled = false; tracking = listener.ready();
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> tracking = false;
            case MotionEvent.ACTION_MOVE -> {
                if (zoomed() && !scaler.isInProgress() && event.getPointerCount() == 1) {
                    panX += event.getX() - lastX; panY += event.getY() - lastY; apply(); listener.zoomed(false);
                } else if (scaled) listener.zoomed(false);
                lastX = event.getX(); lastY = event.getY();
            }
            case MotionEvent.ACTION_POINTER_UP -> {
                // The remaining finger continues the pan from where it is, not from the lifted one.
                int remaining = event.getActionIndex() == 0 ? 1 : 0;
                lastX = event.getX(remaining); lastY = event.getY(remaining);
            }
            case MotionEvent.ACTION_UP -> {
                float dx = event.getX() - startX, dy = event.getY() - startY;
                boolean turn = tracking && !scaled && !zoomed() && listener.ready()
                    && Math.abs(dx) >= minimumDistance && Math.abs(dx) > Math.abs(dy) * 1.5f;
                tracking = false;
                if (turn) { if (dx < 0) listener.forward(); else listener.backward(); }
                else { if (scaled || zoomed()) listener.zoomed(true); performClick(); }
            }
            default -> { }
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
