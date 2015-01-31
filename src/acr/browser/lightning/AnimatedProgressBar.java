package acr.browser.lightning;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.LinearLayout;

/**
 * Copyright 11/4/2014 Anthony Restaino
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class AnimatedProgressBar extends LinearLayout {

    private View mProgressView;
    private int mProgress = 0;
    private boolean mBidirectionalAnimate = true;
    private Animation mAnimation;
    int mInitialWidth;
    int mDeltaWidth;
    int mMaxWidth;

    public AnimatedProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AnimatedProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Initialize the AnimatedProgressBar
     *
     * @param context is the context passed by the constructor
     * @param attrs   is the attribute set passed by the constructor
     */
    private void init(final Context context, AttributeSet attrs) {
    	this.setLayerType(LAYER_TYPE_HARDWARE, null);
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AnimatedProgressBar, 0, 0);
        int backgroundColor;
        int progressColor;
        try {   // Retrieve the style of the progress bar that the user hopefully set

            int DEFAULT_BACKGROUND_COLOR = 0x00000000;
            int DEFAULT_PROGRESS_COLOR = 0x2196f3;

            backgroundColor = array.getColor(R.styleable.AnimatedProgressBar_backgroundColor, DEFAULT_BACKGROUND_COLOR);
            progressColor = array.getColor(R.styleable.AnimatedProgressBar_progressColor, DEFAULT_PROGRESS_COLOR);
            mBidirectionalAnimate = array.getBoolean(R.styleable.AnimatedProgressBar_bidirectionalAnimate, false);

        } finally {

            array.recycle();

        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.animated_progress_bar, this, true);

        mProgressView = findViewById(android.R.id.progress);

        this.setBackgroundColor(backgroundColor);           // set the background color for this view
        mProgressView.setBackgroundColor(progressColor);    // set the color of the progress bar

        mAnimation = new Animation() {
            int width;

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                width = mInitialWidth + (int) (mDeltaWidth * interpolatedTime);
                if (width <= mMaxWidth) {
                    mProgressView.getLayoutParams().width = width;
                    mProgressView.requestLayout();
                }
                mProgressView.invalidate();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        mAnimation.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (mProgress >= 100) {
                    fadeOut();
                }
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
        	
        });
        
        mAnimation.setDuration(300);
        mAnimation.setInterpolator(new DecelerateInterpolator());
    }

    /**
     * Returns the current progress value between 0 and 100
     *
     * @return progress of the view
     */
    public int getProgress() {
        return mProgress;
    }


    /**
     * sets the progress as an integer value between 0 and 100.
     * Values above or below that interval will be adjusted to their
     * nearest value within the interval, i.e. setting a value of 150 will have
     * the effect of setting the progress to 100. You cannot trick us.
     *
     * @param progress an integer between 0 and 100
     */
    public void setProgress(int progress) {

        if (progress > 100) {       // progress cannot be greater than 100
            progress = 100;
        } else if (progress < 0) {  // progress cannot be less than 0
            progress = 0;
        }

        if (mProgressView.getAlpha() < 1.0f && progress < 100) {
            fadeIn();
        }

        final int maxWidth = this.getMeasuredWidth();           // get the maximum width the view can be
        int initialWidth = mProgressView.getMeasuredWidth();    // get the initial width of the view

        if (progress < mProgress && !mBidirectionalAnimate) {   // if the we only animate the view in one direction
            // then reset the view width if it is less than the
            // previous progress
            mProgressView.getLayoutParams().width = 0;
            mProgressView.requestLayout();
            initialWidth = 0;

        } else if (progress == mProgress) {     // we don't need to go any farther if the progress is unchanged
            return;
        }

        mProgress = progress;       // save the progress

        final int deltaWidth = (maxWidth * mProgress / 100) - initialWidth;     // calculate amount the width has to change

        animateView(initialWidth, maxWidth, deltaWidth);    // animate the width change
    }

    /**
     * private method used to create and run the animation used to change the progress
     *
     * @param initialWidth is the width at which the progress starts at
     * @param maxWidth     is the maximum width (total width of the view)
     * @param deltaWidth   is the amount by which the width of the progress view will change
     */
    private void animateView(final int initialWidth, final int maxWidth, final int deltaWidth) {
    	mInitialWidth = initialWidth;
    	mMaxWidth = maxWidth;
    	mDeltaWidth = deltaWidth;
    	mAnimation.reset();
        mProgressView.startAnimation(mAnimation);
    }

    /**
     * fades in the progress bar
     */
    private void fadeIn() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mProgressView, "alpha", 1.0f);
        fadeIn.setDuration(200);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.start();
    }

    /**
     * fades out the progress bar
     */
    private void fadeOut() {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mProgressView, "alpha", 0.0f);
        fadeOut.setDuration(200);
        fadeOut.setInterpolator(new DecelerateInterpolator());
        fadeOut.start();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {

        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            this.mProgress = bundle.getInt("progressState");
            state = bundle.getParcelable("instanceState");


        }

        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {

        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putInt("progressState", mProgress);
        return bundle;
    }

}
