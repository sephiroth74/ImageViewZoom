package it.sephiroth.android.library.imagezoom.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.io.InputStream;

/**
 * Fast bitmap drawable. Does not support states. it only
 * support alpha and colormatrix
 *
 * @author alessandro
 */
public class FastBitmapDrawable extends Drawable implements IBitmapDrawable {
    protected Bitmap mBitmap;
    protected Paint mPaint;
    protected int mIntrinsicWidth, mIntrinsicHeight;

    public FastBitmapDrawable(Bitmap b) {
        mBitmap = b;
        if (null != mBitmap) {
            mIntrinsicWidth = mBitmap.getWidth();
            mIntrinsicHeight = mBitmap.getHeight();
        } else {
            mIntrinsicWidth = 0;
            mIntrinsicHeight = 0;
        }
        mPaint = new Paint();
        mPaint.setDither(true);
        mPaint.setFilterBitmap(true);
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public FastBitmapDrawable(Resources res, InputStream is) {
        this(BitmapFactory.decodeStream(is));
    }

    @Override
    public void draw(Canvas canvas) {
        if (null != mBitmap && !mBitmap.isRecycled()) {
            final Rect bounds = getBounds();
            if (!bounds.isEmpty()) {
                canvas.drawBitmap(mBitmap, null, bounds, mPaint);
            } else {
                canvas.drawBitmap(mBitmap, 0f, 0f, mPaint);
            }
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    @Override
    public int getMinimumWidth() {
        return mIntrinsicWidth;
    }

    @Override
    public int getMinimumHeight() {
        return mIntrinsicHeight;
    }

    public void setAntiAlias(boolean value) {
        mPaint.setAntiAlias(value);
        invalidateSelf();
    }

    @Override
    public Bitmap getBitmap() {
        return mBitmap;
    }

    public Paint getPaint() {
        return mPaint;
    }
}