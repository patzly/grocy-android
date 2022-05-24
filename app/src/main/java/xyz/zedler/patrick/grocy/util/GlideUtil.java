/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import org.wordpress.aztec.Html;

public class GlideUtil {
  public static class GlideImageLoader implements Html.ImageGetter {

    private final Context context;

    public GlideImageLoader(Context context) {
      this.context = context;
    }

    @Override
    public void loadImage(String source, Callbacks callbacks, int maxWidth) {
      loadImage(source, callbacks, maxWidth, 0);
    }

    @Override
    public void loadImage(String source, Callbacks callbacks, int maxWidth, int minWidth) {
      Glide.with(context).asBitmap().load(source).into(new Target<Bitmap>() {
        public void onLoadStarted(Drawable placeholder) {
          callbacks.onImageLoading(placeholder);
        }

        public void onLoadFailed(Drawable errorDrawable) {
          callbacks.onImageFailed();
        }

        private Bitmap upscaleTo(Bitmap bitmap, int desiredWidth) {
          float ratio = (float) bitmap.getHeight() / (float) bitmap.getWidth();
          float proportionateHeight = ratio * desiredWidth;
          int finalHeight = (int) Math.rint(proportionateHeight);
          return Bitmap.createScaledBitmap(bitmap, desiredWidth, finalHeight, true);
        }

        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
          //Upscaling bitmap only for demonstration purposes.
          //This should probably be done somewhere more appropriate for Glide (?).
          if (resource.getWidth() < minWidth) {
            callbacks.onImageLoaded(new BitmapDrawable(context.getResources(), upscaleTo(resource, minWidth)));
            return;
          }

          // By default, BitmapFactory.decodeFile sets the bitmap's density to the device default so, we need
          // to correctly set the input density to 160 ourselves.
          resource.setDensity(DisplayMetrics.DENSITY_DEFAULT);
          callbacks.onImageLoaded(new BitmapDrawable(context.getResources(), resource));
        }

        public void onLoadCleared(Drawable placeholder) {}

        public void getSize(@NonNull SizeReadyCallback cb) {
          cb.onSizeReady(maxWidth, Target.SIZE_ORIGINAL);
        }

        public void removeCallback(@NonNull SizeReadyCallback cb) {
        }

        public void setRequest(Request request) {
        }

        public Request getRequest() {
          return null;
        }

        public void onStart() {
        }

        public void onStop() {
        }

        public void onDestroy() {
        }
      });
    }
  }
}
