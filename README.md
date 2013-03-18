ImageViewTouch Widget
----


ImageViewTouch is an android `ImageView` widget with zoom and pan capabilities.
This is an implementation of the ImageView widget used in the Gallery app of the Android opensource project.

Checkout the repository and run the **ImageViewTouchTest** project to see how it works.
Beside the superclass **setImageBitmap** method it offers the following methods:

* `setImageBitmap( final Bitmap bitmap, Matrix matrix );`
* `setImageBitmap( final Bitmap bitmap, Matrix matrix, float minZoom, float maxZoom );`


If you want to load a new Bitmap with a particular zoom/pan state (let's say the same from another ImageView ), you can call:

	Matrix matrix = mImageView1.getDisplayMatrix();
	mImageView2.setImageBitmap( bitmap, matrix );


## Tweaks

The initial display state can be set, using `public void setDisplayType( DisplayType type )`, as:

* `DisplayType.FIT_TO_SCREEN`: The image loaded will always fit the current view's bounds.
* `DisplayType.NONE`: The image will be presented with its current dimensions if smaller than the image bounds, otherwise it will be scaled to fit its contents inside the screen.

The default display state is `DisplayState.NONE'.


##Usage (Maven)
    <dependency>
        <groupId>it.sephiroth.android.library.imagezoom</groupId>
        <artifactId>image-view-zoom</artifactId>
        <version>1.0.0</version>
        <type>apklib</type>
    </dependency>


##LICENSE

This software is provided under the MIT license:<br />
http://opensource.org/licenses/mit-license.php


##Author

Alessandro Crugnola