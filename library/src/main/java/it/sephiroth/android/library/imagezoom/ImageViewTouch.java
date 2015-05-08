package it.sephiroth.android.library.imagezoom;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ViewConfiguration;

public class ImageViewTouch extends ImageViewTouchBase {

	static final float SCROLL_DELTA_THRESHOLD = 1.0f;

	/** minimum time between a scale event and a valid fling event */
	public static long MIN_FLING_DELTA_TIME = 150;

	protected ScaleGestureDetector mScaleDetector;
	protected GestureDetector mGestureDetector;
	protected int mTouchSlop;
	protected float mScaleFactor;
	protected int mDoubleTapDirection;
	protected OnGestureListener mGestureListener;
	protected OnScaleGestureListener mScaleListener;
	protected boolean mDoubleTapEnabled = true;
	protected boolean mScaleEnabled = true;
	protected boolean mScrollEnabled = true;
	private OnImageViewTouchDoubleTapListener mDoubleTapListener;
	private OnImageViewTouchSingleTapListener mSingleTapListener;

	public ImageViewTouch(Context context) {
		super(context);
	}

	public ImageViewTouch(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ImageViewTouch(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void init(Context context, AttributeSet attrs, int defStyle) {
		super.init(context, attrs, defStyle);
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		mGestureListener = getGestureListener();
		mScaleListener = getScaleListener();

		mScaleDetector = new ScaleGestureDetector(getContext(), mScaleListener);
		mGestureDetector = new GestureDetector(getContext(), mGestureListener, null, true);

		mDoubleTapDirection = 1;
	}

	public void setDoubleTapListener(OnImageViewTouchDoubleTapListener listener) {
		mDoubleTapListener = listener;
	}

	public void setSingleTapListener(OnImageViewTouchSingleTapListener listener) {
		mSingleTapListener = listener;
	}

	public void setDoubleTapEnabled(boolean value) {
		mDoubleTapEnabled = value;
	}

	public void setScaleEnabled(boolean value) {
		mScaleEnabled = value;
	}

	public void setScrollEnabled(boolean value) {
		mScrollEnabled = value;
	}

	public boolean getDoubleTapEnabled() {
		return mDoubleTapEnabled;
	}

	protected OnGestureListener getGestureListener() {
		return new GestureListener();
	}

	protected OnScaleGestureListener getScaleListener() {
		return new ScaleListener();
	}

	@Override
	protected void _setImageDrawable(final Drawable drawable, final Matrix initial_matrix, float min_zoom, float max_zoom) {
		super._setImageDrawable(drawable, initial_matrix, min_zoom, max_zoom);
		mScaleFactor = getMaxScale() / 3;
	}

	long mPointerUpTime;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (getBitmapChanged()) return false;

		final int action = event.getActionMasked();

		if (action == MotionEvent.ACTION_POINTER_UP) {
			mPointerUpTime = event.getEventTime();
		}

		mScaleDetector.onTouchEvent(event);

		if (! mScaleDetector.isInProgress()) {
			mGestureDetector.onTouchEvent(event);
		}

		switch (action) {
			case MotionEvent.ACTION_UP:
				return onUp(event);
		}
		return true;
	}


	@Override
	protected void onZoomAnimationCompleted(float scale) {

		if (DEBUG) {
			Log.d(TAG, "onZoomAnimationCompleted. scale: " + scale + ", minZoom: " + getMinScale());
		}

		if (scale < getMinScale()) {
			zoomTo(getMinScale(), 50);
		}
	}

	protected float onDoubleTapPost(float scale, final float maxZoom, final float minScale) {
		if (mDoubleTapDirection == 1) {
			if ((scale + (mScaleFactor * 2)) <= maxZoom) {
				return scale + mScaleFactor;
			}
			else {
				mDoubleTapDirection = - 1;
				return maxZoom;
			}
		}
		else {
			mDoubleTapDirection = 1;
			return minScale;
		}
	}

	public boolean onSingleTapConfirmed(MotionEvent e) {
		return true;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (getScale() <= 1f) return false;
		mUserScaled = true;
		scrollBy(- distanceX, - distanceY);
		invalidate();
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if(getScale() <= 1) return false;

		if(DEBUG) Log.i(TAG, "onFling");

		if (Math.abs(velocityX) > (mMinFlingVelocity * 4) || Math.abs(velocityY) > (mMinFlingVelocity * 4)) {
			if (DEBUG) {
				Log.v(TAG, "velocity: " + velocityY);
				Log.v(TAG, "diff: " + (e2.getY() - e1.getY()));
			}

			final float scale = Math.min(Math.max(2f, getScale() / 2), 3.f);

			float scaledDistanceX = ((velocityX) / mMaxFlingVelocity) * (getWidth() * scale);
			float scaledDistanceY = ((velocityY) / mMaxFlingVelocity) * (getHeight() * scale);

			if (DEBUG) {
				Log.v(TAG, "scale: " + getScale() + ", scale_final: " + scale);
				Log.v(TAG, "scaledDistanceX: " + scaledDistanceX);
				Log.v(TAG, "scaledDistanceY: " + scaledDistanceY);
			}

			mUserScaled = true;

			double total = Math.sqrt(Math.pow(scaledDistanceX, 2) + Math.pow(scaledDistanceY, 2));

			scrollBy(scaledDistanceX, scaledDistanceY, (long) Math.min(Math.max(300, total / 5), 800));

			postInvalidate();
			return true;
		}
		return false;
	}

	public boolean onDown(MotionEvent e) {
		if (getBitmapChanged()) return false;
		return true;
	}

	public boolean onUp(MotionEvent e) {
		if (getBitmapChanged()) return false;
		if (getScale() < getMinScale()) {
			zoomTo(getMinScale(), 50);
		}
		return true;
	}

	public boolean onSingleTapUp(MotionEvent e) {
		if (getBitmapChanged()) return false;
		return true;
	}

	/**
	 * Determines whether this ImageViewTouch can be scrolled.
	 *
	 * @param direction - positive direction value means scroll from right to left,
	 *                  negative value means scroll from left to right
	 * @return true if there is some more place to scroll, false - otherwise.
	 */
	public boolean canScroll(int direction) {
		RectF bitmapRect = getBitmapRect();
		updateRect(bitmapRect, mScrollRect);
		Rect imageViewRect = new Rect();
		getGlobalVisibleRect(imageViewRect);

		if (null == bitmapRect) {
			return false;
		}

		if (bitmapRect.right >= imageViewRect.right) {
			if (direction < 0) {
				return Math.abs(bitmapRect.right - imageViewRect.right) > SCROLL_DELTA_THRESHOLD;
			}
		}

		double bitmapScrollRectDelta = Math.abs(bitmapRect.left - mScrollRect.left);
		return bitmapScrollRectDelta > SCROLL_DELTA_THRESHOLD;
	}

	public class GestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {

			if (null != mSingleTapListener) {
				mSingleTapListener.onSingleTapConfirmed();
			}

			return ImageViewTouch.this.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (DEBUG) {
				Log.i(TAG, "onDoubleTap. double tap enabled? " + mDoubleTapEnabled);
			}
			if (mDoubleTapEnabled) {
				mUserScaled = true;
				float scale = getScale();
				float targetScale;
				targetScale = onDoubleTapPost(scale, getMaxScale(), getMinScale());
				targetScale = Math.min(getMaxScale(), Math.max(targetScale, getMinScale()));
				zoomTo(targetScale, e.getX(), e.getY(), mDefaultAnimationDuration);
			}

			if (null != mDoubleTapListener) {
				mDoubleTapListener.onDoubleTap();
			}

			return super.onDoubleTap(e);
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if (isLongClickable()) {
				if (! mScaleDetector.isInProgress()) {
					setPressed(true);
					performLongClick();
				}
			}
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (! mScrollEnabled) return false;
			if (e1 == null || e2 == null) return false;
			if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
			if (mScaleDetector.isInProgress()) return false;
			return ImageViewTouch.this.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (! mScrollEnabled) return false;
			if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
			if (mScaleDetector.isInProgress()) return false;

			final long delta = (SystemClock.uptimeMillis() - mPointerUpTime);

			// prevent fling happening just
			// after a quick pinch to zoom
			if(delta > MIN_FLING_DELTA_TIME) {
				return ImageViewTouch.this.onFling(e1, e2, velocityX, velocityY);
			} else {
				return false;
			}
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return ImageViewTouch.this.onSingleTapUp(e);
		}

		@Override
		public boolean onDown(MotionEvent e) {
			if (DEBUG) {
				Log.i(TAG, "onDown");
			}
			stopAllAnimations();


			return ImageViewTouch.this.onDown(e);
		}
	}


	public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		protected boolean mScaled = false;

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float span = detector.getCurrentSpan() - detector.getPreviousSpan();
			float targetScale = getScale() * detector.getScaleFactor();

			if (mScaleEnabled) {
				if (mScaled && span != 0) {
					mUserScaled = true;
					targetScale = Math.min(getMaxScale(), Math.max(targetScale, getMinScale() - 0.1f));
					zoomTo(targetScale, detector.getFocusX(), detector.getFocusY());
					mDoubleTapDirection = 1;
					invalidate();
					return true;
				}

				// This is to prevent a glitch the first time 
				// image is scaled.
				if (! mScaled) mScaled = true;
			}
			return true;
		}

	}

	public interface OnImageViewTouchDoubleTapListener {

		void onDoubleTap();
	}

	public interface OnImageViewTouchSingleTapListener {

		void onSingleTapConfirmed();
	}
}
