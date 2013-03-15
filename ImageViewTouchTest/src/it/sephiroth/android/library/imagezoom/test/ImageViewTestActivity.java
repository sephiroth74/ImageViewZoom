package it.sephiroth.android.library.imagezoom.test;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchDoubleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchSingleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnBitmapChangedListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.ScaleType;
import it.sephiroth.android.library.imagezoom.test.utils.DecodeUtils;
import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class ImageViewTestActivity extends Activity {
	
	private static final String LOG_TAG = "image-test";

	ImageViewTouch mImage;
	Button mButton;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		setContentView( R.layout.main );
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mImage = (ImageViewTouch) findViewById( R.id.image );
		mButton = (Button) findViewById( R.id.button );
		
		mButton.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				selectRandomImage();
			}
		} );
		
		mImage.setSingleTapListener( new OnImageViewTouchSingleTapListener() {
			
			@Override
			public void onSingleTapConfirmed() {
				Log.d( LOG_TAG, "onSingleTapConfirmed" );
			}
		} );
		
		mImage.setDoubleTapListener( new OnImageViewTouchDoubleTapListener() {
			
			@Override
			public void onDoubleTap() {
				Log.d( LOG_TAG, "onDoubleTap" );
			}
		} );
		
		mImage.setOnBitmapChangedListener( new OnBitmapChangedListener() {
			
			@Override
			public void onBitmapChanged( Drawable drawable ) {
				Log.i( LOG_TAG, "onBitmapChanged: " + drawable );
			}
		} );
	}
	
	@Override
	public void onConfigurationChanged( Configuration newConfig ) {
		super.onConfigurationChanged( newConfig );
	}
	
	Matrix imageMatrix;

	public void selectRandomImage() {
		Cursor c = getContentResolver().query( Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null );
		if ( c != null ) {
			int count = c.getCount();
			int position = (int) ( Math.random() * count );
			if ( c.moveToPosition( position ) ) {
				long id = c.getLong( c.getColumnIndex( Images.Media._ID ) );

				Uri imageUri = Uri.parse( Images.Media.EXTERNAL_CONTENT_URI + "/" + id );
				
				// imageUri = Uri.parse( "content://media/external/images/media/14138" );

				
				Log.d( "image", imageUri.toString() );
				
				final int size = 400;
				Bitmap bitmap = DecodeUtils.decode( this, imageUri, size, size );
				if( null != bitmap )
				{
					// you can set the minimum zoom of the image ( must be called before anything else )
					// mImage.setMinZoom( 1.9f );
					
					// calling this will force the image to fit the ImageView container width/height
					mImage.setScaleType( ScaleType.FitToScreen );
					
					if( null == imageMatrix ) {
						imageMatrix = new Matrix();
					} else {
						// imageMatrix = new Matrix( mImage.getDisplayMatrix() );
					}
					
					// if the scaleType is set to ScaleType.FitToScreen, then the 
					// initial matrix is ignored
					mImage.setImageBitmap( bitmap, true, imageMatrix.isIdentity() ? null : imageMatrix, -1, -1 );
					
					
				} else {
					Toast.makeText( this, "Failed to load the image", Toast.LENGTH_LONG ).show();
				}
			}
			c.close();
			c = null;
			return;
		}
	}
}
