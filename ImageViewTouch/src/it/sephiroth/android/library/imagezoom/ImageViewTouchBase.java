package it.sephiroth.android.library.imagezoom;

import it.sephiroth.android.library.imagezoom.easing.Cubic;
import it.sephiroth.android.library.imagezoom.easing.Easing;
import it.sephiroth.android.library.imagezoom.graphics.FastBitmapDrawable;
import it.sephiroth.android.library.imagezoom.utils.IDisposable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * Base View to manage image zoom/scrool/pinch operations
 * 
 * @author alessandro
 * 
 */
public class ImageViewTouchBase extends ImageView implements IDisposable {

	public interface OnBitmapChangedListener {
		void onBitmapChanged( Drawable drawable );
	};
	
	public enum ScaleType {
		Normal, FitToScreen,
	};

	public static final String LOG_TAG = "image";
	
	protected static final float ZOOM_INVALID = -1f;

	protected Easing mEasing = new Cubic();
	protected Matrix mBaseMatrix = new Matrix();
	protected Matrix mSuppMatrix = new Matrix();
	protected Matrix mNextMatrix;
	protected Handler mHandler = new Handler();
	protected Runnable mLayoutRunnable = null;
	protected float mMaxZoom = ZOOM_INVALID;
	protected float mMinZoom = ZOOM_INVALID;
	protected final Matrix mDisplayMatrix = new Matrix();
	protected final float[] mMatrixValues = new float[9];
	protected int mThisWidth = -1, mThisHeight = -1;
	
	protected ScaleType mScaleType = ScaleType.Normal;
	private boolean mScaleTypeChanged;
	private boolean mBitmapChanged;

	final protected int DEFAULT_ANIMATION_DURATION = 200;

	protected RectF mBitmapRect = new RectF();
	protected RectF mCenterRect = new RectF();
	protected RectF mScrollRect = new RectF();

	private OnBitmapChangedListener mListener;

	public ImageViewTouchBase( Context context ) {
		super( context );
		init();
	}

	public ImageViewTouchBase( Context context, AttributeSet attrs ) {
		super( context, attrs );
		init();
	}

	public void setOnBitmapChangedListener( OnBitmapChangedListener listener ) {
		mListener = listener;
	}

	protected void init() {
		setScaleType( ImageView.ScaleType.MATRIX );
	}

	public void clear() {
		setImageBitmap( null, true );
	}

	public void setScaleType( ScaleType type ) {
		if ( type != mScaleType ) {
			mScaleType = type;
			mScaleTypeChanged = true;
			requestLayout();
		}
	}

	public void setMinZoom( float value ) {
		Log.d( LOG_TAG, "setMinZoom: " + value );
		mMinZoom = value;
	}
	
	public void setMaxZoom( float value ) {
		Log.d( LOG_TAG, "setMaxZoom: " + value );
		mMaxZoom = value;
	}

	@Override
	protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
		
		Log.i( LOG_TAG, "onLayout: " + changed + ", bitmapChanged: " + mBitmapChanged + ", scaleChanged: " + mScaleTypeChanged );
		
		super.onLayout( changed, left, top, right, bottom );
		mThisWidth = right - left;
		mThisHeight = bottom - top;
		
		Runnable r = mLayoutRunnable;
		
		if ( r != null ) {
			mLayoutRunnable = null;
			r.run();
		}
		
