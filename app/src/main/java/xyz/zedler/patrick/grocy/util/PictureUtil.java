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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class PictureUtil {

  public static void loadPicture(ImageView imageView, @Nullable CardView frame, String pictureUrl) {
    Glide.with(imageView.getContext())
        .load(new GlideUrl(
            pictureUrl,
            RequestHeaders.getGlideGrocyAuthHeaders(imageView.getContext())
        )).transition(DrawableTransitionOptions.withCrossFade())
        .listener(new RequestListener<>() {
          @Override
          public boolean onLoadFailed(@Nullable GlideException e, Object model,
              Target<Drawable> target, boolean isFirstResource) {
            if (frame != null) frame.setVisibility(View.GONE);
            return false;
          }
          @Override
          public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
              DataSource dataSource, boolean isFirstResource) {
            if (frame != null) frame.setVisibility(View.VISIBLE);
            return false;
          }
        }).into(imageView);
  }

  public static void loadPicture(
      ImageView picture,
      @Nullable CardView frame,
      @Nullable CardView placeHolder,
      String pictureUrl,
      Headers grocyAuthHeaders,
      boolean keepAspectRatio
  ) {
    RequestBuilder<Drawable> requestBuilder = Glide.with(picture.getContext())
        .load(new GlideUrl(pictureUrl, grocyAuthHeaders));
    requestBuilder = requestBuilder
        .transform(new CenterCrop())
        .transition(DrawableTransitionOptions.withCrossFade());
    if (keepAspectRatio) {
      requestBuilder = requestBuilder.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
    }
    requestBuilder.listener(new RequestListener<>() {
      @Override
      public boolean onLoadFailed(@Nullable GlideException e, Object model,
          Target<Drawable> target, boolean isFirstResource) {
        picture.setVisibility(View.GONE);
        if (frame != null) frame.setVisibility(View.GONE);
        if (placeHolder != null) {
          placeHolder.setVisibility(View.VISIBLE);
        }
        return false;
      }
      @Override
      public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
          DataSource dataSource, boolean isFirstResource) {
        picture.setVisibility(View.VISIBLE);
        if (frame != null) frame.setVisibility(View.VISIBLE);
        if (placeHolder != null) {
          placeHolder.setVisibility(View.GONE);
        }
        return false;
      }
    }).into(picture);
  }

  public static Bitmap scaleBitmap(String imagePath) {
    int maxWidth = 1280;
    int maxHeight = 800;
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(imagePath, options);
    int imageHeight = options.outHeight;
    int imageWidth = options.outWidth;
    int scaleFactor = Math.min(imageWidth / maxWidth, imageHeight / maxHeight);
    options.inJustDecodeBounds = false;
    options.inSampleSize = scaleFactor;
    return BitmapFactory.decodeFile(imagePath, options);
  }

  public static Bitmap scaleBitmap(Bitmap bitmap) {
    int maxWidth = 1280;
    int maxHeight = 800;
    float scale = Math.min(
        ((float)maxWidth / bitmap.getWidth()),
        ((float)maxHeight / bitmap.getHeight())
    );
    int newWidth = Math.round(bitmap.getWidth() * scale);
    int newHeight = Math.round(bitmap.getHeight() * scale);
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
  }

  public static byte[] convertBitmapToByteArray(Bitmap bitmap) {
    if (bitmap == null) return null;
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
    return stream.toByteArray();
  }

  public static File createImageFile(File storageDir) throws IOException {
    return File.createTempFile(
        String.valueOf(System.currentTimeMillis()),
        ".jpg",
        storageDir
    );
  }

  public static String createImageFilename() {
    return System.currentTimeMillis() + ".jpg";
  }
}
