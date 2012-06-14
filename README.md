ImageViewZoom
=============

Android ImageView widget with zoom and pan capabilities.
This is an implementation of the ImageView widget used in the Gallery app of the Android opensource project.

Checkout the repository and run the **ImageViewTouchTest** project to see how it works.
Beside the superclass **setImageBitmap** method it offers the following methods:

* setImageDrawable( Drawable drawable )
* setImageBitmap( final Bitmap bitmap, final boolean reset )
* setImageBitmap( final Bitmap bitmap, final boolean reset, Matrix matrix )
* setImageBitmap( final Bitmap bitmap, final boolean reset, Matrix matrix, float maxZoom )


If you want to load a new Bitmap with a particular zoom/pan state (let's say the same from another imageview ), you can call:

	Matrix matrix = mImageView1.getDisplayMatrix();
	mImageView2.setImageBitmap( bitmap, true, matrix );
