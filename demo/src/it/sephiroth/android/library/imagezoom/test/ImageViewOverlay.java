package it.sephiroth.android.library.imagezoom.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.graphics.FastBitmapDrawable;

public class ImageViewOverlay extends ImageViewTouch {

	protected Drawable mOverlayDrawable;
	protected Drawable mOverlayTempDrawable;

	protected Matrix mDrawMatrix2;
	protected Matrix mBaseMatrix2 = new Matrix();
	protected Matrix mSuppMatrix2 = new Matrix();
	protected Matrix mDisplayMatrix2 = new Matrix();
	protected Matrix mMatrix2 = new Matrix();
	protected RectF mOverlayBitmapRect = new RectF();
	private RectF mTempViewPort = new RectF();
	private int mOverlayDrawableWidth, mOverlayDrawableHeight;

	private static final int MAX_VIEWPORT_SIZE = 2048;

	public ImageViewOverlay(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public ImageViewOverlay(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void init(final Context context, final AttributeSet attrs, final int defStyle) {
		super.init(context, attrs, defStyle);
	}

	@Override
	public DisplayType getDisplayType() {
		// force fit to screen
		return DisplayType.FIT_TO_SCREEN;
	}

	public Matrix getImageViewMatrix2() {
		return getImageViewMatrix2(mSuppMatrix2);
	}

	public Matrix getImageViewMatrix2(Matrix supportMatrix) {
		mDisplayMatrix2.set(mBaseMatrix2);
		mDisplayMatrix2.postConcat(supportMatrix);
		return mDisplayMatrix2;
	}

	public void setImageMatrix2(Matrix matrix) {
		if (matrix != null && matrix.isIdentity()) {
			matrix = null;
		}

		// don't invalidate unless we're actually changing our matrix
		if (matrix == null && ! mMatrix2.isIdentity() || matrix != null && ! mMatrix2.equals(matrix)) {
			mMatrix2.set(matrix);
			configureBounds2();
			invalidate();
		}
	}

	public RectF getOverlayBitmapRect() {
		return getOverlayBitmapRect(mSuppMatrix2);
	}

	protected RectF getOverlayBitmapRect(Matrix supportMatrix) {
		final Drawable drawable = mOverlayDrawable;

		if (drawable == null) return null;
		Matrix m = getImageViewMatrix2(supportMatrix);
		mOverlayBitmapRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		m.mapRect(mOverlayBitmapRect);
		return mOverlayBitmapRect;
	}

	@Override
	protected void getProperBaseMatrix(Drawable drawable, Matrix matrix, RectF rect) {
		if (null == mOverlayDrawable) {
			super.getProperBaseMatrix(drawable, matrix, rect);
			return;
		}

		float w = drawable.getIntrinsicWidth();
		float h = drawable.getIntrinsicHeight();
		float widthScale, heightScale;

		matrix.reset();

		widthScale = rect.width() / w;
		heightScale = rect.height() / h;
		float scale = Math.max(widthScale, heightScale);
		matrix.postScale(scale, scale);
		matrix.postTranslate(rect.left, rect.top);

		float tw = (rect.width() - w * scale) / 2.0f;
		float th = (rect.height() - h * scale) / 2.0f;
		matrix.postTranslate(tw, th);
		printMatrix(matrix);
	}

	protected void getProperBaseMatrix2(Drawable drawable, Matrix matrix, RectF rect) {
		float w = drawable.getIntrinsicWidth();
		float h = drawable.getIntrinsicHeight();
		float widthScale, heightScale;

		matrix.reset();

		widthScale = rect.width() / w;
		heightScale = rect.height() / h;
		float scale = Math.min(widthScale, heightScale);
		matrix.postScale(scale, scale);
		matrix.postTranslate(rect.left, rect.top);

		float tw = (rect.width() - w * scale) / 2.0f;
		float th = (rect.height() - h * scale) / 2.0f;
		matrix.postTranslate(tw, th);
		printMatrix(matrix);
	}

	private void configureBounds2() {
		if (mOverlayDrawable == null) {
			return;
		}

		int dwidth = mOverlayDrawableWidth;
		int dheight = mOverlayDrawableHeight;

		int vwidth = getWidth();
		int vheight = getHeight();

		if (dwidth <= 0 || dheight <= 0) {
			mOverlayDrawable.setBounds(0, 0, vwidth, vheight);
			mDrawMatrix2 = null;
		}
		else {
			mOverlayDrawable.setBounds(0, 0, dwidth, dheight);

			if (mMatrix2.isIdentity()) {
				mDrawMatrix2 = null;
			}
			else {
				mDrawMatrix2 = mMatrix2;
			}
		}
	}

	@Override
	protected float computeMinZoom() {
		if (null == mOverlayDrawable) return super.computeMinZoom();
		return 1;
	}

	public void setImageBitmap(final Bitmap bitmap, final Bitmap overlay) {
		if (null != overlay) {
			mOverlayTempDrawable = new FastBitmapDrawable(overlay);
		}
		else {
			mOverlayTempDrawable = null;
		}
		super.setImageBitmap(bitmap, null, - 1, - 1);
	}

	public void setImageDrawable(final Drawable drawable, final Bitmap overlay) {
		if (null != overlay) {
			mOverlayTempDrawable = new FastBitmapDrawable(overlay);
		}
		else {
			mOverlayTempDrawable = null;
		}
		super.setImageDrawable(drawable, null, - 1, - 1);
	}

	public void updateImageOverlay(final Bitmap overlay) {
		if (mOverlayDrawable == null || null == overlay) return;

		if (mOverlayDrawable.getIntrinsicWidth() == overlay.getWidth() && mOverlayDrawable.getIntrinsicHeight() == overlay.getHeight()) {
			mOverlayDrawable = new FastBitmapDrawable(overlay);
			invalidate();
		}
		else {
			setImageDrawable(getDrawable(), overlay);
		}
	}

	@Override
	public void requestLayout() {
		super.requestLayout();
	}

	public Drawable getOverlayDrawable() {
		return mOverlayDrawable;
	}

	@Override
	protected void onViewPortChanged(final float left, final float top, final float right, final float bottom) {
		if (null == mOverlayDrawable) {
			super.onViewPortChanged(left, top, right, bottom);
		}
		else {
			super.onViewPortChanged(mTempViewPort.left, mTempViewPort.top, mTempViewPort.right, mTempViewPort.bottom);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		mTempViewPort.set(left, top, right, bottom);

		if (null != mOverlayTempDrawable) {
			mOverlayDrawable = mOverlayTempDrawable;
			mOverlayTempDrawable = null;
			mOverlayDrawableWidth = mOverlayDrawable.getIntrinsicWidth();
			mOverlayDrawableHeight = mOverlayDrawable.getIntrinsicHeight();
		}

		final Drawable overlay = mOverlayDrawable;

		if (changed || mBitmapChanged) {
			Drawable drawable = getDrawable();
			if (null != drawable && null != mOverlayDrawable) {
				int dwidth = Math.min(right - left, Math.min(drawable.getIntrinsicWidth(), MAX_VIEWPORT_SIZE));
				int dheight = Math.min(bottom - top, Math.min(drawable.getIntrinsicHeight(), MAX_VIEWPORT_SIZE));

				if (mTempViewPort.width() > dwidth || mTempViewPort.height() > dheight) {
					float widthScale, heightScale;

					Matrix matrix = new Matrix();
					widthScale = dwidth / mTempViewPort.width();
					heightScale = dheight / mTempViewPort.height();
					float scale = Math.max(widthScale, heightScale);
					matrix.postScale(scale, scale, mTempViewPort.centerX(), mTempViewPort.centerY());
					matrix.mapRect(mTempViewPort);
				}
			}
			changed = true;
		}

		if (null != overlay) {
			if (changed || mBitmapChanged) {
				mBaseMatrix2.reset();
				mSuppMatrix2.reset();

				getProperBaseMatrix2(overlay, mBaseMatrix2, mTempViewPort);
				setImageMatrix2(getImageViewMatrix2());

				mTempViewPort.set(getOverlayBitmapRect());
			}
		}

		super.onLayout(changed, left, top, right, bottom);
	}


	@Override
	protected void onDraw(final Canvas canvas) {

		Matrix drawMatrix = getImageMatrix();
		Drawable drawable = getDrawable();
		int saveCount;

		if (null == drawable) return;

		saveCount = canvas.getSaveCount();
		canvas.save();

		canvas.clipRect(mViewPort);

		if (drawMatrix != null) {
			canvas.concat(drawMatrix);
		}
		drawable.draw(canvas);
		canvas.restoreToCount(saveCount);

		if (mOverlayDrawable == null) return;
		if (mOverlayDrawableWidth == 0 || mOverlayDrawableHeight == 0) return;

		if (mDrawMatrix2 != null) {
			saveCount = canvas.getSaveCount();
			canvas.save();
			canvas.concat(mDrawMatrix2);
			mOverlayDrawable.draw(canvas);
			canvas.restoreToCount(saveCount);
		}
	}
}
