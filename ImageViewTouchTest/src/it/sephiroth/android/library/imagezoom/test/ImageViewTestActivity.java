package it.sephiroth.android.library.imagezoom.test;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchDoubleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchSingleTapListener;
import it.sephiroth.android.library.imagezoom.test.utils.DecodeUtils;
import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
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

	ImageViewTouch mImage;
	Button mButton;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		Log.i( "image", "onCreate" );
		
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
				Log.d( "image", "onSingleTapConfirmed" );
			}
		} );
		
		mImage.setDoubleTapListener( new OnImageViewTouchDoubleTapListener() {
			
			@Override
			public void onDoubleTap() {
				Log.d( "image", "onDoubleTap" );
			}
		} );
	}
	
	@Override
	public void onConfigurationChanged( Configuration newConfig ) {
		super.onConfigurationChanged( newConfig );
	}

	public void selectRandomImage() {
		Cursor c = getContentResolver().query( Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null );
		if ( c != null ) {
			int count = c.getCount();
			int position = (int) ( Math.random() * count );
			if ( c.moveToPosition( position ) ) {
				long id = c.getLong( c.getColumnIndex( Images.Media._ID ) );

				Uri imageUri = Uri.parse( Images.Media.EXTERNAL_CONTENT_URI + "/" + id );
				Log.d( "image", imageUri.toString() );
				
				Bitmap bitmap = DecodeUtils.decode( this, imageUri, 800, 600 );
				if( null != bitmap )
				{
					// you can set the minimum zoom of the image ( must be called before anything else )
					// mImage.setMinZoom( 1.9f );
					
					// calling this will force the image to fit the ImageView container width/height
					mImage.setFitToScreen( true );
					
					mImage.setImageBitmap( bitmap, true, null, 5.0f );
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
