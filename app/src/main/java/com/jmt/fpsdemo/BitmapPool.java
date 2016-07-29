package com.jmt.fpsdemo;

import java.util.ArrayList;
import android.graphics.Bitmap;
import android.util.Log;

public class BitmapPool {
   @SuppressWarnings("unused")
   private static final String TAG = "BitmapPool";

   private final ArrayList<Bitmap> mPool;
   private final int mPoolLimit;

   public BitmapPool (int poolLimit) {
      mPoolLimit = poolLimit;
      mPool = new ArrayList<Bitmap>(poolLimit);
   }

   // Get a last Bitmap from the pool.
   public synchronized Bitmap getlastBitmap() {
      int size = mPool.size();
      return size > 0 ? mPool.remove(size - 1) : null;
   }

   // Get a first Bitmap from the pool.
   public synchronized Bitmap getfirstBitmap() {
      int size = mPool.size();
      Log.i(TAG, "size = " + size);
      return size > 0 ? mPool.remove(0) : null;
   }

   // Put a Bitmap into the pool last pos
   public void putlastBitmap (Bitmap bitmap) {
      if (bitmap == null) return;

      synchronized (this) {
         if (mPool.size() >= mPoolLimit) mPool.remove(0);
         mPool.add (bitmap);
      }
   }

   // Put a Bitmap into the pool first pos
   public void putfirstBitmap (Bitmap bitmap) {
      if (bitmap == null) return;

      synchronized (this) {
         int size = mPool.size();
         if (size >= mPoolLimit) mPool.remove (size - 1);
         mPool.add (0, bitmap);
      }
   }
}
