package com.example.myronlg.longimageviewerdemo;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.io.IOException;

/**
 * Created by myron.lg on 2015/8/20.
 */
public class LongImageView extends View {

    private int width;
    private int height;

    private Paint paint;
    private float downY;
    private int oldScrollY;
    private Bitmap[] bitmaps;
    private BitmapRegionDecoder bitmapRegionDecoder;
    private Matrix matrix;
    private int cutImgHeight;
    private int maxScrollY;
    private int minScrollY;
    private int longImgWidth;
    private int longImgHeight;

    private LruCache<Integer, Bitmap> cache;
    private Scroller scroller;
    private VelocityTracker tracker;

    public LongImageView(Context context) {
        super(context);
        init();
    }

    public LongImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LongImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                width = getWidth();
                height = getHeight();

                longImgWidth = bitmapRegionDecoder.getWidth();
                longImgHeight = bitmapRegionDecoder.getHeight();
                float scale = width * 1.0F / longImgWidth;
                matrix = new Matrix();
                matrix.postScale(scale, scale);
                cutImgHeight = (int) (height / scale);
                minScrollY = 0;
                maxScrollY = (int) (longImgHeight * scale - height);
                getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
        paint = new Paint();

        try {
            bitmapRegionDecoder = BitmapRegionDecoder.newInstance("/storage/emulated/0/long.jpg", true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        cache = new LruCache<Integer, Bitmap>(memoryClassBytes / 10);

        scroller = new Scroller(getContext(), new DecelerateInterpolator());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int screens = getScrollY() / height;

        Bitmap bitmap;
        for (int position = -1; position < 2; position++) {
            bitmap = getBitmapByScreen(screens + position);
            if (bitmap == null) {
                continue;
            }
            canvas.save();
            canvas.translate(0, (screens + position) * height - position);
            canvas.drawBitmap(bitmap, matrix, paint);
            canvas.restore();
        }
    }

    private int getAndRefreshBitmapAtPositionByScreen(int screens, int position) {
        int i = (screens + position) % 3 < 0 ? (screens + position) % 3 + 3 : (screens + position) % 3;
//        if (bitmaps[i] != null) {
//            bitmaps[i].recycle();
//        }
        bitmaps[i] = getBitmapByScreen(screens + position);
        return i;
    }

    private Bitmap getBitmapByScreen(int screens) {
        Bitmap bitmap = cache.get(screens);
        if (bitmap != null) {
            return bitmap;
        }
        if (screens >= 0 && bitmapRegionDecoder != null && screens * cutImgHeight < longImgHeight) {
            bitmap = bitmapRegionDecoder.decodeRegion(new Rect(0, Math.min(screens * cutImgHeight, longImgHeight), longImgWidth, Math.min((screens + 1) * cutImgHeight, longImgHeight)), null);
            cache.put(screens, bitmap);
            return bitmap;
        }
        return null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
//        if (!scroller.isFinished()){
//            return true;
//        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downY = event.getY();
                oldScrollY = getScrollY();
                scroller.abortAnimation();
                tracker.clear();
                tracker.addMovement(event);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (downY == -1) {
                    downY = event.getY();
                    oldScrollY = getScrollY();
                }
                float dy = event.getY() - downY;
                setScrollY(clampScrollY(oldScrollY + (int) -dy));
                tracker.addMovement(event);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                tracker.addMovement(event);
                tracker.computeCurrentVelocity(1000);
                scroller.fling(getScrollX(), getScrollY(), 0, (int) -tracker.getYVelocity(), getScrollX(), getScrollX(), minScrollY, maxScrollY);
                invalidate();
                downY = -1;
                break;
            }
        }
        return true;
//        return super.dispatchTouchEvent(event);
    }

    @Override
    public void computeScroll() {
//        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            setScrollY(scroller.getCurrY());
        }
    }

    private int clampScrollY(float scrollY) {
        return (int) Math.min(maxScrollY, Math.max(minScrollY, scrollY));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        tracker = VelocityTracker.obtain();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cache.evictAll();
        tracker.recycle();
    }
}