		if( getDrawable() != null ) {
			
			if( changed || mScaleTypeChanged || mBitmapChanged ) {
				
				Log.d( LOG_TAG, "compute matrix..." );
				
				getProperBaseMatrix( getDrawable(), mBaseMatrix );
				
				float scale = 0;
				
				if( mNextMatrix != null ) {
					mSuppMatrix.set( mNextMatrix );
					mNextMatrix = null;
					scale = getScale( mSuppMatrix );
				}
				
				setImageMatrix( getImageViewMatrix() );
				
				
				if( mScaleType == ScaleType.FitToScreen ) {
					scale = 1f;
				} else if( mScaleType == ScaleType.Normal ) {
					
					if( scale == 0 ) {
						scale = 1f / getScale( mBaseMatrix );
					}
				}
				
				// ============================================
				// TODO: 
				//		need to find a nice way to restore the old
				// 	image position/scale if we just receive a change in
				//		the layout ( ie. screen orientation change )
				// ============================================
				
				zoomTo( scale );
				
				Log.d( LOG_TAG, "scale: " +  getScale( mSuppMatrix ) );
				Log.d( LOG_TAG, "minZoom: " +  getMinZoom() );
				Log.d( LOG_TAG, "maxZoom: " +  getMaxZoom() );
				
				if( mScaleTypeChanged ) mScaleTypeChanged = false;
				if( mBitmapChanged ) mBitmapChanged = false;
			}
		}
	}

	@Override
	public void setImageBitmap( Bitmap bm ) {
		setImageBitmap( bm, true );
	}
	
	
	@Override
	public void setImageResource( int resId ) {
		setImageDrawable( getContext().getResources().getDrawable( resId ) );
	}

	/**
	 * Set the new image to display and reset the internal matrix.
	 * 
	 * @param bitmap
	 *           - the {@link Bitmap} to display
	 * @param reset
	 *           - if true the image bounds will be recreated, otherwise the old {@link Matrix} is used to display the new bitmap
	 * @see #setImageBitmap(Bitmap)
	 */	
	public void setImageBitmap( final Bitmap bitmap, final boolean reset ) {
		setImageBitmap( bitmap, reset, null, ZOOM_INVALID, ZOOM_INVALID );
	}

	/**
	 * 
	 * @param bitmap
	 * @param reset
	 * @param matrix
	 * @param maxZoom
	 *           - maximum zoom allowd during zoom gestures
	 * 
	 * @see #setImageBitmap(Bitmap, boolean, Matrix)
	 */
	public void setImageBitmap( final Bitmap bitmap, final boolean reset, Matrix matrix, float min_zoom, float max_zoom ) {

		if ( bitmap != null )
			setImageDrawable( new FastBitmapDrawable( bitmap ), reset, matrix, min_zoom, max_zoom );
		else
			setImageDrawable( null, reset, matrix, min_zoom, max_zoom );
	}

	@Override
	public void setImageDrawable( Drawable drawable ) {
		setImageDrawable( drawable, true, null, ZOOM_INVALID, ZOOM_INVALID );
	}

	public void setImageDrawable( final Drawable drawable, final boolean reset, final Matrix initial_matrix, final float min_zoom, final float max_zoom ) {

		final int viewWidth = getWidth();

		if ( viewWidth <= 0 ) {
			mLayoutRunnable = new Runnable() {

				@Override
				public void run() {
					setImageDrawable( drawable, reset, initial_matrix, min_zoom, max_zoom );
				}
			};
			return;
		}

		_setImageDrawable( drawable, reset, initial_matrix, min_zoom, max_zoom );
	}

	protected void _setImageDrawable( final Drawable drawable, final boolean reset, final Matrix initial_matrix, float min_zoom, float max_zoom ) {
		
		Log.i( LOG_TAG, "_setImageDrawable" );
		
		if ( drawable != null ) {
			Log.d( LOG_TAG, "size: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight() );
			super.setImageDrawable( drawable );
		} else {
			mBaseMatrix.reset();
			super.setImageDrawable( null );
		}
		
		
		if( min_zoom != ZOOM_INVALID && max_zoom != ZOOM_INVALID ) {
			min_zoom = Math.min( min_zoom, max_zoom );
			max_zoom = Math.max( min_zoom, max_zoom );
			
			mMinZoom = min_zoom;
			mMaxZoom = max_zoom;
			
			if( mScaleType == ScaleType.FitToScreen ) {
				
				if( mMinZoom >= 1 ) {
					mMinZoom = ZOOM_INVALID;
				}
				
				if( mMaxZoom <= 1 ) {
					mMaxZoom = ZOOM_INVALID;
				}
			}
		} else {
			mMinZoom = ZOOM_INVALID;
			mMaxZoom = ZOOM_INVALID;
		}
		

		if ( reset ) {
			mSuppMatrix.reset();
			
			if ( initial_matrix != null && mScaleType == ScaleType.Normal ) {
				mNextMatrix = new Matrix( initial_matrix );
			}
		}

		onBitmapChanged( drawable );
		requestLayout();
	}

	protected void onBitmapChanged( final Drawable bitmap ) {
		Log.i( LOG_TAG, "onBitmapChanged" );
		
		mBitmapChanged = true;
		if ( mListener != null ) {
			mListener.onBitmapChanged( bitmap );
		}
	}

	protected float computeMaxZoom() {
		final Drawable drawable = getDrawable();

		if ( drawable == null ) {
			return 1F;
		}

		float fw = (float) drawable.getIntrinsicWidth() / (float) mThisWidth;
		float fh = (float) drawable.getIntrinsicHeight() / (float) mThisHeight;
		float scale = Math.max( fw, fh ) * 8;
		
		Log.i( LOG_TAG, "computeMaxZoom: " + scale );
		return scale;
	}
	
	protected float computeMinZoom() {
		final Drawable drawable = getDrawable();

		if ( drawable == null ) {
			return 1F;
		}
		
		float scale;
		
		if( mScaleType == ScaleType.Normal ) {
			scale = Math.min( 1f, 1f / getScale( mBaseMatrix ) );
		} else {
			scale = Math.min( 1f, 1f / getScale( mBaseMatrix ) );
			//scale = 1f;
		}

		Log.i( LOG_TAG, "computeMinZoom: " + scale );
		return scale;
	}

	public float getMaxZoom() {
		if ( mMaxZoom == ZOOM_INVALID ) {
			mMaxZoom = computeMaxZoom();
		}
		return mMaxZoom;
	}

	public float getMinZoom() {
		if( mMinZoom == ZOOM_INVALID ) {
			mMinZoom = computeMinZoom();
		}
		return mMinZoom;
	}

	public Matrix getImageViewMatrix() {
		return getImageViewMatrix( mSuppMatrix );
	}

	public Matrix getImageViewMatrix( Matrix supportMatrix ) {
		mDisplayMatrix.set( mBaseMatrix );
		mDisplayMatrix.postConcat( supportMatrix );
		return mDisplayMatrix;
	}
	
	@Override
	public void setImageMatrix( Matrix matrix ) {
		
		Matrix current = getImageMatrix();
		boolean needUpdate = false;
		if (matrix == null && !current.isIdentity() || matrix != null && !current.equals(matrix)) {
			needUpdate = true;
		}
		super.setImageMatrix( matrix );
		
		if( needUpdate )
			onConfigureBounds();
	}
	
	/**
	 * Called just after the {@link #setImageMatrix(Matrix)}
	 * has been invoked.
	 */
	protected void onConfigureBounds() {
		
	}

	/**
	 * Returns the current image display matrix. This matrix can be used in the next call to the
	 * {@link #setImageBitmap(Bitmap, boolean, Matrix)} to restore the same view state of the previous {@link Bitmap}
	 * 
	 * @return
	 */
	public Matrix getDisplayMatrix() {
		return new Matrix( mSuppMatrix );
	}

	/**
	 * Setup the base matrix so that the image is centered and scaled properly.
	 * 
	 * @param bitmap
	 * @param matrix
	 */
	protected void getProperBaseMatrix( Drawable drawable, Matrix matrix ) {
		float viewWidth = getWidth();
		float viewHeight = getHeight();
		float w = drawable.getIntrinsicWidth();
		float h = drawable.getIntrinsicHeight();
		float widthScale, heightScale;
		matrix.reset();

		if ( w > viewWidth || h > viewHeight ) {
			widthScale = Math.min( viewWidth / w, 2.0f );
			heightScale = Math.min( viewHeight / h, 2.0f );
			float scale = Math.min( widthScale, heightScale );
			matrix.postScale( scale, scale );
			
			Log.d( LOG_TAG, "scale: " + scale );
			
			float tw = ( viewWidth - w * scale ) / 2.0f;
			float th = ( viewHeight - h * scale ) / 2.0f;
			matrix.postTranslate( tw, th );
			
		} else {
			//float tw = ( viewWidth - w ) / 2.0f;
			//float th = ( viewHeight - h ) / 2.0f;
			//matrix.postTranslate( tw, th );
			
			widthScale = viewWidth / w;
			heightScale = viewHeight / h;
			float scale = Math.min( widthScale, heightScale );
			matrix.postScale( scale, scale );
			
			Log.d( LOG_TAG, "scale: " + scale + ", widthScale: " + widthScale + ", heightScale: " + heightScale );
			
			float tw = ( viewWidth - w * scale ) / 2.0f;
			float th = ( viewHeight - h * scale ) / 2.0f;
			matrix.postTranslate( tw, th );		
		}
	}

	/**
	 * Setup the base matrix so that the image is centered and scaled properly.
	 * 
	 * @param bitmap
	 * @param matrix
	 */
	protected void getProperBaseMatrix2( Drawable bitmap, Matrix matrix ) {
		
		float viewWidth = getWidth();
		float viewHeight = getHeight();
		
		float w = bitmap.getIntrinsicWidth();
		float h = bitmap.getIntrinsicHeight();
		
		matrix.reset();
		
		float widthScale = viewWidth / w;
		float heightScale = viewHeight / h;
		
		float scale = Math.min( widthScale, heightScale );
		
		matrix.postScale( scale, scale );
		matrix.postTranslate( ( viewWidth - w * scale ) / 2.0f, ( viewHeight - h * scale ) / 2.0f );
	}

	protected float getValue( Matrix matrix, int whichValue ) {
		matrix.getValues( mMatrixValues );
		return mMatrixValues[whichValue];
	}

	protected RectF getBitmapRect() {
		return getBitmapRect( mSuppMatrix );
	}

	protected RectF getBitmapRect( Matrix supportMatrix ) {
		final Drawable drawable = getDrawable();

		if ( drawable == null ) return null;
		Matrix m = getImageViewMatrix( supportMatrix );
		mBitmapRect.set( 0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight() );
		m.mapRect( mBitmapRect );
		return mBitmapRect;
	}

	protected float getScale( Matrix matrix ) {
		return getValue( matrix, Matrix.MSCALE_X );
	}

	@SuppressLint("Override")
	public float getRotation() {
		return 0;
	}

	public float getScale() {
		return getScale( mSuppMatrix );
	}

	protected void center( boolean horizontal, boolean vertical ) {
		final Drawable drawable = getDrawable();

		if ( drawable == null ) return;
		RectF rect = getCenter( mSuppMatrix, horizontal, vertical );
		if ( rect.left != 0 || rect.top != 0 ) {
			postTranslate( rect.left, rect.top );
		}
	}

	protected RectF getCenter( Matrix supportMatrix, boolean horizontal, boolean vertical ) {
		final Drawable drawable = getDrawable();

		if ( drawable == null ) return new RectF( 0, 0, 0, 0 );

		mCenterRect.set( 0, 0, 0, 0 );
		RectF rect = getBitmapRect( supportMatrix );
		float height = rect.height();
		float width = rect.width();
		float deltaX = 0, deltaY = 0;
		if ( vertical ) {
			int viewHeight = getHeight();
			if ( height < viewHeight ) {
				deltaY = ( viewHeight - height ) / 2 - rect.top;
			} else if ( rect.top > 0 ) {
				deltaY = -rect.top;
			} else if ( rect.bottom < viewHeight ) {
				deltaY = getHeight() - rect.bottom;
			}
		}
		if ( horizontal ) {
			int viewWidth = getWidth();
			if ( width < viewWidth ) {
				deltaX = ( viewWidth - width ) / 2 - rect.left;
			} else if ( rect.left > 0 ) {
				deltaX = -rect.left;
			} else if ( rect.right < viewWidth ) {
				deltaX = viewWidth - rect.right;
			}
		}
		mCenterRect.set( deltaX, deltaY, 0, 0 );
		return mCenterRect;
	}

	protected void postTranslate( float deltaX, float deltaY ) {
		mSuppMatrix.postTranslate( deltaX, deltaY );
		setImageMatrix( getImageViewMatrix() );
	}

	protected void postScale( float scale, float centerX, float centerY ) {
		mSuppMatrix.postScale( scale, scale, centerX, centerY );
		setImageMatrix( getImageViewMatrix() );
	}

	protected void zoomTo( float scale ) {
		float cx = getWidth() / 2F;
		float cy = getHeight() / 2F;
		
		if ( scale > getMaxZoom() ) scale = getMaxZoom();
		if ( scale < getMinZoom() ) scale = getMinZoom();
		
		zoomTo( scale, cx, cy );
	}

	public void zoomTo( float scale, float durationMs ) {
		float cx = getWidth() / 2F;
		float cy = getHeight() / 2F;
		zoomTo( scale, cx, cy, durationMs );
	}

	protected void zoomTo( float scale, float centerX, float centerY ) {
		if ( scale > mMaxZoom ) scale = mMaxZoom;
		
		float oldScale = getScale();
		float deltaScale = scale / oldScale;
		Log.d( LOG_TAG, "zoomTo: " + scale + ", center: " + centerX + "x" + centerY );
		postScale( deltaScale, centerX, centerY );
		onZoom( getScale() );
		center( true, true );
	}

	protected void onZoom( float scale ) {}

	protected void onZoomAnimationCompleted( float scale ) {}

	public void scrollBy( float x, float y ) {
		panBy( x, y );
	}

	protected void panBy( double dx, double dy ) {
		RectF rect = getBitmapRect();
		mScrollRect.set( (float) dx, (float) dy, 0, 0 );
		updateRect( rect, mScrollRect );
		postTranslate( mScrollRect.left, mScrollRect.top );
		center( true, true );
	}

	protected void updateRect( RectF bitmapRect, RectF scrollRect ) {
        if( bitmapRect==null )
            return;

		float width = getWidth();
		float height = getHeight();

		if ( bitmapRect.top >= 0 && bitmapRect.bottom <= height ) scrollRect.top = 0;
		if ( bitmapRect.left >= 0 && bitmapRect.right <= width ) scrollRect.left = 0;
		if ( bitmapRect.top + scrollRect.top >= 0 && bitmapRect.bottom > height ) scrollRect.top = (int) ( 0 - bitmapRect.top );
		if ( bitmapRect.bottom + scrollRect.top <= ( height - 0 ) && bitmapRect.top < 0 )
			scrollRect.top = (int) ( ( height - 0 ) - bitmapRect.bottom );
		if ( bitmapRect.left + scrollRect.left >= 0 ) scrollRect.left = (int) ( 0 - bitmapRect.left );
		if ( bitmapRect.right + scrollRect.left <= ( width - 0 ) ) scrollRect.left = (int) ( ( width - 0 ) - bitmapRect.right );
	}

	protected void scrollBy( float distanceX, float distanceY, final double durationMs ) {
		final double dx = distanceX;
		final double dy = distanceY;
		final long startTime = System.currentTimeMillis();
		mHandler.post( new Runnable() {

			double old_x = 0;
			double old_y = 0;

			@Override
			public void run() {
				long now = System.currentTimeMillis();
				double currentMs = Math.min( durationMs, now - startTime );
				double x = mEasing.easeOut( currentMs, 0, dx, durationMs );
				double y = mEasing.easeOut( currentMs, 0, dy, durationMs );
				panBy( ( x - old_x ), ( y - old_y ) );
				old_x = x;
				old_y = y;
				if ( currentMs < durationMs ) {
					mHandler.post( this );
				} else {
					RectF centerRect = getCenter( mSuppMatrix, true, true );
					if ( centerRect.left != 0 || centerRect.top != 0 ) scrollBy( centerRect.left, centerRect.top );
				}
			}
		} );
	}

	protected void zoomTo( float scale, float centerX, float centerY, final float durationMs ) {
		if ( scale > getMaxZoom() ) scale = getMaxZoom();
		
		final long startTime = System.currentTimeMillis();
		final float oldScale = getScale();

		final float deltaScale = scale - oldScale;

		Matrix m = new Matrix( mSuppMatrix );
		m.postScale( scale, scale, centerX, centerY );
		RectF rect = getCenter( m, true, true );

		final float destX = centerX + rect.left * scale;
		final float destY = centerY + rect.top * scale;

		mHandler.post( new Runnable() {

			@Override
			public void run() {
				long now = System.currentTimeMillis();
				float currentMs = Math.min( durationMs, now - startTime );
				float newScale = (float) mEasing.easeInOut( currentMs, 0, deltaScale, durationMs );
				zoomTo( oldScale + newScale, destX, destY );
				if ( currentMs < durationMs ) {
					mHandler.post( this );
				} else {
					onZoomAnimationCompleted( getScale() );
					center( true, true );
				}
			}
		} );
	}

	@Override
	public void dispose() {
		clear();
	}
}
