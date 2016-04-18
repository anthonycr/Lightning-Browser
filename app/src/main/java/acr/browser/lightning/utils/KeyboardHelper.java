package acr.browser.lightning.utils;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;

public class KeyboardHelper {

    public interface KeyboardListener {
        /**
         * Called when the visibility of the keyboard changes.
         * Parameter tells whether the keyboard has been shown
         * or hidden.
         *
         * @param visible true if the keyboard has been shown,
         *                false otherwise.
         */
        void keyboardVisibilityChanged(boolean visible);
    }

    @NonNull private final View mView;
    private int mLastRight = -1;
    private int mLastBottom = -1;

    /**
     * Constructor
     *
     * @param view the view to listen on, should be
     *             the {@link android.R.id#content} view.
     */
    public KeyboardHelper(@NonNull View view) {
        mView = view;
    }

    /**
     * Registers a {@link KeyboardListener} to receive
     * callbacks when the keyboard is shown for the specific
     * view. The view used should be the content view as it
     * will receive resize events from the system.
     *
     * @param listener the listener to register to receive events.
     */
    public void registerKeyboardListener(@NonNull final KeyboardListener listener) {
        mView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                if (mLastBottom == -1) {
                    mLastBottom = rect.bottom;
                }
                if (mLastRight == -1) {
                    mLastRight = rect.right;
                }
                mView.getWindowVisibleDisplayFrame(rect);
                if (mLastRight == rect.right && rect.bottom < mLastBottom) {
                    listener.keyboardVisibilityChanged(true);
                } else if (mLastRight == rect.right && rect.bottom > mLastBottom) {
                    listener.keyboardVisibilityChanged(false);
                }
                mLastBottom = rect.bottom;
                mLastRight = rect.right;
            }
        });
    }

}
