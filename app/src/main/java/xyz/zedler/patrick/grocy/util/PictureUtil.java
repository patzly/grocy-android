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
}
