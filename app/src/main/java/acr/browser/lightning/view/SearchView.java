package acr.browser.lightning.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class SearchView extends AppCompatAutoCompleteTextView {

    public interface PreFocusListener {
        void onPreFocus();
    }

    @Nullable private PreFocusListener mListener;
    private boolean mIsBeingClicked;
    private long mTimePressed;

    public SearchView(@NonNull Context context) {
        super(context);
    }

    public SearchView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnPreFocusListener(@Nullable PreFocusListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTimePressed = System.currentTimeMillis();
                mIsBeingClicked = true;
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsBeingClicked = false;
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingClicked && !isLongPress()) {
                    if (mListener != null) {
                        mListener.onPreFocus();
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean isLongPress() {
        return (System.currentTimeMillis() - mTimePressed) >= ViewConfiguration.getLongPressTimeout();
    }


}
