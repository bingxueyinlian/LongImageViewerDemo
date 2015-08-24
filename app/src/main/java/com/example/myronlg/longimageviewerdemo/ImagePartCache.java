package com.example.myronlg.longimageviewerdemo;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Created by myron.lg on 2015/8/24.
 */
public class ImagePartCache extends LruCache<Integer, Bitmap> {
    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public ImagePartCache(int maxSize) {
        super(maxSize);
    }
}
