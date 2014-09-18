package it.sephiroth.android.library.imagezoom.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.graphics.FastBitmapDrawable;
import it.sephiroth.android.library.imagezoom.test.utils.DecodeUtils;

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
	private boolean mOverlayChanged;

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
		mOverlayChanged = true;
		super.setImageBitmap(bitmap, null, - 1, - 1);
	}

	public void setImageDrawable(final Drawable drawable, final Bitmap overlay) {
		if (null != overlay) {
			mOverlayTempDrawable = new FastBitmapDrawable(overlay);
		}
		else {
			mOverlayTempDrawable = null;
		}
		mOverlayChanged = true;
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

	@SuppressWarnings ("unused")
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
		Log.i(TAG, "onLayout(" + left + ", " + top + ", " + right + ", " + bottom + ")");

		mTempViewPort.set(left, top, right, bottom);

		if (mOverlayChanged) {
			mOverlayDrawable = mOverlayTempDrawable;
			mOverlayTempDrawable = null;

			if (null != mOverlayDrawable) {
				mOverlayDrawableWidth = mOverlayDrawable.getIntrinsicWidth();
				mOverlayDrawableHeight = mOverlayDrawable.getIntrinsicHeight();
			}
			else {
				mOverlayDrawableWidth = 0;
				mOverlayDrawableHeight = 0;
			}
		}

		if (changed || mBitmapChanged) {
			Drawable drawable = getDrawable();
			if (null != drawable && null != mOverlayDrawable) {

				Log.v(TAG, "bitmap size: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());

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

		if (null != mOverlayDrawable) {
			if (changed || mBitmapChanged) {
				mBaseMatrix2.reset();
				mSuppMatrix2.reset();

				getProperBaseMatrix2(mOverlayDrawable, mBaseMatrix2, mTempViewPort);
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

	@SuppressLint ("WrongCall")
	public Bitmap generateResultBitmap() {
		if (null == mOverlayDrawable) return null;

		Drawable drawable = getDrawable();
		if (null == drawable) return null;

		RectF rect = getOverlayBitmapRect();

		PointF previewSize = new PointF(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		RectF bitmapRect = new RectF(getBitmapRect());
		RectF overlayRect = new RectF(getOverlayBitmapRect());
		PointF overlaySize = new PointF(mOverlayDrawableWidth, mOverlayDrawableHeight);

		Log.i(TAG, "-----------------------");
		Log.i(TAG, "generateResultBitmap");
		Log.i(TAG, "-----------------------");

		Log.v(TAG, "overlay rect: " + getOverlayBitmapRect() + ", size: " + getOverlayBitmapRect().width() + "x" + getOverlayBitmapRect().height());
		Log.v(TAG, "bitmap rect: " + getBitmapRect() + ", size: " + getBitmapRect().width() + "x" + getBitmapRect().height());
		Log.v(TAG, "overlay real size: " + mOverlayDrawableWidth + "x" + mOverlayDrawableHeight);
		Log.v(TAG, "bitmap real size: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());
		Log.v(TAG, "current scale: " + getScale());

		printMatrix(getImageMatrix());
		printMatrix(getImageViewMatrix());

		//Bitmap bitmap = ((FastBitmapDrawable) drawable).getBitmap();
		Bitmap overlay = null;
		InputStream stream = null;
		Bitmap bitmap = DecodeUtils.decode(getContext(), Uri.parse("content://media/external/images/media/5063"), 5000, 5000);

		try {
			stream = getContext().getAssets().open("images/circle-black-large.png");
			overlay = BitmapFactory.decodeStream(stream);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		double overlaySizeRatio = overlayRect.width() / overlayRect.height();

		float bitmapScaleX = (float)bitmap.getWidth()/previewSize.x;
		float bitmapScaleY = (float)bitmap.getHeight()/previewSize.y;

		Log.d(TAG, String.format("bitmapScale: %fx%f", bitmapScaleX, bitmapScaleY));

		float scaleX = bitmapRect.width() / previewSize.x;
		float scaleY = bitmapRect.height() / previewSize.y;

		Log.d(TAG, "bitmap size: " + width + "x" + height);
		Log.d(TAG, "scaleX: " + scaleX + ", scaleY: " + scaleY);
		Log.d(TAG, "overlaySizeRatio: " + overlaySizeRatio);

		RectF sk_overlayRect = new RectF(overlayRect.left, overlayRect.top, overlayRect.right, overlayRect.bottom);
		RectF sk_bitmapRect = new RectF(bitmapRect.left, bitmapRect.top, bitmapRect.right, bitmapRect.bottom);

		float dx = sk_overlayRect.left;
		float dy = sk_overlayRect.top;

		sk_overlayRect.offset(- dx, - dy);
		sk_bitmapRect.offset(- dx, - dy);

		Log.d(TAG, String.format("sk_overlayRect(%s)", sk_overlayRect));
		Log.d(TAG, String.format("sk_bitmapRect(%s)", sk_bitmapRect));

		int targetHeight;
		int targetWidth;
		float scale;

		if(overlaySizeRatio >= 1){
			targetHeight = bitmap.getHeight();
			targetWidth = (int) (overlaySizeRatio * targetHeight);
			scale = bitmapRect.height()/overlayRect.height();
		} else {
			targetWidth = bitmap.getWidth();
			targetHeight = (int) (overlaySizeRatio * targetWidth);
			scale = bitmapRect.width()/overlayRect.width();
		}

		Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

		Log.d(TAG, "result size: " + result.getWidth() + "x" + result.getHeight());
		Log.d(TAG, "scale: " + scale);

		Canvas newCanvas = new Canvas(result);
		newCanvas.drawColor(Color.RED);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);

		int count = newCanvas.save();
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		Log.v(TAG, "translate: " + (sk_bitmapRect.left/scaleX*scale*bitmapScaleX) + ", " + (sk_bitmapRect.top/scaleY*scale*bitmapScaleY));
		matrix.postTranslate(sk_bitmapRect.left/scaleX*scale*bitmapScaleX, sk_bitmapRect.top/scaleY*scale*bitmapScaleY);

		//newCanvas.concat(matrix);
		newCanvas.scale(scale, scale);
		newCanvas.drawBitmap(bitmap, 0, 0, paint);
		newCanvas.restoreToCount(count);
		newCanvas.drawBitmap(overlay, null, new Rect(0, 0, result.getWidth(), result.getHeight()), paint);

		int max_size = 2048;
		int w = result.getWidth();
		int h = result.getHeight();
		if(result.getWidth() > max_size || result.getHeight() > max_size) {
			if(result.getWidth() >= result.getHeight()) {
				w = 2048;
				h = (int) (2048/((float)result.getWidth()/result.getHeight()));
			} else {
				h = 2048;
				w = (int) (2048/((float)result.getHeight()/result.getWidth()));
			}
		}
		return Bitmap.createScaledBitmap(result, w, h, true);
	}
}
